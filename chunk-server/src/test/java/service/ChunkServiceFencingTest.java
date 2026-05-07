package service;

import com.gfs.grpc.WriteChunkRequest;
import com.gfs.grpc.WriteChunkResponse;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChunkServiceFencingTest {

	private final ChunkService service = new ChunkService();

	@AfterEach
	void cleanup() throws Exception {
		Path dir = Path.of("data/chunks");
		if (!Files.exists(dir)) {
			return;
		}
		try (var stream = Files.list(dir)) {
			stream.forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (Exception ignored) {
				}
			});
		}
	}

	@Test
	void rejectsStaleFencingToken() throws Exception {
		byte[] data = "hello".getBytes();
		String checksum = sha256(data);

		WriteChunkResponse ok = invokeWrite(WriteChunkRequest.newBuilder()
				.setChunkId("c1")
				.setData(ByteString.copyFrom(data))
				.setReplicatedWrite(true)
				.setVersionNumber(1L)
				.setSerialNumber(5L)
				.setChecksum(checksum)
				.build());
		assertTrue(ok.getSuccess());

		WriteChunkResponse stale = invokeWrite(WriteChunkRequest.newBuilder()
				.setChunkId("c1")
				.setData(ByteString.copyFrom(data))
				.setReplicatedWrite(true)
				.setVersionNumber(2L)
				.setSerialNumber(4L)
				.setChecksum(checksum)
				.build());
		assertFalse(stale.getSuccess());
		assertNotNull(stale.getErrorMessage());
	}

	@Test
	void rejectsChecksumMismatch() throws Exception {
		byte[] data = "hello".getBytes();

		WriteChunkResponse resp = invokeWrite(WriteChunkRequest.newBuilder()
				.setChunkId("c2")
				.setData(ByteString.copyFrom(data))
				.setReplicatedWrite(true)
				.setVersionNumber(1L)
				.setSerialNumber(1L)
				.setChecksum("deadbeef")
				.build());
		assertFalse(resp.getSuccess());
		assertNotNull(resp.getErrorMessage());
	}

	private WriteChunkResponse invokeWrite(WriteChunkRequest request) throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		WriteChunkResponse[] holder = new WriteChunkResponse[1];
		Throwable[] err = new Throwable[1];

		service.writeChunk(request, new StreamObserver<>() {
			@Override
			public void onNext(WriteChunkResponse value) {
				holder[0] = value;
			}

			@Override
			public void onError(Throwable t) {
				err[0] = t;
				latch.countDown();
			}

			@Override
			public void onCompleted() {
				latch.countDown();
			}
		});

		assertTrue(latch.await(5, TimeUnit.SECONDS));
		if (err[0] != null) {
			throw new RuntimeException(err[0]);
		}
		return holder[0];
	}

	private static String sha256(byte[] data) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		return HexFormat.of().formatHex(md.digest(data));
	}
}

