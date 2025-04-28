package com.example.network.controller;

import com.example.network.dto.ReservationUpdateRequest;
import com.example.network.entity.Reservation;
import com.example.network.repository.ReservationRepository;
import com.example.network.repository.BakeryRepository;
import com.example.network.repository.EasyboxRepository;
import com.example.network.entity.Bakery;
import com.example.network.entity.Easybox;
import com.example.network.dto.ReservationDto;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/reservations")
public class ReservationAdminController {

    private final ReservationRepository reservationRepository;
    private final BakeryRepository bakeryRepository;
    private final EasyboxRepository easyboxRepository;

    public ReservationAdminController(ReservationRepository reservationRepository,
                                      BakeryRepository bakeryRepository,
                                      EasyboxRepository easyboxRepository) {
        this.reservationRepository = reservationRepository;
        this.bakeryRepository = bakeryRepository;
        this.easyboxRepository = easyboxRepository;
    }

    // GET all reservations
    @GetMapping
    public Flux<ReservationDto> getAllReservations() {
        return reservationRepository.findAll()
                .flatMap(this::toDto);
    }

    // GET one reservation
    @GetMapping("/{id}")
    public Mono<ReservationDto> getReservation(@PathVariable Long id) {
        return reservationRepository.findById(id)
                .flatMap(this::toDto);
    }

    // UPDATE reservation
    @PutMapping("/{id}")
    public Mono<Reservation> updateReservation(@PathVariable Long id, @RequestBody ReservationUpdateRequest update) {
        return reservationRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Reservation not found")))
                .flatMap(reservation -> {
                    if (update.getStatus() != null) {
                        reservation.setStatus(update.getStatus());
                    }
                    if (update.getEasyboxId() != null) {
                        reservation.setEasyboxId(update.getEasyboxId());
                    }
                    return reservationRepository.save(reservation);
                });
    }

    // DELETE reservation
    @DeleteMapping("/{id}")
    public Mono<Void> deleteReservation(@PathVariable Long id) {
        return reservationRepository.deleteById(id);
    }

    private Mono<ReservationDto> toDto(Reservation reservation) {
        Mono<Bakery> bakeryMono = bakeryRepository.findById(reservation.getBakeryId());
        Mono<Easybox> easyboxMono = easyboxRepository.findById(reservation.getEasyboxId());

        return Mono.zip(bakeryMono, easyboxMono)
                .map(tuple -> {
                    Bakery bakery = tuple.getT1();
                    Easybox easybox = tuple.getT2();
                    return new ReservationDto(
                            reservation.getId(),
                            reservation.getClient(),
                            bakery != null ? bakery.getName() : "Unknown Bakery",
                            easybox != null ? easybox.getAddress() : "Unknown Easybox",
                            reservation.getStatus()
                    );
                });
    }
}
