package com.example.network.service;

import com.example.network.entity.Reservation;
import com.example.network.repository.CompartmentRepository;
import com.example.network.repository.EasyboxRepository;
import com.example.network.repository.ReservationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    @Scheduled(fixedRate = 3600000) // every hour
    public void cleanupOverdueReservations() {
        LocalDateTime now = LocalDateTime.now();

        reservationRepository.findAll()
                .filter(r ->  r.getReservationEnd().isBefore(now))
                .flatMap(r ->
                        easyboxRepository.findById(r.getEasyboxId())
                                .flatMap(box ->
                                        compartmentRepository.findById(r.getCompartmentId())
                                                .flatMap(comp -> {
                                                    comp.setStatus("free");
                                                    return compartmentRepository.save(comp);
                                                })
                                                .then(Mono.fromCallable(() -> {
                                                    r.setStatus("expired");
                                                    return r;
                                                }))
                                )
                )
                .flatMap(reservationRepository::save)
                .subscribe(
                        res   -> System.out.println("🧹 Cleaned up reservation " + res.getId()),
                        error -> System.err.println("❌ Cleanup error: " + error.getMessage())
                );
    }

}
