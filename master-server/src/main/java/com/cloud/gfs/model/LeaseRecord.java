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
@Table(name = "lease_records")
public class LeaseRecord {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String chunkId;

	@Column(nullable = false)
	private String primaryServerAddress;

	@Column(nullable = false)
	private Long serialNumber;

	private String clientId;

	@Column(nullable = false)
	private LocalDateTime expirationTime;

	@Column(nullable = false)
	private LocalDateTime createdTime = LocalDateTime.now();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private LeaseStatus status = LeaseStatus.ACTIVE;

	public enum LeaseStatus {
		ACTIVE,
		REVOKED,
		EXPIRED
	}

	public LeaseRecord(String chunkId, String primaryServerAddress, Long serialNumber, LocalDateTime expirationTime) {
		this.chunkId = chunkId;
		this.primaryServerAddress = primaryServerAddress;
		this.serialNumber = serialNumber;
		this.expirationTime = expirationTime;
		this.createdTime = LocalDateTime.now();
		this.status = LeaseStatus.ACTIVE;
	}

	public boolean isExpired() {
		return LocalDateTime.now().isAfter(expirationTime);
	}

	public void extend(LocalDateTime newExpirationTime) {
		this.expirationTime = newExpirationTime;
		this.status = LeaseStatus.ACTIVE;
	}

	public void revoke() {
		this.status = LeaseStatus.REVOKED;
	}
}


