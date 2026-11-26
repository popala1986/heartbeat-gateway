package io.github.popala1986.heartbeat.simulator;

import com.google.protobuf.Timestamp;
import io.github.popala1986.heartbeat.grpc.v1.HeartbeatRequest;
import io.github.popala1986.heartbeat.grpc.v1.HeartbeatResponse;
import io.github.popala1986.heartbeat.grpc.v1.HeartbeatServiceGrpc;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class HeartbeatSender {

    private static final Logger log =
            LoggerFactory.getLogger(HeartbeatSender.class);

    private final HeartbeatServiceGrpc.HeartbeatServiceBlockingStub stub;
    private final String deviceId;
    private final Clock clock;
    private final Duration deadline;
    private final AtomicLong sequence = new AtomicLong();

    HeartbeatSender(
            HeartbeatServiceGrpc.HeartbeatServiceBlockingStub stub,
            String deviceId,
            Clock clock,
            Duration deadline) {
        this.stub = stub;
        this.deviceId = deviceId;
        this.clock = clock;
        this.deadline = deadline;
    }

    HeartbeatResponse send() {
        HeartbeatRequest request = HeartbeatRequest.newBuilder()
                .setCorrelationId(UUID.randomUUID().toString())
                .setDeviceId(deviceId)
                .setSequence(sequence.incrementAndGet())
                .setSentAt(toTimestamp(clock.instant()))
                .build();

        HeartbeatResponse response = stub
                .withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS)
                .sendHeartbeat(request);

        log.info("Heartbeat sequence={} correlationId={} status={}",
                request.getSequence(),
                response.getCorrelationId(),
                response.getStatus());
        return response;
    }

    private static Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
