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
@Table(name = "operation_log", indexes = {
		@Index(name = "idx_operation_id", columnList = "operationId"),
		@Index(name = "idx_timestamp", columnList = "timestamp"),
		@Index(name = "idx_status", columnList = "status")
})
public class OperationLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String operationId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OperationType operationType;

	@Column(nullable = false)
	private String clientId;

	@Column(nullable = false, unique = true)
	private Long sequenceNumber;

	@Column(columnDefinition = "TEXT")
	private String operationDetails;

	@Column(nullable = false)
	private LocalDateTime timestamp = LocalDateTime.now();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OperationStatus status = OperationStatus.PENDING;

	@Column(columnDefinition = "TEXT")
	private String resultData;

	public enum OperationType {
		CREATE_CHUNK,
		WRITE_CHUNK,
		APPEND_RECORD,
		DELETE_FILE,
		EXTEND_LEASE,
		CREATE_SNAPSHOT,
		CREATE_NAMESPACE,
		DELETE_CHUNK
	}

	public enum OperationStatus {
		PENDING,
		IN_PROGRESS,
		COMPLETED,
		FAILED
	}

	public OperationLog(String operationId, OperationType operationType, String clientId, Long sequenceNumber) {
		this.operationId = operationId;
		this.operationType = operationType;
		this.clientId = clientId;
		this.sequenceNumber = sequenceNumber;
		this.timestamp = LocalDateTime.now();
		this.status = OperationStatus.PENDING;
	}

	public void markCompleted(String result) {
		this.status = OperationStatus.COMPLETED;
		this.resultData = result;
	}

	public void markFailed(String error) {
		this.status = OperationStatus.FAILED;
		this.resultData = error;
	}
}


