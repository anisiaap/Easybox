package com.example.network.service;

import com.example.network.model.Reservation;
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

                    Mono<Reservation> updated = Mono.just(reservation);

                    if (startingSoon) {
                        updated = easyboxRepository.findById(reservation.getEasyboxId())
                                .flatMap(easybox -> {
                                    if (!"active".equals(easybox.getStatus())) {
                                        reservation.setStatus("canceled");
                                        return Mono.just(reservation);
                                    }
                                    return compartmentRepository.findById(reservation.getCompartmentId())
                                            .flatMap(compartment -> {
                                                String cond = compartment.getStatus();
                                                if ("dirty".equalsIgnoreCase(cond) || "broken".equalsIgnoreCase(cond)) {
                                                    reservation.setStatus("waiting_cleaning");
                                                    return Mono.just(reservation);
                                                }
                                                return Mono.just(reservation);
                                            });
                                });
                    }

                    return updated.flatMap(r -> {
                        if (r.getReservationEnd() != null && r.getReservationEnd().isBefore(now) && "waiting_bakery_drop_off".equalsIgnoreCase(r.getStatus())) {
                            r.setStatus("expired");
                            return Mono.just(r);
                        }

                        if ("confirmed".equalsIgnoreCase(r.getStatus()) && r.getReservationStart().isBefore(now)) {
                            r.setStatus("waiting_bakery_drop_off");
                            return Mono.just(r);
                        }

                        if ("waiting_client_pick_up".equalsIgnoreCase(r.getStatus()) &&
                                r.getReservationEnd().minusHours(3).isBefore(now)) {
                            r.setStatus("waiting_cleaning");
                            return Mono.just(r);
                        }

                        return Mono.empty();
                    });

                })
                .flatMap(reservationRepository::save)
                .subscribe(
                        res -> System.out.println("Updated reservation " + res.getId() + " → " + res.getStatus()),
                        error -> System.err.println("Error during reservation update: " + error.getMessage())
                );
    }


}
