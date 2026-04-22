package com.cloud.gfs.service;

import com.cloud.gfs.model.ChunkRecord;
import com.cloud.gfs.model.FileMetadata;
import com.cloud.gfs.repository.FileRepository;
import com.gfs.grpc.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@GrpcService
public class MasterService extends MasterServiceGrpc.MasterServiceImplBase {
    private static final int REPLICATION_FACTOR = 3;
    private static final long LEASE_DURATION_MILLIS = 60_000;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private ServerHealthMonitor healthMonitor;

    @Override
    public void sendHeartbeat(HeartbeatRequest request, StreamObserver<HeartbeatResponse> responseObserver) {
        String address = request.getServerAddress();
        healthMonitor.recordHeartbeat(address);
        responseObserver.onNext(HeartbeatResponse.newBuilder().setSuccess(true).build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void getUploadLocation(FileRequest request, StreamObserver<ChunkLocationResponse> responseObserver) {
        String fileName = request.getFileName();

        FileMetadata file = fileRepository.findByFileName(fileName)
                .orElseGet(() -> {
                    FileMetadata newFile = new FileMetadata();
                    newFile.setFileName(fileName);
                    return fileRepository.save(newFile);
                });

        List<String> healthyServers = healthMonitor.getAllServers().keySet().stream()
                .filter(healthMonitor::isServerHealthy)
                .collect(Collectors.toList());

        if (healthyServers.isEmpty()) {
            responseObserver.onError(Status.UNAVAILABLE
                    .withDescription("No active chunk servers available")
                    .asRuntimeException());
            return;
        }

        List<String> assignedReplicas = chooseReplicas(healthyServers);
        String chunkId = UUID.randomUUID().toString();
        String primaryServer = assignedReplicas.getFirst();
        long leaseExpiration = System.currentTimeMillis() + LEASE_DURATION_MILLIS;

        ChunkRecord chunk = new ChunkRecord();
        chunk.setChunkId(chunkId);
        chunk.setSequenceNumber(file.getChunks().size());
        chunk.setPrimaryServerAddress(primaryServer);
        chunk.setLeaseExpirationEpochMillis(leaseExpiration);
        chunk.setReplicaServerAddresses(new ArrayList<>(assignedReplicas));

        file.getChunks().add(chunk);
        fileRepository.save(file);

        responseObserver.onNext(ChunkLocationResponse.newBuilder()
                .setChunkId(chunkId)
                .setPrimaryServerAddress(primaryServer)
                .addAllReplicaServerAddresses(assignedReplicas)
                .setLeaseExpirationEpochMillis(leaseExpiration)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void getFileChunks(FileRequest request, StreamObserver<ChunkLocationList> responseObserver) {
        String fileName = request.getFileName();
        FileMetadata file = fileRepository.findByFileName(fileName)
                .orElse(null);

        if (file == null) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("File not found: " + fileName)
                    .asRuntimeException());
            return;
        }

        ChunkLocationList.Builder listBuilder = ChunkLocationList.newBuilder();

        for (ChunkRecord record : file.getChunks()) {
            refreshPrimaryLease(record);
            ChunkLocationResponse location = ChunkLocationResponse.newBuilder()
                    .setChunkId(record.getChunkId())
                    .setPrimaryServerAddress(record.getPrimaryServerAddress())
                    .addAllReplicaServerAddresses(record.getReplicaServerAddresses())
                    .setLeaseExpirationEpochMillis(record.getLeaseExpirationEpochMillis())
                    .build();
            listBuilder.addChunks(location);
        }

        fileRepository.save(file);
        responseObserver.onNext(listBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional(readOnly = true)
    public void listFiles(Empty request, StreamObserver<FileList> responseObserver) {
        List<FileMetadata> files = fileRepository.findAll();
        FileList.Builder listBuilder = FileList.newBuilder();

        for (FileMetadata file : files) {
            listBuilder.addFileNames(file.getFileName());
        }

        responseObserver.onNext(listBuilder.build());
        responseObserver.onCompleted();
    }

    private List<String> chooseReplicas(List<String> healthyServers) {
        List<String> shuffledServers = new ArrayList<>(healthyServers);
        Collections.shuffle(shuffledServers, new Random());
        return shuffledServers.stream()
                .limit(Math.min(REPLICATION_FACTOR, shuffledServers.size()))
                .collect(Collectors.toList());
    }

    private void refreshPrimaryLease(ChunkRecord record) {
        List<String> healthyReplicas = record.getReplicaServerAddresses().stream()
                .filter(healthMonitor::isServerHealthy)
                .collect(Collectors.toList());

        if (healthyReplicas.isEmpty()) {
            throw Status.UNAVAILABLE
                    .withDescription("No healthy replicas available for chunk " + record.getChunkId())
                    .asRuntimeException();
        }

        record.setReplicaServerAddresses(new ArrayList<>(healthyReplicas));

        boolean leaseExpired = record.getLeaseExpirationEpochMillis() < System.currentTimeMillis();
        boolean primaryUnavailable = !healthyReplicas.contains(record.getPrimaryServerAddress());

        if (leaseExpired || primaryUnavailable) {
            record.setPrimaryServerAddress(healthyReplicas.getFirst());
            record.setLeaseExpirationEpochMillis(System.currentTimeMillis() + LEASE_DURATION_MILLIS);
        }
    }
}
