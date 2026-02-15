package com.cloud.gfs;

import com.cloud.gfs.controller.FileService;
import com.gfs.grpc.MasterServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @Bean
    public MasterServiceGrpc.MasterServiceBlockingStub masterServiceStub() {
        ManagedChannel masterChannel = ManagedChannelBuilder.forTarget("master-server:8081")
                .usePlaintext()
                .build();
        return MasterServiceGrpc.newBlockingStub(masterChannel);
    }
}
