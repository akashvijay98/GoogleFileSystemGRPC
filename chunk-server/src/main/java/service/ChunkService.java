package service;

import com.gfs.grpc.ChunkData;
import com.gfs.grpc.ChunkRequest;
import com.gfs.grpc.ChunkServiceGrpc;
import com.gfs.grpc.WriteChunkRequest;
import com.gfs.grpc.WriteChunkResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@GrpcService
public class ChunkService extends ChunkServiceGrpc.ChunkServiceImplBase {

    private final Path storageDir = Paths.get("data/chunks");
    private final Map<String, ManagedChannel> channelCache = new ConcurrentHashMap<>();

    @Override
    public void writeChunk(WriteChunkRequest request, StreamObserver<WriteChunkResponse> responseObserver) {
        try {
            long newVersion = persistChunk(request.getChunkId(), request.getData().toByteArray(), request.getChecksum(), request.getVersionNumber());

            if (!request.getReplicatedWrite()) {
                replicateToSecondaries(request, newVersion);
            }

            responseObserver.onNext(WriteChunkResponse.newBuilder()
                    .setSuccess(true)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to write chunk " + request.getChunkId() + ": " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void downloadChunk(ChunkRequest request, StreamObserver<ChunkData> responseObserver) {
        String chunkId = request.getChunkId();
        Path chunkPath = storageDir.resolve(chunkId);

        if (!Files.exists(chunkPath)) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Chunk " + chunkId + " not found")
                    .asRuntimeException());
            return;
        }

        try (InputStream inputStream = Files.newInputStream(chunkPath)) {
            byte[] buffer = new byte[64 * 1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                responseObserver.onNext(ChunkData.newBuilder()
                        .setChunkId(chunkId)
                        .setData(com.google.protobuf.ByteString.copyFrom(buffer, 0, bytesRead))
                        .build());
            }
            responseObserver.onCompleted();
        } catch (IOException e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error reading chunk from disk")
                    .asRuntimeException());
        }
    }

    @PreDestroy
    void shutdown() {
        channelCache.values().forEach(ManagedChannel::shutdown);
    }

    private long persistChunk(String chunkId, byte[] data, String checksum, long incomingVersion) throws IOException {
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }

        Path chunkPath = storageDir.resolve(chunkId);
        Files.write(chunkPath, data);

        // write metadata alongside chunk
        Path metaPath = storageDir.resolve(chunkId + ".meta");
        long version = Math.max(1L, incomingVersion);
        if (Files.exists(metaPath)) {
            try {
                List<String> lines = Files.readAllLines(metaPath);
                if (!lines.isEmpty()) {
                    try {
                        long existing = Long.parseLong(lines.get(0));
                        version = Math.max(existing + 1, version);
                    } catch (NumberFormatException ignored) {}
                }
            } catch (IOException ignored) {}
        }

        String metaContent = version + "\n" + checksum + "\n" + System.currentTimeMillis();
        Files.writeString(metaPath, metaContent);
        return version;
    }

    private void replicateToSecondaries(WriteChunkRequest request, long versionNumber) {
        for (String secondary : request.getSecondaryReplicasList()) {
            ManagedChannel channel = channelCache.computeIfAbsent(secondary,
                    key -> ManagedChannelBuilder.forTarget(key).usePlaintext().build());

            WriteChunkResponse response = ChunkServiceGrpc.newBlockingStub(channel)
                    .writeChunk(WriteChunkRequest.newBuilder()
                            .setChunkId(request.getChunkId())
                            .setData(request.getData())
                            .setReplicatedWrite(true)
                            .setVersionNumber(versionNumber)
                            .setSerialNumber(request.getSerialNumber())
                            .setChecksum(request.getChecksum())
                            .build());

            if (!response.getSuccess()) {
                throw new IllegalStateException("Replica write rejected by " + secondary);
            }
        }
    }
}
