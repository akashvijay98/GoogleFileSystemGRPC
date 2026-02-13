package com.cloud.gfs.service;

import com.cloud.gfs.model.ChunkRecord;
import com.cloud.gfs.model.FileMetadata;
import com.cloud.gfs.repository.FileRepository;
import com.gfs.grpc.ChunkLocationResponse;
import com.gfs.grpc.FileRequest;
import com.gfs.grpc.MasterServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class MasterService  extends MasterServiceGrpc.MasterServiceImplBase {
    private final List<String> chunkServers = List.of("localhost:9001", "localhost:9002", "localhost:9003");
    @Autowired
    private FileRepository fileRepository;


    @Override
    @Transactional
    public void getUploadLocation(FileRequest request, StreamObserver<ChunkLocationResponse> responseObserver){
        String fileName = request.getFileName();

        FileMetadata file = fileRepository.findByFileName(fileName)
                .orElseGet(() -> {
                    FileMetadata newFile = new FileMetadata();
                    newFile.setFileName(fileName);
                    return fileRepository.save(newFile);
                } );
        String assignedServer = chunkServers.get(new Random().nextInt(3));
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
}
