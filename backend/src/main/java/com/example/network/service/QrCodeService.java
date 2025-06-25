package com.example.network.service;

import com.example.network.dto.QrCodeResult;
import com.example.network.exception.InvalidRequestException;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.awt.image.BufferedImage;

import com.example.network.model.Reservation;
import com.example.network.repository.CompartmentRepository;
import com.example.network.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;

@Service
public class QrCodeService {

    private final ReservationRepository reservationRepository;
    private final CompartmentRepository compartmentRepository;

    public QrCodeService(ReservationRepository reservationRepository,
                         CompartmentRepository compartmentRepository) {
        this.reservationRepository = reservationRepository;
        this.compartmentRepository = compartmentRepository;
    }
    private boolean isExpired(Reservation reservation) {
        return reservation.getExpiresAt() != null &&
                reservation.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC));
    }

    public Mono<QrCodeResult> handleQrScan(String qrContent) {
        if (!qrContent.startsWith("reservation:")) {
            return Mono.error(new InvalidRequestException("Invalid QR format"));
        }

        return reservationRepository.findAll()
                .flatMap(res -> {
                    try {
                        String decoded = decodeQrFromBase64Png(res.getQrCodeData());
                        if (qrContent.equals(decoded)) {
                            return Mono.just(res);
                        } else {
                            return Mono.empty();
                        }
                    } catch (Exception e) {
                        System.err.println("QR decode failed for reservation " + res.getId() + ": " + e.getMessage());
                        return Mono.empty();
                    }
                })
                .singleOrEmpty()
                .switchIfEmpty(Mono.error(new InvalidRequestException("No reservation matches this QR code")))
                .flatMap(reservation -> {
                    if (isExpired(reservation) ||
                            "cancelled".equals(reservation.getStatus()) ||
                            "expired".equals(reservation.getStatus())) {
                        return Mono.error(new InvalidRequestException("Reservation expired or cancelled"));
                    }

                    String status = reservation.getStatus();
                    if ("waiting_bakery_drop_off".equals(status) || "waiting_client_pick_up".equals(status)) {
                        return Mono.just(new QrCodeResult(
                                reservation.getCompartmentId(),
                                status
                        ));
                    }

                    return Mono.error(new InvalidRequestException("Reservation in unexpected state: " + status));
                });
    }


    public Mono<Void> handleConfirmation(Long compartmentId) {
        return reservationRepository.findByCompartmentIdAndStatus(compartmentId, "waiting_bakery_drop_off")
                .switchIfEmpty(reservationRepository.findByCompartmentIdAndStatus(compartmentId, "waiting_client_pick_up"))
                .flatMap(reservation -> {
                    String status = reservation.getStatus();
                    if ("waiting_bakery_drop_off".equals(status)) {
                        reservation.setStatus("waiting_client_pick_up");
                        return updateReservationAndCompartment(reservation, "busy").then();
                    } else if ("waiting_client_pick_up".equals(status)) {
                        reservation.setStatus("completed");
                        return updateReservationAndCompartment(reservation, "free").then();
                    } else {
                        return Mono.error(new InvalidRequestException("Invalid confirmation state"));
                    }
                });
    }

    private Mono<QrCodeResult> updateReservationAndCompartment(Reservation reservation, String newCompartmentStatus) {
        return compartmentRepository.findById(reservation.getCompartmentId())
                .flatMap(comp -> {
                    comp.setStatus(newCompartmentStatus);
                    return compartmentRepository.save(comp)
                            .then(reservationRepository.save(reservation))
                            .thenReturn(new QrCodeResult(
                                    reservation.getCompartmentId(),
                                    reservation.getStatus()
                            ));
                });
    }
    public static String generateQrCodeBase64(String text) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 250, 250);

        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(bufferedImage, "PNG", baos);
        byte[] bytes = baos.toByteArray();

        return Base64.getEncoder().encodeToString(bytes);
    }
    public static String decodeQrFromBase64Png(String base64Png) throws Exception {
        byte[] imageBytes = Base64.getDecoder().decode(base64Png);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));

        if (image == null) {
            throw new IllegalArgumentException("Failed to decode image from Base64");
        }

        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap    = new BinaryBitmap(new HybridBinarizer(source));

        Result result = new MultiFormatReader().decode(bitmap);
        return result.getText();
    }

}
