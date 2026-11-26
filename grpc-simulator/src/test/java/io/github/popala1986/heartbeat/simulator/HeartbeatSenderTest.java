package io.github.popala1986.heartbeat.simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.popala1986.heartbeat.grpc.v1.HeartbeatRequest;
import io.github.popala1986.heartbeat.grpc.v1.HeartbeatResponse;
import io.github.popala1986.heartbeat.grpc.v1.HeartbeatServiceGrpc;
import io.github.popala1986.heartbeat.grpc.v1.HeartbeatStatus;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HeartbeatSenderTest {

    private static final Instant NOW =
            Instant.parse("2026-01-01T12:00:00Z");

    private final List<HeartbeatRequest> received = new ArrayList<>();
    private Server server;
    private ManagedChannel channel;
    private HeartbeatSender sender;

    @BeforeEach
    void startFakeServer() throws IOException {
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(new FakeHeartbeatService())
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(name)
                .directExecutor()
                .build();
        sender = new HeartbeatSender(
                HeartbeatServiceGrpc.newBlockingStub(channel),
                "device-test",
                Clock.fixed(NOW, ZoneOffset.UTC),
                Duration.ofSeconds(1));
    }

    @AfterEach
    void stopFakeServer() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void sendsHeartbeatAndReturnsServerResponse() {
        HeartbeatResponse response = sender.send();

        HeartbeatRequest request = received.get(0);
        assertEquals("device-test", request.getDeviceId());
        assertEquals(1, request.getSequence());
        assertEquals(NOW.getEpochSecond(), request.getSentAt().getSeconds());
        assertFalse(request.getCorrelationId().isEmpty());
        assertEquals(request.getCorrelationId(), response.getCorrelationId());
        assertEquals(HeartbeatStatus.HEARTBEAT_STATUS_ACCEPTED,
                response.getStatus());
    }

    @Test
    void increasesSequenceWithEveryHeartbeat() {
        sender.send();
        sender.send();

        assertEquals(1, received.get(0).getSequence());
        assertEquals(2, received.get(1).getSequence());
    }

    private class FakeHeartbeatService
            extends HeartbeatServiceGrpc.HeartbeatServiceImplBase {

        @Override
        public void sendHeartbeat(
                HeartbeatRequest request,
                StreamObserver<HeartbeatResponse> responseObserver) {
            received.add(request);
            responseObserver.onNext(HeartbeatResponse.newBuilder()
                    .setCorrelationId(request.getCorrelationId())
                    .setStatus(HeartbeatStatus.HEARTBEAT_STATUS_ACCEPTED)
                    .build());
            responseObserver.onCompleted();
        }
    }
}
