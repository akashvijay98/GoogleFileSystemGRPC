package service;

import com.gfs.grpc.ChunkData;
import com.gfs.grpc.ChunkServiceGrpc;
import com.gfs.grpc.UploadStatus;
import io.grpc.stub.StreamObserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class ChunkService extends ChunkServiceGrpc.ChunkServiceImplBase {

    private final Path storageDir = Paths.get("data/chunks");


    @Override
    public StreamObserver<ChunkData> uploadChunk(StreamObserver<UploadStatus> responseObserver){
        return new StreamObserver<ChunkData>() {
            private String chunkId;
            private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            @Override
            public void onNext(ChunkData chunkData) {

                if (chunkId == null) {
                    chunkId = chunkData.getChunkId();
                }

                buffer.writeBytes(chunkData.getData().toByteArray());
            }
            @Override
            public void onCompleted() {
                try {
                    if (!Files.exists(storageDir)) {
                        Files.write(storageDir.resolve(chunkId), buffer.toByteArray());
                    }


                    responseObserver.onNext(UploadStatus.newBuilder().setSuccess(true).build());
                    responseObserver.onCompleted();
                }
                catch (IOException e) {

                    System.err.println("Disk write failed: " + e.getMessage());
                    responseObserver.onError(io.grpc.Status.INTERNAL
                            .withDescription("Could not write chunk to disk")
                            .asRuntimeException());
                }

                finally {
                    try {
                        buffer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                // Log the error so you know why the upload failed
                System.err.println("Upload failed for chunk " + chunkId + ": " + t.getMessage());

                // Clean up resources
                try {
                    buffer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        };
    }
}
