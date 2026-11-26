package io.github.popala1986.heartbeat.simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.popala1986.heartbeat.grpc.v1.HeartbeatRequest;
import io.github.popala1986.heartbeat.grpc.v1.HeartbeatResponse;
import io.github.popala1986.heartbeat.grpc.v1.HeartbeatServiceGrpc;
import io.github.popala1986.heartbeat.grpc.v1.HeartbeatStatus;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.time.Clock;
import java.time.Duration;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SimulatorWindowTest {

    private final String name = InProcessServerBuilder.generateName();
    private final ManagedChannel channel =
            InProcessChannelBuilder.forName(name)
                    .directExecutor()
                    .build();
    private final SimulatorWindow window = new SimulatorWindow(
            new HeartbeatSender(
                    HeartbeatServiceGrpc.newBlockingStub(channel),
                    "device-test",
                    Clock.systemUTC(),
                    Duration.ofSeconds(1)),
            null);
    private Server server;

    @AfterEach
    void cleanUp() {
        channel.shutdownNow();
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    void showsAcceptedHeartbeatOnTheList() throws Exception {
        server = InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(new FakeHeartbeatService())
                .build()
                .start();

        window.sendAndShow();
        SwingUtilities.invokeAndWait(() -> { });

        assertEquals(1, window.responses.size());
        assertTrue(window.responses.get(0).startsWith(
                "Heartbeat status=HEARTBEAT_STATUS_ACCEPTED"));
    }

    @Test
    void showsErrorWhenServerIsDown() throws Exception {
        window.sendAndShow();
        SwingUtilities.invokeAndWait(() -> { });

        assertEquals("Heartbeat failed: UNAVAILABLE",
                window.responses.get(0));
    }

    private static class FakeHeartbeatService
            extends HeartbeatServiceGrpc.HeartbeatServiceImplBase {

        @Override
        public void sendHeartbeat(
                HeartbeatRequest request,
                StreamObserver<HeartbeatResponse> responseObserver) {
            responseObserver.onNext(HeartbeatResponse.newBuilder()
                    .setCorrelationId(request.getCorrelationId())
                    .setStatus(HeartbeatStatus.HEARTBEAT_STATUS_ACCEPTED)
                    .build());
            responseObserver.onCompleted();
        }
    }
}
