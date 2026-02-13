package com.cloud.gfs.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class ChunkRecord {
    @Id
    private String chunkId;
    private String serverAddress;
    private int sequenceNumber;

}
