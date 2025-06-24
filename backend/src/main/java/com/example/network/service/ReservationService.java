package com.example.network.service;

import com.example.network.dto.*;
import com.example.network.model.*;
import com.example.network.exception.ConfigurationException;
import com.example.network.exception.ConflictException;
import com.example.network.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final EasyboxRepository      easyboxRepository;
    private final CompartmentRepository  compartmentRepository;
    private final GeocodingService       geocodingService;
    private final UserService userService;
    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
    public ReservationService(
            ReservationRepository reservationRepository,
            EasyboxRepository easyboxRepository,
            CompartmentRepository compartmentRepository,
            GeocodingService geocodingService,
            UserService userService
    ) {
        this.reservationRepository = reservationRepository;
        this.easyboxRepository     = easyboxRepository;
        this.compartmentRepository = compartmentRepository;
        this.geocodingService      = geocodingService;
        this.userService = userService;
    }
    private static final SecureRandom random = new SecureRandom();

    private String generateUniqueSuffix() {
        byte[] bytes = new byte[5]; // 40-bit
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); // URL-safe
    }
    private Flux<Compartment> filterCompartments(Easybox box, Integer minTemp, Integer totalDim, LocalDateTime start, LocalDateTime end) {
        LocalDateTime now = LocalDateTime.now();

        return compartmentRepository.findByEasyboxId(box.getId())
                .filter(c -> c.getCondition() != null && ("good".equalsIgnoreCase(c.getCondition()) ||
                        "clean".equalsIgnoreCase(c.getCondition())))
                .filter(c -> minTemp == null || c.getTemperature() == minTemp)
                .filter(c -> totalDim == null || c.getSize() >= totalDim)
                .concatMap(c ->
                        reservationRepository.findByCompartmentId(c.getId())
                                .filter(r -> {
                                    boolean confirmed = "confirmed".equalsIgnoreCase(r.getStatus());
                                    boolean pending = "pending".equalsIgnoreCase(r.getStatus())
                                            && r.getExpiresAt() != null
                                            && r.getExpiresAt().isAfter(now);
                                    return confirmed || pending;
                                })
                                .filter(r -> r.getReservationStart().isBefore(end) &&
                                        r.getReservationEnd().isAfter(start))
                                .hasElements()
                                .map(hasOverlap -> !hasOverlap)
                                .filter(canUse -> canUse)
                                .map(canUse -> c)
                );
    }
    @Transactional
    public Mono<Reservation> holdReservation(CreateReservationRequest req, Authentication auth) {
        Long bakeryId = extractUserIdFromJwt(auth);
        return userService.getOrCreate(req.getPhone())
                .flatMap(user -> {
                    LocalDateTime delivery = LocalDateTime.parse(req.getDeliveryTime());
                    LocalDateTime start = delivery.minusHours(3);
                    LocalDateTime end = delivery.plusHours(27);

                    return easyboxRepository.findById(req.getEasyboxId())
                            .switchIfEmpty(Mono.error(new ConflictException("Easybox not found")))
                            .flatMap(box ->
                                    findAndLockAvailableCompartment(
                                            box,
                                            req.getMinTemperature(),
                                            req.getTotalDimension(),
                                            start,
                                            end
                                    )
                                            .flatMap(compId -> {
                                                Reservation r = new Reservation();
                                                r.setDeliveryTime(delivery);
                                                r.setReservationStart(start);
                                                r.setReservationEnd(end);
                                                r.setStatus("pending");              // 15-min soft lock
                                                r.setExpiresAt(LocalDateTime.now().plusMinutes(15));
                                                r.setEasyboxId(req.getEasyboxId());
                                                r.setCompartmentId(compId);
                                                r.setUserId(user.getId());
                                                r.setBakeryId(bakeryId);

                                                return reservationRepository.save(r)
                                                        .onErrorResume(DuplicateKeyException.class, e -> {
                                                            return Mono.error(new ConflictException("Compartment reservation conflict, please try again."));
                                                        })
                                                        .onErrorResume(e -> {
                                                            if (e.getMessage() != null && e.getMessage().contains("compartment_no_overlap")) {
                                                                return Mono.error(new ConflictException("Compartment already reserved, please try again."));
                                                            }
                                                            return Mono.error(e);
                                                        });
                                            })
                            );
                });
    }

    @Transactional
    public Mono<Reservation> confirmReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .switchIfEmpty(Mono.error(new ConflictException("Hold not found")))
                .flatMap(r -> {
                    if (!"pending".equals(r.getStatus())) {
                        return Mono.just(r);
                    }
                    return easyboxRepository.findById(r.getEasyboxId())
                            .flatMap(box -> {
                                r.setStatus("confirmed");
                                r.setExpiresAt(null);
                                return reservationRepository.save(r)
                                        .flatMap(saved -> {
                                            try {
                                                String uniqueSuffix = generateUniqueSuffix();

                                                String qrContent = "reservation:" + saved.getId() + ":" + uniqueSuffix;

                                                String qrBase64 = QrCodeService.generateQrCodeBase64(qrContent);

                                                saved.setQrCodeData(qrBase64);

                                                return reservationRepository.save(saved);
                                            } catch (Exception e) {
                                                return Mono.error(new ConfigurationException("Failed to generate QR code"));
                                            }
                                        });

                            });
                });
    }
    private Long extractUserIdFromJwt(Authentication auth) {
        Jwt jwt = (Jwt) auth.getPrincipal();
        return jwt.getClaim("userId");
    }
    public Mono<RecommendedBoxesResponse> findAvailableBoxes(
            String address,
            LocalDateTime start,
            LocalDateTime end,
            Integer minTemp,
            Integer totalDim
    ) {
        return easyboxRepository.findByAddressIgnoreCase(address)
                .filter(box -> "active".equalsIgnoreCase(box.getStatus()))
                .flatMap(exactBox ->
                        boxIfAvailable(exactBox, start, end, minTemp, totalDim, null, null)
                                .defaultIfEmpty(null)
                                .flatMap(exactDto -> {
                                    if (exactDto != null) {
                                        return collectOtherBoxes(address, exactDto, start, end, minTemp, totalDim);
                                    }
                                    return fallbackByDistance(address, start, end, minTemp, totalDim);
                                })
                )
                .switchIfEmpty(fallbackByDistance(address, start, end, minTemp, totalDim));
    }

    private Mono<Long> findAndLockAvailableCompartment(Easybox box,
                                                       Integer minTemp,
                                                       Integer totalDim,
                                                       LocalDateTime start,
                                                       LocalDateTime end) {

        LocalDateTime now = LocalDateTime.now();
        return filterCompartments(box, minTemp, totalDim, start, end)
                .map(Compartment::getId)
                .next()   // first compartment that we successfully locked
                .switchIfEmpty(Mono.error(new ConflictException("No compartments available in that window")));
    }
    private Mono<EasyboxDto> boxIfAvailable(Easybox box,
                                            LocalDateTime st,
                                            LocalDateTime ed,
                                            Integer minTemp,
                                            Integer totalDim,
                                            Double userLat,
                                            Double userLon) {

      return filterCompartments(box, minTemp, totalDim, st, ed)
                .hasElements()
                .filter(Boolean::booleanValue)
                .map(ok -> {
                    EasyboxDto dto = new EasyboxDto(
                            box.getId(),
                            box.getAddress(),
                            box.getStatus(),
                            box.getLatitude(),
                            box.getLongitude(),
                            1000
                    );
                    dto.setAvailable(true);
                    if (userLat != null && userLon != null) {
                        dto.setDistance(
                                geocodingService.distance(box.getLatitude(), box.getLongitude(),
                                        userLat,         userLon));
                    }
                    return dto;
                });
    }


    private Mono<RecommendedBoxesResponse> collectOtherBoxes(String address, EasyboxDto exact,
                                                             LocalDateTime st, LocalDateTime ed,
                                                             Integer minTemp, Integer totalDim) {

        return geocodingService.geocodeAddress(address)
                .flatMap(coords ->
                        easyboxRepository.findAll()
                                .filter(e -> !e.getId().equals(exact.getId()))
                                .collectList()
                                .flatMapMany(list -> Flux.fromIterable(list)
                                        .sort(Comparator.comparingDouble(
                                                e -> geocodingService.distance(e.getLatitude(), e.getLongitude(),
                                                        coords[0],        coords[1])))
                                        .concatMap(e -> boxIfAvailable(e, st, ed, minTemp, totalDim,
                                                coords[0], coords[1])))
                                .collectList()
                                .map(others -> new RecommendedBoxesResponse(exact, others)));
    }

    private Mono<RecommendedBoxesResponse> fallbackByDistance(String address,
                                                              LocalDateTime st, LocalDateTime ed,
                                                              Integer minTemp, Integer totalDim) {

        return geocodingService.geocodeAddress(address)
                .flatMap(coords ->
                        easyboxRepository.findAll()
                                .filter(b -> "active".equalsIgnoreCase(b.getStatus()))
                                .sort(Comparator.comparingDouble(
                                        e -> geocodingService.distance(e.getLatitude(), e.getLongitude(),
                                                coords[0],        coords[1])))
                                .concatMap(e -> boxIfAvailable(e, st, ed, minTemp, totalDim,
                                        coords[0], coords[1]))
                                .collectList()
                                .map(list -> {
                                    if (list.isEmpty()) {
                                        return new RecommendedBoxesResponse(null, List.of());
                                    }
                                    EasyboxDto rec   = list.get(0);
                                    List<EasyboxDto> others = (list.size() > 1)
                                            ? list.subList(1, list.size())
                                            : List.of();
                                    return new RecommendedBoxesResponse(rec, others);
                                }));
    }
}
