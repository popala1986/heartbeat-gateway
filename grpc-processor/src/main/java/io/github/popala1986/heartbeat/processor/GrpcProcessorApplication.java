package io.github.popala1986.heartbeat.processor;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;
import java.io.IOException;
import java.time.Clock;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GrpcProcessorApplication {

    private static final Logger log =
            LoggerFactory.getLogger(GrpcProcessorApplication.class);
    private static final int PORT = 9090;

    private GrpcProcessorApplication() {
    }

    public static void main(String[] args)
            throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(PORT)
                .addService(
                        new HeartbeatServiceImpl(Clock.systemUTC()))
                .addService(ProtoReflectionServiceV1.newInstance())
                .build()
                .start();

        log.info("grpc-processor listening on port {}", PORT);

        Thread shutdownHook = new Thread(() -> {
            log.info("Shutting down grpc-processor");
            try {
                server.shutdown()
                        .awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        server.awaitTermination();
    }
}
