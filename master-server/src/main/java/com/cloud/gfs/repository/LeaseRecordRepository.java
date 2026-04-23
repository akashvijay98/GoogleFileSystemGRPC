package com.cloud.gfs.repository;

import com.cloud.gfs.model.LeaseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LeaseRecordRepository extends JpaRepository<LeaseRecord, Long> {
	Optional<LeaseRecord> findByChunkIdAndStatus(String chunkId, LeaseRecord.LeaseStatus status);

	@Query("SELECT l FROM LeaseRecord l WHERE l.chunkId = :chunkId AND l.status = 'ACTIVE'")
	Optional<LeaseRecord> findActiveLeaseByChunkId(@Param("chunkId") String chunkId);

	@Query("SELECT l FROM LeaseRecord l WHERE l.expirationTime < :now AND l.status = 'ACTIVE'")
	List<LeaseRecord> findExpiredLeases(@Param("now") LocalDateTime now);

	@Query("SELECT l FROM LeaseRecord l WHERE l.primaryServerAddress = :serverAddress AND l.status = 'ACTIVE'")
	List<LeaseRecord> findLeasesByPrimaryServer(@Param("serverAddress") String serverAddress);
}


