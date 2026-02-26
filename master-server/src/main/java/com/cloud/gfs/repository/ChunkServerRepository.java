package com.cloud.gfs.repository;

import com.cloud.gfs.model.ChunkServerNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChunkServerRepository extends JpaRepository<ChunkServerNode, Long> {

    Optional<ChunkServerNode> findByServerAddress(String serverAddress);

    List<ChunkServerNode> findByStatus(ChunkServerNode.Status status);

    @Query("SELECT n FROM ChunkServerNode n WHERE n.lastHeartbeatTime < :threshold AND n.status = 'ACTIVE'")
    List<ChunkServerNode> findInactiveSince(LocalDateTime threshold);
}
