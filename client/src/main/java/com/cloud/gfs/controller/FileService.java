package com.cloud.gfs.controller;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.gfs.grpc.*;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PreDestroy;

@Service
public class FileService {
    private final MasterServiceGrpc.MasterServiceBlockingStub masterStub;
    // Cache channels so we don't recreate them for every chunk
    private final Map<String, ManagedChannel> channelCache = new ConcurrentHashMap<>();
    private ExecutorService uploadExecutor = Executors.newFixedThreadPool(8);
    @Autowired
    public FileService(MasterServiceGrpc.MasterServiceBlockingStub masterStub) {
        this.masterStub = masterStub;
    }

    public void uploadFile(File file) {
        List<CompletableFuture> futures = new ArrayList<>();
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

                String addr = loc.getServerAddress();
                String cid = loc.getChunkId();

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    uploadChunk(addr, cid, chunkData, index);
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

    private void uploadChunk(String addr, String cid, byte[] data, int index){

            ManagedChannel channel = channelCache.computeIfAbsent(addr,
                    k -> ManagedChannelBuilder.forTarget(k).usePlaintext().build());

            ChunkServiceGrpc.ChunkServiceStub chunkStub = ChunkServiceGrpc.newStub(channel);
            CompletableFuture<Boolean> ackFuture = new CompletableFuture<>();

            StreamObserver<ChunkData> requestObserver = chunkStub.uploadChunk(new StreamObserver<>() {
                private UploadStatus status;

                @Override
                public void onNext(UploadStatus v) {
                    this.status = v;
                }

                @Override
                public void onError(Throwable t) {
                    ackFuture.completeExceptionally(t);
                }

                @Override
                public void onCompleted() {
                    if (status != null && status.getSuccess()) {
                        ackFuture.complete(true);
                    } else {
                        ackFuture.completeExceptionally(new RuntimeException("Upload failed for chunk " + cid));
                    }
                }
            });

            requestObserver.onNext(ChunkData.newBuilder()
                    .setChunkId(cid)
                    .setData(ByteString.copyFrom(data))
                    .build());
            requestObserver.onCompleted();

            try {

                ackFuture.get(); // Wait for this chunk to be acknowledged
                System.out.println("Uploaded chunk " + cid);
            }
            catch(Exception e) {
                throw new RuntimeException("Upload chunk failed for chunk" + cid, e);
            }
    }


    // Call this when the app closes
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
                    String addr = loc.getServerAddress();
                    String cid = loc.getChunkId();

                    ManagedChannel channel = channelCache.computeIfAbsent(addr,
                            k -> ManagedChannelBuilder.forTarget(k).usePlaintext().build());

                    ChunkServiceGrpc.ChunkServiceStub chunkStub = ChunkServiceGrpc.newStub(channel);
                    CompletableFuture<byte[]> chunkDataFuture = new CompletableFuture<>();
                    ByteArrayOutputStream chunkBuffer = new ByteArrayOutputStream();

                    chunkStub.downloadChunk(ChunkRequest.newBuilder().setChunkId(cid).build(),
                            new StreamObserver<ChunkData>() {
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

                    byte[] chunkBytes = chunkDataFuture.get();
                    fos.write(chunkBytes);
                    System.out.println("Downloaded chunk " + cid);
                }
            }
            return downloadedFile;
        } catch (Exception e) {
            throw new RuntimeException("File download failed", e);
        }
    }

    public List<String> listFiles() {
        FileList response = masterStub.listFiles(Empty.newBuilder().build());
        return response.getFileNamesList();
    }
}
