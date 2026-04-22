package com.cloud.gfs.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
public class ChunkRecord {
    @Id
    private String chunkId;
    private int sequenceNumber;
    private String primaryServerAddress;
    private long leaseExpirationEpochMillis;

    @ElementCollection
    private List<String> replicaServerAddresses = new ArrayList<>();

}
