package service;

import com.gfs.grpc.HeartbeatRequest;
import com.gfs.grpc.HeartbeatResponse;
import com.gfs.grpc.MasterServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HeartbeatScheduler {

    @Value("${master.server.address:master-server:8081}")
    private String masterAddress;

    private final MasterServiceGrpc.MasterServiceBlockingStub masterStub;

    public HeartbeatScheduler() {
        ManagedChannel channel = ManagedChannelBuilder.forTarget("master-server:8081")
                .usePlaintext()
                .build();
        this.masterStub = MasterServiceGrpc.newBlockingStub(channel);
    }

    @Scheduled(fixedRate = 5000)
    public void sendHeartbeat() {
        try {
            HeartbeatResponse response = masterStub.sendHeartbeat(
                    HeartbeatRequest.newBuilder()
                            .setServerAddress("chunk-server:" + System.getenv("SERVER_NAME"))
                            .build()
            );

            if (response.getSuccess()) {
                System.out.println("Heartbeat sent successfully to master");
            }
        } catch (Exception e) {
            System.err.println("Failed to send heartbeat: " + e.getMessage());
        }
    }
}
