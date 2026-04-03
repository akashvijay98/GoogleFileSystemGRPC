package service;

import com.gfs.grpc.HeartbeatRequest;
import com.gfs.grpc.HeartbeatResponse;
import com.gfs.grpc.MasterServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import net.devh.boot.grpc.server.event.GrpcServerStartedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HeartbeatScheduler {

    @Value("${master.server.address:master-server:8081}")
    private String masterAddress;

    private final MasterServiceGrpc.MasterServiceBlockingStub masterStub;
    private int runningPort = 0;

    String myHostName = System.getenv().getOrDefault("HOSTNAME", "localhost");

    public HeartbeatScheduler() {
        ManagedChannel channel = ManagedChannelBuilder.forTarget("master-server:8081")
                .usePlaintext()
                .build();
        this.masterStub = MasterServiceGrpc.newBlockingStub(channel);
    }

    @EventListener
    public void onServerStarted(GrpcServerStartedEvent event) {
        this.runningPort = event.getServer().getPort();
        System.out.println("Detected Chunk Server gRPC running on port: " + this.runningPort);
    }

    @Scheduled(fixedRate = 5000)
    public void sendHeartbeat() {
        if (runningPort == 0) {
            return;
        }
        try {
            HeartbeatResponse response = masterStub.sendHeartbeat(
                    HeartbeatRequest.newBuilder()
                            .setServerAddress(myHostName + ":" + runningPort)
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
