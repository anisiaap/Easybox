package com.example.network.controller;

import com.example.network.dto.RecommendedBoxesResponse;
import com.example.network.dto.ReservationUpdateRequest;
import com.example.network.entity.*;
import com.example.network.exception.NotFoundException;
import com.example.network.repository.*;
import com.example.network.dto.ReservationDto;
import com.example.network.service.CompartmentSyncService;
import com.example.network.service.ReservationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/reservations")
public class ReservationAdminController {

    private final ReservationRepository reservationRepository;
    private final BakeryRepository bakeryRepository;
    private final EasyboxRepository easyboxRepository;
    private final UserRepository userRepository;
    private final ReservationService reservationService;
    private final CompartmentRepository compartmentRepository;

    public ReservationAdminController(ReservationRepository reservationRepository,
                                      BakeryRepository bakeryRepository,
                                      EasyboxRepository easyboxRepository, UserRepository userRepository, ReservationService reservationService, CompartmentRepository compartmentRepository) {
        this.reservationRepository = reservationRepository;
        this.bakeryRepository = bakeryRepository;
        this.easyboxRepository = easyboxRepository;
        this.userRepository = userRepository;
        this.reservationService = reservationService;
        this.compartmentRepository = compartmentRepository;
    }

    // GET all reservations
    @GetMapping
    public Flux<ReservationDto> getAllReservations(@RequestParam(required = false) Long bakeryId,
                                                   @RequestParam(required = false) Long userId,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
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
    public Mono<Reservation> updateReservation(@PathVariable Long id,
                                               @RequestBody ReservationUpdateRequest update) {

        return reservationRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Reservation not found")))
                .flatMap(existing -> {

                    // ---------- (A) easyboxId changed ----------
                    if (update.getEasyboxId() != null &&
                            !update.getEasyboxId().equals(existing.getEasyboxId())) {

                        return reservationService.reassignEasybox(id, update.getEasyboxId())
                                .flatMap(updated -> {
                                    // status may ALSO change in the same request
                                    if (update.getStatus() != null &&
                                            !update.getStatus().equals(updated.getStatus())) {
                                        updated.setStatus(update.getStatus());
                                        return reservationRepository.save(updated);
                                    }
                                    return Mono.just(updated);
                                });
                    }

                    // ---------- (B) only status changed ----------
                    if (update.getStatus() != null &&
                            !update.getStatus().equals(existing.getStatus())) {
                        existing.setStatus(update.getStatus());
                        return reservationRepository.save(existing);
                    }

                    return Mono.just(existing);       // nothing changed
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
        Mono<User> userMono = userRepository.findById(reservation.getUserId());

        return Mono.zip(bakeryMono, easyboxMono, userMono)
                .map(tuple -> {
                    Bakery bakery = tuple.getT1();
                    Easybox easybox = tuple.getT2();
                    User user = tuple.getT3();

                    return new ReservationDto(
                            reservation.getId(),
                            user != null ? user.getPhoneNumber() : "Unknown",
                            bakery != null ? bakery.getName() : "Unknown Bakery",
                            easybox != null ? easybox.getAddress() : "Unknown Easybox",
                            reservation.getStatus(),
                            reservation.getCompartmentId(),
                            reservation.getReservationStart(),
                            reservation.getReservationEnd()
                    );
                });
    }

//    @PutMapping("/{id}/reassign-easybox")
//    public Mono<Reservation> reassignEasybox(@PathVariable Long id, @RequestParam Long newEasyboxId) {
//        return reservationService.reassignEasybox(id, newEasyboxId);
//    }

    @GetMapping("/{id}/available-easyboxes")
    public Mono<RecommendedBoxesResponse> getAvailableBoxesForReservation(@PathVariable Long id) {
        return reservationRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Reservation not found")))
                .flatMap(reservation -> {
                    Long easyboxId = reservation.getEasyboxId();
                    Long compartmentId = reservation.getCompartmentId();
                    if (easyboxId == null || compartmentId == null) {
                        return Mono.error(new NotFoundException("Reservation is missing easybox or compartment"));
                    }

                    return Mono.zip(
                            easyboxRepository.findById(easyboxId)
                                    .switchIfEmpty(Mono.error(new NotFoundException("Easybox not found"))),
                            compartmentRepository.findById(compartmentId)
                                    .switchIfEmpty(Mono.error(new NotFoundException("Compartment not found")))
                    ).flatMap((Tuple2<Easybox, Compartment> tuple) -> {
                        Easybox easybox = tuple.getT1();
                        Compartment comp = tuple.getT2();

                        return reservationService.findAvailableBoxes(
                                easybox.getAddress(),
                                reservation.getReservationStart(),
                                reservation.getReservationEnd(),
                                comp.getTemperature(),
                                comp.getSize()
                        );
                    });
                });
    }
}
