package com.cloud.gfs.service;

import com.cloud.gfs.model.ChunkServerNode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
public class ServerHealthMonitor {

    private final Map<String, ServerHealth> serverHealthMap = new ConcurrentHashMap<>();
    private Consumer<String> onServerDeadCallback;

    private static final long DEAD_THRESHOLD_SECONDS = 10;

    public void recordHeartbeat(String serverAddress) {
        ServerHealth health = serverHealthMap.computeIfAbsent(serverAddress, 
            k -> new ServerHealth(ChunkServerNode.Status.ACTIVE));
        health.updateHeartbeat();
    }

    public boolean isServerHealthy(String serverAddress) {
        ServerHealth health = serverHealthMap.get(serverAddress);
        if (health == null) {
            return false;
        }
        return health.isHealthy();
    }

    public ChunkServerNode.Status getServerStatus(String serverAddress) {
        ServerHealth health = serverHealthMap.get(serverAddress);
        if (health == null) {
            return ChunkServerNode.Status.DEAD;
        }
        return health.getStatus();
    }

    public Map<String, ServerHealth> getAllServers() {
        return Map.copyOf(serverHealthMap);
    }

    public void setOnServerDead(Consumer<String> callback) {
        this.onServerDeadCallback = callback;
    }

    @Scheduled(fixedRate = 5000)
    public void checkServerHealth() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(DEAD_THRESHOLD_SECONDS);
        
        serverHealthMap.forEach((address, health) -> {
            if (health.isHealthy() && health.getLastHeartbeat().isBefore(threshold)) {
                health.markDead();
                System.out.println("Server marked as DEAD: " + address);
                if (onServerDeadCallback != null) {
                    onServerDeadCallback.accept(address);
                }
            }
        });
    }

    public static class ServerHealth {
        private LocalDateTime lastHeartbeat;
        private ChunkServerNode.Status status;

        ServerHealth(ChunkServerNode.Status initialStatus) {
            this.status = initialStatus;
            this.lastHeartbeat = LocalDateTime.now();
        }

        void updateHeartbeat() {
            this.lastHeartbeat = LocalDateTime.now();
            this.status = ChunkServerNode.Status.ACTIVE;
        }

        boolean isHealthy() {
            return status == ChunkServerNode.Status.ACTIVE;
        }

        ChunkServerNode.Status getStatus() {
            return status;
        }

        LocalDateTime getLastHeartbeat() {
            return lastHeartbeat;
        }

        void markDead() {
            this.status = ChunkServerNode.Status.DEAD;
        }
    }
}
