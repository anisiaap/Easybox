package com.example.network.service;

import com.example.network.repository.CompartmentRepository;
import com.example.network.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.example.network.entity.Reservation;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReservationCleanupTask {

    private final ReservationRepository reservationRepository;
    private final CompartmentRepository compartmentRepository;

    /** runs every 60 s */
    @Scheduled(fixedDelay = 60_000)
    public void releaseExpired() {

        LocalDateTime now = LocalDateTime.now();

        reservationRepository
                .findAllByStatusAndExpiresAtBefore("pending", now)   // Flux<Reservation>
                .flatMap(reservation ->
                        compartmentRepository
                                .findById(reservation.getCompartmentId())    // ← use getCompartmentId()
                                .then(reservationRepository.delete(reservation)) // delete the hold
                )
                .subscribe(); // fire-and-forget
    }
}

