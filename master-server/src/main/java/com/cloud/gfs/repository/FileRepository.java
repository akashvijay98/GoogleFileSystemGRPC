package com.cloud.gfs.repository;

import com.cloud.gfs.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileRepository  extends JpaRepository<FileMetadata, Long> {
    Optional<FileMetadata> findByFileName(String fileName);
}
