package com.cloud.gfs.controller;

import java.io.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import  com.gfs.grpc.*;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class FileService {
    private final MasterServiceGrpc.MasterServiceBlockingStub masterStub;
    // Cache channels so we don't recreate them for every chunk
    private final Map<String, ManagedChannel> channelCache = new ConcurrentHashMap<>();

    public FileService(MasterServiceGrpc.MasterServiceBlockingStub masterStub) {
        this.masterStub = masterStub;
    }

    public void uploadFile(File file) {
        byte[] buffer = new byte[1024 * 1024];

        try (InputStream is = new FileInputStream(file)) {
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {

                ChunkLocationResponse loc = masterStub.getUploadLocation(FileRequest.newBuilder()
                        .setFileName(file.getName())
                        .build());

                String addr = loc.getServerAddress();
                String cid = loc.getChunkId();

                // Reuse or create channel
                ManagedChannel channel = channelCache.computeIfAbsent(addr, k ->
                        ManagedChannelBuilder.forTarget(k).usePlaintext().build());

                ChunkServiceGrpc.ChunkServiceStub chunkStub = ChunkServiceGrpc.newStub(channel);
                CompletableFuture<Boolean> future = new CompletableFuture<>();

                StreamObserver<ChunkData> requestObserver = chunkStub.uploadChunk(new StreamObserver<>() {
                    private UploadStatus status;
                    @Override public void onNext(UploadStatus v) { this.status = v; }
                    @Override public void onError(Throwable t) { future.completeExceptionally(t); }
                    @Override public void onCompleted() {
                        if (status != null && status.getSuccess()) future.complete(true);
                        else future.completeExceptionally(new RuntimeException("Fail"));
                    }
                });

                // Send and Close stream
                requestObserver.onNext(ChunkData.newBuilder()
                        .setChunkId(cid)
                        .setData(ByteString.copyFrom(buffer, 0, bytesRead))
                        .build());
                requestObserver.onCompleted();

                future.get(); // Wait for server acknowledgment
                System.out.println("Uploaded: " + cid);
            }
        } catch (Exception e) {
            throw new RuntimeException("Upload interrupted", e);
        }
    }

    // Call this when the app closes
    public void shutdown() {
        channelCache.values().forEach(ManagedChannel::shutdown);
    }
}