package com.cloud.gfs.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FileMetadata {
    @Id
    @GeneratedValue
    private Long id;
    private String fileName;

    @OneToMany(cascade = CascadeType.ALL)
    private List<ChunkRecord> chunks;
}
