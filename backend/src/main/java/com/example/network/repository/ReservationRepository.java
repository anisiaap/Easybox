package com.example.network.repository;

import com.example.network.model.Reservation;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface ReservationRepository extends ReactiveCrudRepository<Reservation, Long> {
    Flux<Reservation> findByEasyboxId(Long easyboxId);

    Flux<Reservation>findAllByStatusAndExpiresAtBefore(String pending, LocalDateTime now);
    Flux<Reservation> findAllByBakeryIdOrderByReservationStartDesc(Long bakeryId);
     Flux<Reservation> findAllByOrderByReservationStartDesc();
    Flux<Reservation> findAllByUserIdOrderByReservationStartDesc(Long userId);
    Flux<Reservation> findByCompartmentId(Long id);
    Flux<Reservation> findAllByUserId(Long userId);
    Flux<Reservation> findAllByBakeryId(Long bakeryId);
    Mono<Reservation> findByIdAndBakeryId(Long id, Long bakeryId);
    Mono<Reservation> findByIdAndUserId(Long id, Long userId);

    Mono<Reservation> findByCompartmentIdAndStatus(Long compartmentId, String waitingBakeryDropOff);
}
