package com.cloud.gfs.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.gfs.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;

@Service
public class FileService {
    private final MasterServiceGrpc.MasterServiceBlockingStub masterStub;
    private final ConcurrentHashMap<String, ManagedChannel> channelCache = new ConcurrentHashMap<>();
    private final ExecutorService uploadExecutor = Executors.newFixedThreadPool(8);

    @Autowired
    public FileService(MasterServiceGrpc.MasterServiceBlockingStub masterStub) {
        this.masterStub = masterStub;
    }

    public void uploadFile(File file) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        try (InputStream is = new FileInputStream(file)) {
            byte[] buffer = new byte[1024 * 1024]; // 1MB chunk size
            int bytesRead;
            int chunkIndex = 0;

            while ((bytesRead = is.read(buffer)) != -1) {

                final byte[] chunkData = Arrays.copyOf(buffer, bytesRead);
                final int index = chunkIndex++;
                ChunkLocationResponse loc = masterStub.getUploadLocation(FileRequest.newBuilder()
                        .setFileName(file.getName())
                        .build());

                String primary = loc.getPrimaryServerAddress();
                String chunkId = loc.getChunkId();
                List<String> secondaries = loc.getReplicaServerAddressesList().stream()
                        .filter(server -> !server.equals(primary))
                        .collect(Collectors.toList());

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    uploadChunk(primary, chunkId, secondaries, chunkData, index);
                }, uploadExecutor);

                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            System.out.println("All " + chunkIndex + " chunks uploaded successfully.");

        } catch (Exception e) {
            futures.forEach(f -> f.cancel(true));
            throw new RuntimeException(("file upload failed"), e);
        }
    }

    private void uploadChunk(String primary, String chunkId, List<String> secondaries, byte[] data, int index) {
        ManagedChannel channel = channelCache.computeIfAbsent(primary,
                key -> ManagedChannelBuilder.forTarget(key).usePlaintext().build());

        WriteChunkResponse response = ChunkServiceGrpc.newBlockingStub(channel)
                .writeChunk(WriteChunkRequest.newBuilder()
                        .setChunkId(chunkId)
                        .setData(com.google.protobuf.ByteString.copyFrom(data))
                        .addAllSecondaryReplicas(secondaries)
                        .setReplicatedWrite(false)
                        .build());

        if (!response.getSuccess()) {
            throw new RuntimeException("Primary write failed for chunk " + chunkId + " at index " + index);
        }

        System.out.println("Uploaded chunk " + chunkId + " via primary " + primary);
    }


    @PreDestroy
    private void shutdown() {
        uploadExecutor.shutdown();
        channelCache.values().forEach(ManagedChannel::shutdown);
    }

    public File downloadFile(String fileName) {
        try {
            ChunkLocationList locations = masterStub.getFileChunks(FileRequest.newBuilder()
                    .setFileName(fileName)
                    .build());

            File downloadedFile = new File(System.getProperty("java.io.tmpdir") + "/downloaded_" + fileName);
            try (FileOutputStream fos = new FileOutputStream(downloadedFile)) {
                for (ChunkLocationResponse loc : locations.getChunksList()) {
                    byte[] chunkBytes = downloadFromReplicas(loc);
                    fos.write(chunkBytes);
                    System.out.println("Downloaded chunk " + loc.getChunkId());
                }
            }
            return downloadedFile;
        } catch (Exception e) {
            throw new RuntimeException("File download failed", e);
        }
    }

    private byte[] downloadFromReplicas(ChunkLocationResponse location) throws ExecutionException, InterruptedException {
        List<String> candidates = new ArrayList<>();
        candidates.add(location.getPrimaryServerAddress());
        for (String replica : location.getReplicaServerAddressesList()) {
            if (!candidates.contains(replica)) {
                candidates.add(replica);
            }
        }

        Exception lastFailure = null;
        for (String candidate : candidates) {
            try {
                return downloadChunk(candidate, location.getChunkId());
            } catch (Exception e) {
                lastFailure = e;
            }
        }

        throw new RuntimeException("No readable replicas for chunk " + location.getChunkId(), lastFailure);
    }

    private byte[] downloadChunk(String address, String chunkId) throws ExecutionException, InterruptedException {
        ManagedChannel channel = channelCache.computeIfAbsent(address,
                key -> ManagedChannelBuilder.forTarget(key).usePlaintext().build());

        ChunkServiceGrpc.ChunkServiceStub chunkStub = ChunkServiceGrpc.newStub(channel);
        CompletableFuture<byte[]> chunkDataFuture = new CompletableFuture<>();
        ByteArrayOutputStream chunkBuffer = new ByteArrayOutputStream();

        chunkStub.downloadChunk(ChunkRequest.newBuilder().setChunkId(chunkId).build(),
                new StreamObserver<>() {
                    @Override
                    public void onNext(ChunkData value) {
                        try {
                            chunkBuffer.write(value.getData().toByteArray());
                        } catch (IOException e) {
                            chunkDataFuture.completeExceptionally(e);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        chunkDataFuture.completeExceptionally(t);
                    }

                    @Override
                    public void onCompleted() {
                        chunkDataFuture.complete(chunkBuffer.toByteArray());
                    }
                });

        return chunkDataFuture.get();
    }

    public List<String> listFiles() {
        FileList response = masterStub.listFiles(Empty.newBuilder().build());
        return response.getFileNamesList();
    }
}
