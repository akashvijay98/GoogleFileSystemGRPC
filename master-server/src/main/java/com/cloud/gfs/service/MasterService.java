package com.cloud.gfs.service;

import com.cloud.gfs.model.ChunkRecord;
import com.cloud.gfs.model.ChunkServerNode;
import com.cloud.gfs.model.FileMetadata;
import com.cloud.gfs.repository.FileRepository;
import com.gfs.grpc.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@GrpcService
public class MasterService extends MasterServiceGrpc.MasterServiceImplBase {

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

        String assignedServer = healthyServers.get(new Random().nextInt(healthyServers.size()));
        String chunkId = UUID.randomUUID().toString();

        ChunkRecord chunk = new ChunkRecord();
        chunk.setChunkId(chunkId);
        chunk.setServerAddress(assignedServer);
        chunk.setSequenceNumber(file.getChunks().size());

        file.getChunks().add(chunk);
        fileRepository.save(file);

        responseObserver.onNext(ChunkLocationResponse.newBuilder()
                .setChunkId(chunkId)
                .setServerAddress(assignedServer)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @Transactional(readOnly = true)
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
            ChunkLocationResponse location = ChunkLocationResponse.newBuilder()
                    .setChunkId(record.getChunkId())
                    .setServerAddress(record.getServerAddress())
                    .build();
            listBuilder.addChunks(location);
        }

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
}
