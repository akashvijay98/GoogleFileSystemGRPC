package service;

import com.gfs.grpc.ChunkData;
import com.gfs.grpc.ChunkRequest;
import com.gfs.grpc.ChunkServiceGrpc;
import com.gfs.grpc.PushChunkDataRequest;
import com.gfs.grpc.PushChunkDataResponse;
import com.gfs.grpc.WriteChunkRequest;
import com.gfs.grpc.WriteChunkFromDataIdRequest;
import com.gfs.grpc.WriteChunkResponse;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@GrpcService
public class ChunkService extends ChunkServiceGrpc.ChunkServiceImplBase {

    private final Path storageDir = Paths.get("data/chunks");
    private final Path pushDir = Paths.get("data/push");
    private final Map<String, ManagedChannel> channelCache = new ConcurrentHashMap<>();
    private final Map<String, PushedData> pushedData = new ConcurrentHashMap<>();
    private static final long DEFAULT_PUSH_TTL_MILLIS = TimeUnit.MINUTES.toMillis(2);
    private static final int MAX_PUSHED_ENTRIES = 2048;
    private static final long MAX_PUSHED_BYTES = 256L * 1024 * 1024; // 256MiB

    private record PushedData(Path path, long sizeBytes, long expiresAtMillis) {}

    @Override
    public void writeChunk(WriteChunkRequest request, StreamObserver<WriteChunkResponse> responseObserver) {
        try {
            enforceWriteFencing(request.getChunkId(), request.getSerialNumber());
            long newVersion = persistChunkStreaming(request.getChunkId(), request.getData(), request.getChecksum(), request.getVersionNumber());

            if (!request.getReplicatedWrite()) {
                replicateToSecondaries(request, newVersion);
            }

            responseObserver.onNext(WriteChunkResponse.newBuilder()
                    .setSuccess(true)
                    .build());
            responseObserver.onCompleted();
        } catch (IllegalStateException e) {
            responseObserver.onNext(WriteChunkResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorMessage(e.getMessage())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to write chunk " + request.getChunkId() + ": " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void pushChunkData(PushChunkDataRequest request, StreamObserver<PushChunkDataResponse> responseObserver) {
        String dataId = request.getDataId();
        if (dataId == null || dataId.isBlank()) {
            responseObserver.onNext(PushChunkDataResponse.newBuilder().setSuccess(false).setErrorMessage("dataId is required").build());
            responseObserver.onCompleted();
            return;
        }
        if (pushedData.size() >= MAX_PUSHED_ENTRIES) {
            responseObserver.onNext(PushChunkDataResponse.newBuilder().setSuccess(false).setErrorMessage("push buffer full").build());
            responseObserver.onCompleted();
            return;
        }

        long ttlMillis = request.getTtlMillis() > 0 ? request.getTtlMillis() : DEFAULT_PUSH_TTL_MILLIS;
        long expiresAt = System.currentTimeMillis() + ttlMillis;
        long sizeBytes = request.getData().size();

        try {
            ensurePushDir();
            evictIfNeeded(sizeBytes);

            // Verify checksum while streaming.
            String expectedChecksum = request.getChecksum();
            if (expectedChecksum != null && !expectedChecksum.isBlank()) {
                try (InputStream in = request.getData().newInput()) {
                    String actual = sha256Hex(in);
                    if (!actual.equalsIgnoreCase(expectedChecksum)) {
                        responseObserver.onNext(PushChunkDataResponse.newBuilder().setSuccess(false)
                                .setErrorMessage("Checksum mismatch (expected=" + expectedChecksum + ", actual=" + actual + ")")
                                .build());
                        responseObserver.onCompleted();
                        return;
                    }
                }
            }

            Path path = pushDir.resolve(dataId + ".bin");
            try (InputStream in = request.getData().newInput();
                 OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                in.transferTo(out);
            }
            pushedData.put(dataId, new PushedData(path, sizeBytes, expiresAt));

            // Pipeline forward (optional).
            if (request.getForwardChainCount() > 0) {
                String next = request.getForwardChain(0);
                List<String> rest = request.getForwardChainList().subList(1, request.getForwardChainCount());
                ManagedChannel channel = channelCache.computeIfAbsent(next,
                        key -> ManagedChannelBuilder.forTarget(key).usePlaintext().build());
                PushChunkDataResponse fwd = ChunkServiceGrpc.newBlockingStub(channel)
                        .pushChunkData(PushChunkDataRequest.newBuilder()
                                .setDataId(dataId)
                                .setData(request.getData())
                                .setChecksum(request.getChecksum())
                                .setTtlMillis(ttlMillis)
                                .addAllForwardChain(rest)
                                .build());
                if (!fwd.getSuccess()) {
                    responseObserver.onNext(PushChunkDataResponse.newBuilder().setSuccess(false)
                            .setErrorMessage("Forward to " + next + " failed: " + fwd.getErrorMessage())
                            .build());
                    responseObserver.onCompleted();
                    return;
                }
            }

            responseObserver.onNext(PushChunkDataResponse.newBuilder().setSuccess(true).build());
            responseObserver.onCompleted();
        } catch (IllegalStateException e) {
            responseObserver.onNext(PushChunkDataResponse.newBuilder().setSuccess(false).setErrorMessage(e.getMessage()).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription("Failed to push data: " + e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void writeChunkFromDataId(WriteChunkFromDataIdRequest request, StreamObserver<WriteChunkResponse> responseObserver) {
        try {
            enforceWriteFencing(request.getChunkId(), request.getSerialNumber());
            PushedData pd = getPushedDataOrThrow(request.getDataId());

            // Verify checksum on staged file (streaming).
            String expectedChecksum = request.getChecksum();
            if (expectedChecksum != null && !expectedChecksum.isBlank()) {
                try (InputStream in = Files.newInputStream(pd.path())) {
                    String actual = sha256Hex(in);
                    if (!actual.equalsIgnoreCase(expectedChecksum)) {
                        throw new IllegalStateException("Checksum mismatch (expected=" + expectedChecksum + ", actual=" + actual + ")");
                    }
                }
            }

            long newVersion = persistChunkFromFile(request.getChunkId(), pd.path(), request.getChecksum(), request.getVersionNumber());
            pushedData.remove(request.getDataId());
            tryDeleteQuietly(pd.path());

            if (!request.getReplicatedWrite()) {
                replicateCommandToSecondaries(request, newVersion);
            }

            responseObserver.onNext(WriteChunkResponse.newBuilder().setSuccess(true).build());
            responseObserver.onCompleted();
        } catch (IllegalStateException e) {
            responseObserver.onNext(WriteChunkResponse.newBuilder().setSuccess(false).setErrorMessage(e.getMessage()).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription("Failed to write chunk from dataId: " + e.getMessage()).asRuntimeException());
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

    private long persistChunkStreaming(String chunkId, ByteString data, String expectedChecksum, long incomingVersion) throws IOException {
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }

        Path chunkPath = storageDir.resolve(chunkId);
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new IOException("Failed to init SHA-256: " + e.getMessage(), e);
        }

        try (InputStream in = data.newInput();
             OutputStream out = Files.newOutputStream(chunkPath)) {
            byte[] buf = new byte[256 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) {
                md.update(buf, 0, n);
                out.write(buf, 0, n);
            }
        }

        if (expectedChecksum != null && !expectedChecksum.isBlank()) {
            String actual = HexFormat.of().formatHex(md.digest());
            if (!actual.equalsIgnoreCase(expectedChecksum)) {
                throw new IllegalStateException("Checksum mismatch (expected=" + expectedChecksum + ", actual=" + actual + ")");
            }
        }

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

        String metaContent = version + "\n" + expectedChecksum + "\n" + System.currentTimeMillis();
        Files.writeString(metaPath, metaContent);
        return version;
    }

    private void enforceWriteFencing(String chunkId, long incomingSerial) throws IOException {
        long currentSerial = readCurrentSerial(chunkId);
        if (incomingSerial < currentSerial) {
            throw new IllegalStateException("Stale lease fencing token for chunk " + chunkId +
                    " (incomingSerial=" + incomingSerial + ", currentSerial=" + currentSerial + ")");
        }
        writeCurrentSerial(chunkId, incomingSerial);
    }

    private long readCurrentSerial(String chunkId) throws IOException {
        if (!Files.exists(storageDir)) {
            return 0L;
        }
        Path serialPath = storageDir.resolve(chunkId + ".serial");
        if (!Files.exists(serialPath)) {
            return 0L;
        }
        try {
            String content = Files.readString(serialPath, StandardCharsets.UTF_8).trim();
            if (content.isEmpty()) {
                return 0L;
            }
            return Long.parseLong(content);
        } catch (Exception e) {
            return 0L;
        }
    }

    private void writeCurrentSerial(String chunkId, long serial) throws IOException {
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }
        Path serialPath = storageDir.resolve(chunkId + ".serial");
        Files.writeString(serialPath, Long.toString(serial), StandardCharsets.UTF_8);
    }

    private void verifyChecksum(byte[] data, String expectedSha256Hex) {
        if (expectedSha256Hex == null || expectedSha256Hex.isBlank()) {
            return;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String actual = HexFormat.of().formatHex(md.digest(data));
            if (!actual.equalsIgnoreCase(expectedSha256Hex)) {
                throw new IllegalStateException("Checksum mismatch (expected=" + expectedSha256Hex + ", actual=" + actual + ")");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Checksum verification failed: " + e.getMessage());
        }
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

    private void replicateCommandToSecondaries(WriteChunkFromDataIdRequest request, long versionNumber) {
        for (String secondary : request.getSecondaryReplicasList()) {
            ManagedChannel channel = channelCache.computeIfAbsent(secondary,
                    key -> ManagedChannelBuilder.forTarget(key).usePlaintext().build());

            WriteChunkResponse response = ChunkServiceGrpc.newBlockingStub(channel)
                    .writeChunkFromDataId(WriteChunkFromDataIdRequest.newBuilder()
                            .setChunkId(request.getChunkId())
                            .setDataId(request.getDataId())
                            .setReplicatedWrite(true)
                            .setVersionNumber(versionNumber)
                            .setSerialNumber(request.getSerialNumber())
                            .setChecksum(request.getChecksum())
                            .build());

            if (!response.getSuccess()) {
                throw new IllegalStateException("Replica write rejected by " + secondary + ": " + response.getErrorMessage());
            }
        }
    }

    private void ensurePushDir() throws IOException {
        if (!Files.exists(pushDir)) {
            Files.createDirectories(pushDir);
        }
    }

    private PushedData getPushedDataOrThrow(String dataId) {
        if (dataId == null || dataId.isBlank()) {
            throw new IllegalStateException("dataId is required");
        }
        PushedData pd = pushedData.get(dataId);
        if (pd == null) {
            throw new IllegalStateException("missing pushed data for dataId=" + dataId);
        }
        if (pd.expiresAtMillis() < System.currentTimeMillis()) {
            pushedData.remove(dataId);
            tryDeleteQuietly(pd.path());
            throw new IllegalStateException("pushed data expired for dataId=" + dataId);
        }
        return pd;
    }

    private long totalPushedBytes() {
        long total = 0L;
        for (PushedData pd : pushedData.values()) {
            total += pd.sizeBytes();
        }
        return total;
    }

    private void evictIfNeeded(long incomingBytes) {
        long total = totalPushedBytes();
        if (total + incomingBytes <= MAX_PUSHED_BYTES) {
            return;
        }
        throw new IllegalStateException("push buffer full");
    }

    private void tryDeleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }

    private long persistChunkFromFile(String chunkId, Path sourcePath, String checksum, long incomingVersion) throws IOException {
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }

        Path chunkPath = storageDir.resolve(chunkId);
        try (InputStream in = Files.newInputStream(sourcePath);
             OutputStream out = Files.newOutputStream(chunkPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            in.transferTo(out);
        }

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

        String metaContent = version + "\n" + (checksum == null ? "" : checksum) + "\n" + System.currentTimeMillis();
        Files.writeString(metaPath, metaContent);
        return version;
    }

    private String sha256Hex(InputStream in) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[256 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) {
                md.update(buf, 0, n);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("sha256 failed: " + e.getMessage(), e);
        }
    }
}
