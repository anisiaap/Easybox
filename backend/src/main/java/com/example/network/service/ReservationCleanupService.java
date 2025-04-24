package com.example.network.service;

import com.example.network.entity.Reservation;
import com.example.network.repository.EasyboxRepository;
import com.example.network.repository.ReservationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Service
public class ReservationCleanupService {

    private final ReservationRepository reservationRepository;
    private final EasyboxRepository easyboxRepository;
    private final DeviceService deviceService;

    public ReservationCleanupService(ReservationRepository reservationRepository,
                                     EasyboxRepository easyboxRepository,
                                     DeviceService deviceService) {
        this.reservationRepository = reservationRepository;
        this.easyboxRepository = easyboxRepository;
        this.deviceService = deviceService;
    }

    /**
     * This scheduled method runs every hour.
     * It finds all confirmed reservations that have ended,
     * frees the corresponding compartment on the device,
     * and then updates the reservation status to "expired".
     */
    @Scheduled(fixedRate = 3600000) // every hour
    public void cleanupOverdueReservations() {
        LocalDateTime now = LocalDateTime.now();
        reservationRepository.findAll()
                .filter(reservation -> "confirmed".equalsIgnoreCase(reservation.getStatus())
                        && reservation.getReservationEnd().isBefore(now))
                .flatMap(reservation ->
                        // Look up the Easybox to get the device URL.
                        easyboxRepository.findById(reservation.getEasyboxId())
                                .flatMap(easybox ->
                                        // Call the device to free the compartment.
                                        deviceService.freeCompartmentOnDevice(easybox.getDeviceUrl(), reservation.getCompartmentId())
                                                .thenReturn(reservation)
                                )
                )
                .flatMap(reservation -> {
                    reservation.setStatus("expired");
                    return reservationRepository.save(reservation);
                })
                .subscribe(
                        res -> System.out.println("Cleaned up reservation " + res.getId()),
                        error -> System.err.println("Cleanup error: " + error.getMessage())
                );
    }
}
