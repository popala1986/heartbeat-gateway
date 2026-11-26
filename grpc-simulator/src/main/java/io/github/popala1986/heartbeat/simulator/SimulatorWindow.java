package io.github.popala1986.heartbeat.simulator;

import io.github.popala1986.heartbeat.grpc.v1.HeartbeatResponse;
import io.grpc.StatusRuntimeException;
import java.awt.BorderLayout;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SimulatorWindow {

    private static final Logger log =
            LoggerFactory.getLogger(SimulatorWindow.class);

    final DefaultListModel<String> responses = new DefaultListModel<>();

    private final HeartbeatSender sender;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> sending;

    SimulatorWindow(
            HeartbeatSender sender,
            ScheduledExecutorService scheduler) {
        this.sender = sender;
        this.scheduler = scheduler;
    }

    void show() {
        JButton startButton = new JButton("Start");
        JButton stopButton = new JButton("Stop");
        JButton oneButton = new JButton("Wyślij jeden heartbeat");

        startButton.addActionListener(e -> start());
        stopButton.addActionListener(e -> stop());
        oneButton.addActionListener(
                e -> scheduler.execute(this::sendAndShow));

        JPanel buttons = new JPanel();
        buttons.add(startButton);
        buttons.add(stopButton);
        buttons.add(oneButton);

        JFrame frame = new JFrame("grpc-simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(buttons, BorderLayout.NORTH);
        frame.add(new JScrollPane(new JList<>(responses)),
                BorderLayout.CENTER);
        frame.setSize(700, 400);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void start() {
        if (sending != null) {
            return;
        }
        sending = scheduler.scheduleAtFixedRate(
                this::sendAndShow, 0, 1, TimeUnit.SECONDS);
    }

    private void stop() {
        if (sending == null) {
            return;
        }
        sending.cancel(false);
        sending = null;
    }

    void sendAndShow() {
        String line = sendSafely();
        SwingUtilities.invokeLater(() -> responses.add(0, line));
    }

    private String sendSafely() {
        try {
            HeartbeatResponse response = sender.send();
            return "Heartbeat status=" + response.getStatus()
                    + " correlationId=" + response.getCorrelationId();
        } catch (StatusRuntimeException e) {
            log.warn("Heartbeat failed: {}", e.getStatus().getCode());
            return "Heartbeat failed: " + e.getStatus().getCode();
        }
    }
}
