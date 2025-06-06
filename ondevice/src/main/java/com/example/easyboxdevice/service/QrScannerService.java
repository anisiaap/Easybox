package com.example.easyboxdevice.service;

import com.example.easyboxdevice.controller.DeviceDisplayController;
import com.example.easyboxdevice.exception.ProductNotFoundException;
import com.example.easyboxdevice.service.MqttService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.core.MatOfByte;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Hashtable;
import jakarta.annotation.PreDestroy;


@Service
@RequiredArgsConstructor
public class QrScannerService {
    private final DeviceDisplayController display;
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME); // Load native OpenCV
    }

    private final MqttService mqttService;
    private final VideoCapture camera = new VideoCapture(0); // Pi Camera
    private String lastQr = "";
    private long   lastAt = 0;

    @PostConstruct
    public void init() {
        if (!camera.isOpened()) {
            System.err.println("❌ Cannot open camera!");
        } else {
            System.out.println("✅ Camera initialized");
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void scanLoop() {
        Mat frame = new Mat();
        if (!camera.read(frame)) return;

        try {
            MatOfByte buffer = new MatOfByte();
            Imgcodecs.imencode(".jpg", frame, buffer);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(buffer.toArray()));

            LuminanceSource source = new BufferedImageLuminanceSource(img);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(bitmap, new Hashtable<>());

            String qr = result.getText();
            long   now = System.currentTimeMillis();

            if (!qr.equals(lastQr) || now - lastAt > 5_000) {   // 5-sec debounce
                System.out.println("✅ QR Found: " + qr);
                mqttService.publishQr(qr);
                display.showLoading();
                lastQr = qr;
                lastAt = now;
           }
        } catch (ProductNotFoundException ignore) {
            // no QR in frame
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    @PreDestroy
    public void shutdown() {
        if (camera.isOpened()) camera.release();
    }
}
