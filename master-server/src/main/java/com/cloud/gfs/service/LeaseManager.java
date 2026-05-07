package com.cloud.gfs.service;

import com.cloud.gfs.model.LeaseRecord;
import com.cloud.gfs.repository.LeaseRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Optional;

@Service
public class LeaseManager {

    @Autowired
    private LeaseRecordRepository leaseRecordRepository;

    /**
     * Issue a lease for a chunk to a primary server.
     * @param chunkId chunk identifier
     * @param primaryServer primary server address
     * @param durationMillis lease duration in milliseconds
     * @return persisted LeaseRecord
     */
    public LeaseRecord issueLease(String chunkId, String primaryServer, long durationMillis) {
        LocalDateTime expiration = LocalDateTime.now().plus(Duration.ofMillis(durationMillis));
        long nextSerial = nextLeaseSerial(chunkId);
        LeaseRecord lease = new LeaseRecord(chunkId, primaryServer, nextSerial, expiration);
        return leaseRecordRepository.save(lease);
    }

    private long nextLeaseSerial(String chunkId) {
        Optional<LeaseRecord> lastLease = leaseRecordRepository.findTopByChunkIdOrderBySerialNumberDesc(chunkId);
        return lastLease.map(l -> l.getSerialNumber() + 1L).orElse(1L);
    }
}
