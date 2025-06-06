package com.example.network.service;

import com.example.network.repository.CompartmentRepository;
import com.example.network.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
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
                .findAllByStatusAndExpiresAtBefore("pending", now)
                .flatMap(reservation ->
                                compartmentRepository.findById(reservation.getCompartmentId())
                                        .then(reservationRepository.delete(reservation)),
                        10 // concurrency level
                )

                .collectList() // Collect all results into a list
                .doOnSuccess(deleted -> System.out.println("Cleanup completed. Deleted {} reservations." + deleted.size()))
                .doOnError(error -> System.out.println("Error during cleanup task: " + error))
                .subscribe();
    }
}
