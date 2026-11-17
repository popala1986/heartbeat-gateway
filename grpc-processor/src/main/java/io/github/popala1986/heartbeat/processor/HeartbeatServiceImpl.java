package io.github.popala1986.heartbeat.processor;

import com.google.protobuf.Timestamp;
import io.github.popala1986.heartbeat.grpc.v1.HeartbeatRequest;
import io.github.popala1986.heartbeat.grpc.v1.HeartbeatResponse;
import io.github.popala1986.heartbeat.grpc.v1.HeartbeatServiceGrpc;
import io.github.popala1986.heartbeat.grpc.v1.HeartbeatStatus;
import io.grpc.stub.StreamObserver;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HeartbeatServiceImpl
        extends HeartbeatServiceGrpc.HeartbeatServiceImplBase {

    private static final Logger log =
            LoggerFactory.getLogger(HeartbeatServiceImpl.class);

    private final Clock clock;

    HeartbeatServiceImpl(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void sendHeartbeat(
            HeartbeatRequest request,
            StreamObserver<HeartbeatResponse> responseObserver) {
        log.info("Received heartbeat correlationId={} "
                        + "deviceId={} sequence={}",
                request.getCorrelationId(),
                request.getDeviceId(),
                request.getSequence());

        HeartbeatResponse response = HeartbeatResponse
                .newBuilder()
                .setCorrelationId(request.getCorrelationId())
                .setStatus(HeartbeatStatus.HEARTBEAT_STATUS_ACCEPTED)
                .setAcknowledgedAt(toTimestamp(clock.instant()))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private static Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
