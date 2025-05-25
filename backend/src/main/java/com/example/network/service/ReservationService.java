package com.example.network.service;

import com.example.network.dto.*;
import com.example.network.entity.*;
import com.example.network.exception.ConfigurationException;
import com.example.network.exception.ConflictException;
import com.example.network.repository.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final EasyboxRepository      easyboxRepository;
    private final CompartmentRepository  compartmentRepository;
    private final GeocodingService       geocodingService;
    private final UserService userService;
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

    /* -------------------------------------------------------------------- */
    /* -----------------------  H O L D   P H A S E  ---------------------- */
    /* -------------------------------------------------------------------- */

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

    /* -------------------------------------------------------------------- */
    /* --------------------  C O N F I R M   P H A S E  -------------------- */
    /* -------------------------------------------------------------------- */

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
                                                String qrContent = "reservation:" + saved.getId();  // you can customize QR content
                                                String qrBase64 = QrCodeService.generateQrCodeBase64(qrContent);

                                                saved.setQrCodeData(qrBase64);

                                                return reservationRepository.save(saved);  // update reservation with QR
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

    /* -------------------------------------------------------------------- */
    /* --------------------  I N T E R N A L   H E L P E R S --------------- */
    /* -------------------------------------------------------------------- */

    /**
     * Atomically find **one** compartment that:
     *   • is free & good/clean
     *   • meets temp / size
     *   • has **no confirmed overlap** with [start,end]
     * then mark it busy and return its ID.
     */
    private Mono<Long> findAndLockAvailableCompartment(Easybox box,
                                                       Integer minTemp,
                                                       Integer totalDim,
                                                       LocalDateTime start,
                                                       LocalDateTime end) {

        return compartmentRepository.findByEasyboxId(box.getId())
                .filter(c -> c.getCondition() != null &&
                        ( "good".equalsIgnoreCase(c.getCondition())))
                .filter(c -> minTemp  == null || c.getTemperature() == minTemp)
                .filter(c -> totalDim == null || c.getSize()        >= totalDim)
                /* ---- per-compartment overlap check ------------------------ */
                .concatMap(c ->
                        reservationRepository.findByCompartmentId(c.getId())
                                .filter(r -> "confirmed".equalsIgnoreCase(r.getStatus()))
                                .filter(r -> r.getReservationStart().isBefore(end) &&
                                        r.getReservationEnd()  .isAfter(start))
                                .hasElements()
                                .map(hasOverlap -> !hasOverlap)
                                .filter(canUse -> canUse)
                                .map(canUse -> c.getId())
                )
                .next()   // first compartment that we successfully locked
                .switchIfEmpty(Mono.error(new ConflictException("No compartments available in that window")));
    }

    /** Returns a DTO for UI if at least one qualifying compartment exists. */
    private Mono<EasyboxDto> boxIfAvailable(Easybox box,
                                            LocalDateTime st,
                                            LocalDateTime ed,
                                            Integer minTemp,
                                            Integer totalDim,
                                            Double userLat,
                                            Double userLon) {

        return compartmentRepository.findByEasyboxId(box.getId())
                .filter(c -> "free".equalsIgnoreCase(c.getStatus()))
                .filter(c -> c.getCondition() != null &&
                        ( "good".equalsIgnoreCase(c.getCondition())
                                || "clean".equalsIgnoreCase(c.getCondition())))
                .filter(c -> minTemp  == null || c.getTemperature() >= minTemp)
                .filter(c -> totalDim == null || c.getSize()        >= totalDim)
                .concatMap(c ->
                        // confirmed-overlap test
                        reservationRepository.findByCompartmentId(c.getId())
                                .filter(r -> "confirmed".equalsIgnoreCase(r.getStatus()))
                                .filter(r -> st != null && ed != null &&
                                        r.getReservationStart().isBefore(ed) &&
                                        r.getReservationEnd()  .isAfter(st))
                                .hasElements()
                                .map(hasOverlap -> !hasOverlap)
                                .filter(ok -> ok)
                )
                .hasElements()
                .filter(Boolean::booleanValue)
                .map(ok -> {
                    EasyboxDto dto = new EasyboxDto(
                            box.getId(),
                            box.getAddress(),
                            box.getStatus(),
                            box.getLatitude(),
                            box.getLongitude(),
                            1000   // capacity placeholder
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

    /* ---------------- distance-fallback helpers (unchanged logic) -------- */

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
