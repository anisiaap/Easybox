package com.example.easyboxdevice.service;

import com.example.easyboxdevice.service.MqttService;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class QrScannerService {

    private final MqttService mqttService;
    private String lastQr = null;
    private long lastTime = 0;
    private volatile boolean running = true;
    private Thread scannerThread;

    public QrScannerService(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    @PostConstruct
    public void start() {
        scannerThread = new Thread(this::scanLoop, "QrScannerThread");
        scannerThread.start();
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (scannerThread != null) {
            scannerThread.interrupt();
        }
    }

    private void scanLoop() {
        while (running) {
            try {
                captureImage();
                String qr = decodeQrWithZbar();

                if (qr != null && (!qr.equals(lastQr) || System.currentTimeMillis() - lastTime > 5000)) {
                    lastQr = qr;
                    lastTime = System.currentTimeMillis();
                    System.out.println(" QR detected: " + qr);
                    mqttService.publishQr(qr);
                }

                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                System.err.println(" Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void captureImage() throws IOException, InterruptedException {
        long timestamp = System.currentTimeMillis();
        String uniquePath = "/tmp/qr_" + timestamp + ".jpg";
        String latestPath = "/tmp/latest_qr.jpg";

        ProcessBuilder pb = new ProcessBuilder(
            "libcamera-still",
            "-o", uniquePath,
            "--width", "1280", "--height", "960",
            "--quality", "90",
            "--nopreview", "-t", "300"
        );

        Process process = pb.inheritIO().start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("libcamera-still failed with code " + exitCode);
        }

        java.nio.file.Files.copy(
            new File(uniquePath).toPath(),
            new File(latestPath).toPath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING
        );
    }

    private String decodeQrWithZbar() {
        try {
            ProcessBuilder pb = new ProcessBuilder("zbarimg", "/tmp/latest_qr.jpg");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("QR-Code:")) {
                        return line.substring("QR-Code:".length()).trim();
                    }
                }
            }

            process.waitFor();
        } catch (Exception e) {
            System.err.println(" ZBar failed: " + e.getMessage());
        }

        return null;
    }
}
