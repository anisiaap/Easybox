package com.example.network.repository;

import com.example.network.entity.Reservation;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends ReactiveCrudRepository<Reservation, Long> {
    // e.g. find all reservations for a given easybox
    Flux<Reservation> findByEasyboxId(Long easyboxId);

    Flux<Reservation>findAllByStatusAndExpiresAtBefore(String pending, LocalDateTime now);

    Flux<Reservation> findByCompartmentId(Long id);
    Flux<Reservation> findAllByUserId(Long userId);
    Flux<Reservation> findAllByBakeryId(Long bakeryId);
    Mono<Reservation> findByIdAndBakeryId(Long id, Long bakeryId);
    Mono<Reservation> findByIdAndUserId(Long id, Long userId);

}
