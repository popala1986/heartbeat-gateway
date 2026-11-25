package io.github.popala1986.heartbeat.simulator;

import io.github.popala1986.heartbeat.grpc.v1.HeartbeatServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GrpcSimulatorApplication {

    private static final Logger log =
            LoggerFactory.getLogger(GrpcSimulatorApplication.class);
    private static final String HOST = "localhost";
    private static final int PORT = 9090;
    private static final String DEVICE_ID = "simulator-1";
    private static final Duration DEADLINE = Duration.ofSeconds(2);

    private GrpcSimulatorApplication() {
    }

    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(HOST, PORT)
                .usePlaintext()
                .build();

        HeartbeatSender sender = new HeartbeatSender(
                HeartbeatServiceGrpc.newBlockingStub(channel),
                DEVICE_ID,
                Clock.systemUTC(),
                DEADLINE);

        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                () -> sendSafely(sender), 0, 1, TimeUnit.SECONDS);

        log.info("grpc-simulator sending heartbeats to {}:{}", HOST, PORT);

        Thread shutdownHook = new Thread(() -> {
            log.info("Stopping grpc-simulator");
            scheduler.shutdownNow();
            channel.shutdownNow();
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private static void sendSafely(HeartbeatSender sender) {
        try {
            sender.send();
        } catch (StatusRuntimeException e) {
            log.warn("Heartbeat failed: {}", e.getStatus().getCode());
        }
    }
}
