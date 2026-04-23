package com.cloud.gfs.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "replica_metadata")
public class ReplicaMetadata {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String chunkId;

	@Column(nullable = false)
	private String replicaServerAddress;

	@Column(nullable = false)
	private Long versionNumber = 1L;

	private String checksum;

	@Column(nullable = false)
	private boolean stale = false;

	@Column(nullable = false)
	private LocalDateTime lastVerified = LocalDateTime.now();

	@Column(nullable = false)
	private LocalDateTime createdTime = LocalDateTime.now();

	public ReplicaMetadata(String chunkId, String replicaServerAddress) {
		this.chunkId = chunkId;
		this.replicaServerAddress = replicaServerAddress;
		this.versionNumber = 1L;
		this.lastVerified = LocalDateTime.now();
		this.createdTime = LocalDateTime.now();
	}
}


