package io.github.popala1986.heartbeat.simulator;

import io.github.popala1986.heartbeat.grpc.v1.HeartbeatServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.swing.SwingUtilities;
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

        SimulatorWindow window = new SimulatorWindow(sender, scheduler);
        SwingUtilities.invokeLater(window::show);

        log.info("grpc-simulator window ready, server {}:{}", HOST, PORT);

        Thread shutdownHook = new Thread(() -> {
            log.info("Stopping grpc-simulator");
            scheduler.shutdownNow();
            channel.shutdownNow();
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }
}
