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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Map;

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
    public Flux<ReservationDto> getAllReservations(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String bakeryName,
            @RequestParam(required = false) String userPhone,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Flux<Reservation> base = reservationRepository.findAllByOrderByReservationStartDesc();

        // resolve IDs reactively
        Mono<Long> bakeryIdMono = (bakeryName != null && !bakeryName.isEmpty())
                ? bakeryRepository.findByName(bakeryName).map(Bakery::getId)
                : Mono.empty();

        Mono<Long> userIdMono = (userPhone != null && !userPhone.isEmpty())
                ? userRepository.findByPhoneNumber(userPhone).map(User::getId)
                : Mono.empty();

        Mono<Long> orderIdMono = (orderId != null && !orderId.isEmpty())
                ? Mono.just(Long.parseLong(orderId))
                : Mono.empty();


        return Mono.zip(bakeryIdMono.defaultIfEmpty(null), userIdMono.defaultIfEmpty(null), orderIdMono.defaultIfEmpty(null))


                .flatMapMany(tuple -> {
                    Long bakeryId = tuple.getT1();
                    Long userId = tuple.getT2();
                    Long orderLong = tuple.getT3();

                    return base
                            .filter(r -> bakeryId == null || bakeryId.equals(r.getBakeryId()))
                            .filter(r -> userId == null || userId.equals(r.getUserId()))
                            .filter(r -> orderLong == null || orderLong.equals(r.getId()))
                            .filter(r -> deliveryDate == null ||
                                    (r.getDeliveryTime() != null &&
                                            r.getDeliveryTime().toLocalDate().isEqual(deliveryDate)))
                            .skip((long) page * size)
                            .take(size)
                            .flatMap(this::toDto);
                });
    }

    @GetMapping("/count")
    public Mono<Long> countReservations(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String bakeryName,
            @RequestParam(required = false) String userPhone,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate
    ) {
        Flux<Reservation> all = reservationRepository.findAll();

        Mono<Long> bakeryIdMono = (bakeryName != null && !bakeryName.isEmpty())
                ? bakeryRepository.findByName(bakeryName).map(Bakery::getId)
                : Mono.just(null);

        Mono<Long> userIdMono = (userPhone != null && !userPhone.isEmpty())
                ? userRepository.findByPhoneNumber(userPhone).map(User::getId)
                : Mono.just(null);

        Mono<Long> orderIdMono = (orderId != null && !orderId.isEmpty())
                ? Mono.just(Long.parseLong(orderId))
                : Mono.just(null);

        return Mono.zip(bakeryIdMono, userIdMono, orderIdMono)
                .flatMap(tuple -> {
                    Long bakeryId = tuple.getT1();
                    Long userId = tuple.getT2();
                    Long orderLong = tuple.getT3();

                    return all
                            .filter(r -> bakeryId == null || bakeryId.equals(r.getBakeryId()))
                            .filter(r -> userId == null || userId.equals(r.getUserId()))
                            .filter(r -> orderLong == null || orderLong.equals(r.getId()))
                            .filter(r -> deliveryDate == null ||
                                    (r.getDeliveryTime() != null &&
                                            r.getDeliveryTime().toLocalDate().isEqual(deliveryDate)))
                            .count();
                });
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
                    if (!reservation.getVersion().equals(update.getVersion())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Reservation was modified by someone else."));
                    }

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
    public Mono<Void> deleteReservation(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long version = body.get("version") instanceof Number ? ((Number) body.get("version")).longValue() : null;
        if (version == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Version required for delete"));
        }

        return reservationRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Reservation not found")))
                .flatMap(reservation -> {
                    if (!reservation.getVersion().equals(version)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Reservation was modified by someone else."));
                    }
                    return reservationRepository.delete(reservation);
                });
    }


    private Mono<ReservationDto> toDto(Reservation reservation) {
        Mono<Bakery> bakeryMono  = bakeryRepository.findById(reservation.getBakeryId());
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
}
