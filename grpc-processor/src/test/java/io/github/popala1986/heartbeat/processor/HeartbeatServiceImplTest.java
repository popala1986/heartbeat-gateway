package io.github.popala1986.heartbeat.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.popala1986.heartbeat.grpc.v1.HeartbeatRequest;
import io.github.popala1986.heartbeat.grpc.v1.HeartbeatResponse;
import io.github.popala1986.heartbeat.grpc.v1.HeartbeatServiceGrpc;
import io.github.popala1986.heartbeat.grpc.v1.HeartbeatStatus;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HeartbeatServiceImplTest {

    private static final Instant NOW =
            Instant.parse("2026-01-01T12:00:00Z");

    private Server server;
    private ManagedChannel channel;
    private HeartbeatServiceGrpc
            .HeartbeatServiceBlockingStub stub;

    @BeforeEach
    void startServer() throws IOException {
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(new HeartbeatServiceImpl(
                        Clock.fixed(NOW, ZoneOffset.UTC)))
                .build()
                .start();
        channel = InProcessChannelBuilder.forName(name)
                .directExecutor()
                .build();
        stub = HeartbeatServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void stopServer() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void acceptsHeartbeatAndEchoesCorrelationId() {
        HeartbeatRequest request = HeartbeatRequest.newBuilder()
                .setCorrelationId("abc-123")
                .setDeviceId("device-1")
                .setSequence(1)
                .build();

        HeartbeatResponse response = stub.sendHeartbeat(request);

        assertEquals("abc-123", response.getCorrelationId());
        assertEquals(HeartbeatStatus.HEARTBEAT_STATUS_ACCEPTED,
                response.getStatus());
        assertEquals(NOW.getEpochSecond(),
                response.getAcknowledgedAt().getSeconds());
    }
}
