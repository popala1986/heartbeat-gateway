package io.github.popala1986.heartbeat.simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.popala1986.heartbeat.grpc.v1.HeartbeatResponse;
import io.github.popala1986.heartbeat.grpc.v1.HeartbeatServiceGrpc;
import io.github.popala1986.heartbeat.grpc.v1.HeartbeatStatus;
import io.github.popala1986.heartbeat.processor.HeartbeatServiceImpl;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HeartbeatIntegrationTest {

    private static final Instant NOW =
            Instant.parse("2026-01-01T12:00:00Z");

    private Server server;
    private ManagedChannel channel;
    private HeartbeatSender sender;

    @BeforeEach
    void startServerAndClient() throws IOException {
        server = ServerBuilder.forPort(0)
                .addService(new HeartbeatServiceImpl(
                        Clock.fixed(NOW, ZoneOffset.UTC)))
                .build()
                .start();

        channel = ManagedChannelBuilder
                .forAddress("localhost", server.getPort())
                .usePlaintext()
                .build();

        sender = new HeartbeatSender(
                HeartbeatServiceGrpc.newBlockingStub(channel),
                "device-it",
                Clock.systemUTC(),
                Duration.ofSeconds(2));
    }

    @AfterEach
    void stopServerAndClient() throws InterruptedException {
        channel.shutdownNow()
                .awaitTermination(5, TimeUnit.SECONDS);
        server.shutdownNow()
                .awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void realServerAcceptsHeartbeatFromRealClient() {
        HeartbeatResponse response = sender.send();

        assertEquals(HeartbeatStatus.HEARTBEAT_STATUS_ACCEPTED,
                response.getStatus());
        assertFalse(response.getCorrelationId().isEmpty());
        assertEquals(NOW.getEpochSecond(),
                response.getAcknowledgedAt().getSeconds());
    }

    @Test
    void clientGetsUnavailableWhenServerIsStopped()
            throws InterruptedException {
        server.shutdownNow()
                .awaitTermination(5, TimeUnit.SECONDS);

        StatusRuntimeException error = assertThrows(
                StatusRuntimeException.class, sender::send);

        assertEquals(Status.Code.UNAVAILABLE,
                error.getStatus().getCode());
    }
}
