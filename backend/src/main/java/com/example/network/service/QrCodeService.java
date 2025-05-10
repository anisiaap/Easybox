package com.example.network.service;

import com.example.network.dto.QrCodeResult;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.awt.image.BufferedImage;

import com.example.network.entity.Reservation;
import com.example.network.repository.CompartmentRepository;
import com.example.network.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class QrCodeService {

    private final ReservationRepository reservationRepository;
    private final CompartmentRepository compartmentRepository;

    public QrCodeService(ReservationRepository reservationRepository,
                         CompartmentRepository compartmentRepository) {
        this.reservationRepository = reservationRepository;
        this.compartmentRepository = compartmentRepository;
    }

    public Mono<QrCodeResult> handleQrScan(String qrContent) {
        if (!qrContent.startsWith("reservation:")) {
            return Mono.error(new IllegalArgumentException("Invalid QR format"));
        }

        Long reservationId;
        try {
            reservationId = Long.parseLong(qrContent.substring("reservation:".length()));
        } catch (NumberFormatException e) {
            return Mono.error(new IllegalArgumentException("Invalid reservation ID"));
        }

        return reservationRepository.findById(reservationId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Reservation not found")))
                .flatMap(reservation -> {
                    String currentStatus = reservation.getStatus();
                    if ("waiting_bakery_drop_off".equalsIgnoreCase(currentStatus)) {
                        reservation.setStatus("pickup_order");
                        return updateReservationAndCompartment(reservation, "busy");
                    } else if ("pickup_order".equalsIgnoreCase(currentStatus)) {
                        reservation.setStatus("completed");
                        return updateReservationAndCompartment(reservation, "free");
                    } else {
                        return Mono.error(new IllegalStateException("Reservation in unexpected state: " + currentStatus));
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

}
