package com.example.easyboxdevice.service;

import com.example.easyboxdevice.controller.DeviceDisplayController;
import com.example.easyboxdevice.exception.ProductNotFoundException;
import com.example.easyboxdevice.service.MqttService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;

import org.bytedeco.javacpp.Loader;                 // <-- correct Loader
import org.bytedeco.javacpp.BytePointer;           // buffer for imencode
import org.bytedeco.opencv.global.opencv_core;     // CV_VERSION
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Hashtable;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;


@Service
@RequiredArgsConstructor
public class QrScannerService {

    private final DeviceDisplayController display;
    private final MqttService mqttService;

    /** Pi camera or USB cam at /dev/video0 */
    private final VideoCapture camera = new VideoCapture(0);

    private String lastQr = "";
    private long   lastAt = 0;

    /* Load native OpenCV libs once */
    static { Loader.load(opencv_core.class); }

    @PostConstruct
    public void init() {
        if (!camera.isOpened()) {
            System.err.println("❌ Cannot open camera!");
        } else {
            System.out.println("✅ Camera initialised (OpenCV "
                    + opencv_core.CV_VERSION() + ")");
        }
    }

    /** Grab frame every second, look for QR */
    @Scheduled(fixedDelay = 1000)
    public void scanLoop() {
        Mat frame = new Mat();
        if (!camera.read(frame)) return;

        // Encode Mat -> JPEG -> byte[]
        try (BytePointer buf = new BytePointer()) {
            opencv_imgcodecs.imencode(".jpg", frame, buf);
            byte[] bytes = new byte[(int) buf.limit()];
            buf.get(bytes);

            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) return;

            BinaryBitmap bitmap = new BinaryBitmap(
                    new HybridBinarizer(new BufferedImageLuminanceSource(img)));

            Result qrRes = new MultiFormatReader().decode(
                    bitmap, new Hashtable<>());

            String qr  = qrRes.getText();
            long   now = System.currentTimeMillis();

            if (!qr.equals(lastQr) || now - lastAt > 5_000) {
                System.out.println("✅ QR Found: " + qr);
                mqttService.publishQr(qr);
                display.showLoading();
                lastQr = qr;
                lastAt = now;
            }
        } catch (NotFoundException ignore) {
            // No QR this frame
        } catch (ProductNotFoundException ignore) {
            // Domain-specific
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @PreDestroy
    public void shutdown() {
        if (camera.isOpened()) camera.release();
    }
}
