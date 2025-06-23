package com.example.network.service;

import com.example.network.repository.CompartmentRepository;
import com.example.network.repository.EasyboxRepository;
import com.example.network.repository.ReservationRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.LocalDateTime;

@Service
public class ReservationCleanupService {

    private final ReservationRepository reservationRepository;
    private final EasyboxRepository easyboxRepository;
    private final CompartmentRepository compartmentRepository;

    public ReservationCleanupService(ReservationRepository reservationRepository,
                                     EasyboxRepository     easyboxRepository,
                                     CompartmentRepository compartmentRepository) {
        this.reservationRepository = reservationRepository;
        this.easyboxRepository     = easyboxRepository;
        this.compartmentRepository = compartmentRepository;
    }

    @Scheduled(fixedRate = 600_000) // every 10 minutes
    public void cleanupAndUpdateReservations() {
        LocalDateTime now = LocalDateTime.now();

        reservationRepository.findAll()
                .flatMap(reservation -> {
                    LocalDateTime start = reservation.getReservationStart();
                    boolean startingSoon = start != null && !start.isAfter(now.plusHours(1)); // starts in <= 1h

                    // 💥 New condition: cancel if Easybox inactive or compartment dirty/broken, and reservation starts soon
                    if (startingSoon) {
                        return easyboxRepository.findById(reservation.getEasyboxId())
                                .flatMap(easybox -> {
                                    if (!"active".equals(easybox.getStatus())) {
                                        if("waiting_client_pick_up".equals(reservation.getStatus() )&& "waiting_cleaning".equals(reservation.getStatus())){
                                            reservation.setStatus("waiting_cleaning");
                                            return Mono.just(reservation);
                                        }
                                        reservation.setStatus("canceled");
                                        return Mono.just(reservation);
                                    }
                                    return compartmentRepository.findById(reservation.getCompartmentId())
                                            .flatMap(compartment -> {
                                                String status = compartment.getStatus();
                                                if ("dirty".equalsIgnoreCase(status) || "broken".equalsIgnoreCase(status)) {
                                                    reservation.setStatus("waiting_cleaning");
                                                    return Mono.just(reservation);
                                                }
                                                return Mono.empty(); // no update needed
                                            });
                                });
                    }

                    // Case 1: Expired reservation
                    if (reservation.getReservationEnd() != null && reservation.getReservationEnd().isBefore(now) && !"waiting_bakery_drop_off".equalsIgnoreCase(reservation.getStatus()))  {
                        return easyboxRepository.findById(reservation.getEasyboxId())
                                .flatMap(box -> compartmentRepository.findById(reservation.getCompartmentId())
                                        .then(Mono.fromCallable(() -> {
                                            reservation.setStatus("expired");
                                            return reservation;
                                        }))
                                );
                    }

                    // Case 2: After reservationStart → waiting_bakery_drop_off
                    if ("confirmed".equalsIgnoreCase(reservation.getStatus()) &&
                            reservation.getReservationStart() != null &&
                            reservation.getReservationStart().isBefore(now)) {
                        reservation.setStatus("waiting_bakery_drop_off");
                        return Mono.just(reservation);
                    }

                    // Case 3: Within 3 hours before reservationEnd → waiting_cleaning
                    if ("waiting_client_pick_up".equalsIgnoreCase(reservation.getStatus()) &&
                            reservation.getReservationEnd() != null &&
                            reservation.getReservationEnd().minusHours(3).isBefore(now)) {
                        reservation.setStatus("waiting_cleaning");
                        return Mono.just(reservation);
                    }
                    // No changes
                    return Mono.empty();
                })
                .flatMap(reservationRepository::save)
                .subscribe(
                        res -> System.out.println("🔄 Updated reservation " + res.getId() + " → " + res.getStatus()),
                        error -> System.err.println("❌ Error during reservation update: " + error.getMessage())
                );
    }


}
