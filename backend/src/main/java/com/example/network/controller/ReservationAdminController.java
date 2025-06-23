package com.example.network.controller;

import com.example.network.dto.ReservationUpdateRequest;
import com.example.network.model.Reservation;
import com.example.network.model.User;
import com.example.network.exception.NotFoundException;
import com.example.network.repository.ReservationRepository;
import com.example.network.repository.BakeryRepository;
import com.example.network.repository.EasyboxRepository;
import com.example.network.model.Bakery;
import com.example.network.model.Easybox;
import com.example.network.dto.ReservationDto;
import com.example.network.repository.UserRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/reservations")
public class ReservationAdminController {

    private final ReservationRepository reservationRepository;
    private final BakeryRepository bakeryRepository;
    private final EasyboxRepository easyboxRepository;
    private final UserRepository userRepository;
    public ReservationAdminController(ReservationRepository reservationRepository,
                                      BakeryRepository bakeryRepository,
                                      EasyboxRepository easyboxRepository, UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.bakeryRepository = bakeryRepository;
        this.easyboxRepository = easyboxRepository;
        this.userRepository = userRepository;
    }

    // GET all reservations
    @GetMapping
    public Flux<ReservationDto> getAllReservations(@RequestParam(required = false) Long bakeryId,
                                                   @RequestParam(required = false) Long userId,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                                                   @RequestParam(required = false, defaultValue = "0") int page,
                                                   @RequestParam(required = false, defaultValue = "10") int size)
    {
        Flux<Reservation> base = (bakeryId != null)
                ? reservationRepository.findAllByBakeryIdOrderByReservationStartDesc(bakeryId)
                : reservationRepository.findAllByOrderByReservationStartDesc();

        return base
                .filter(r -> userId == null || userId.equals(r.getUserId()))
                .filter(r -> deliveryDate == null ||
                        r.getDeliveryTime() != null &&
                                r.getDeliveryTime().toLocalDate().isEqual(deliveryDate))
                .skip((long) page * size)
                .take(size)
                .flatMap(this::toDto);
    }
    @GetMapping("/count")
    public Mono<Long> countReservations(
            @RequestParam(required = false) Long bakeryId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate
    ) {
        return reservationRepository.findAll()
                .filter(r -> bakeryId == null || bakeryId.equals(r.getBakeryId()))
                .filter(r -> userId == null || userId.equals(r.getUserId()))
                .filter(r -> deliveryDate == null ||
                        (r.getDeliveryTime() != null &&
                                r.getDeliveryTime().toLocalDate().isEqual(deliveryDate)))
                .count();
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
                .switchIfEmpty(Mono.error(new NotFoundException("Reservation not found")))
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

        Mono<Bakery> bakeryMono = Mono.justOrEmpty(reservation.getBakeryId())
                .flatMap(bakeryRepository::findById)
                .defaultIfEmpty(null);   // ← guard against NULL

        Mono<Easybox> easyboxMono = Mono.justOrEmpty(reservation.getEasyboxId())
                .flatMap(easyboxRepository::findById)
                .defaultIfEmpty(null);   // ← keeps existing behaviour

        Mono<User> userMono = Mono.justOrEmpty(reservation.getUserId())
                .flatMap(userRepository::findById)
                .defaultIfEmpty(null);   // ← guard against NULL

        return Mono.zip(bakeryMono, easyboxMono, userMono)
                .map(tuple -> new ReservationDto(
                        reservation.getId(),
                        tuple.getT3() != null ? tuple.getT3().getPhoneNumber() : "Unknown",
                        tuple.getT1() != null ? tuple.getT1().getName()        : "Unknown Bakery",
                        tuple.getT2() != null ? tuple.getT2().getAddress()     : "Unknown Easybox",
                        reservation.getStatus(),
                        reservation.getCompartmentId(),
                        reservation.getReservationStart(),
                        reservation.getReservationEnd()
                ));
    }


}
