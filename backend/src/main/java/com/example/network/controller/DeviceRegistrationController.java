//// src/main/java/com/example/network/controller/DeviceRegistrationController.java
package com.example.network.controller;

import com.example.network.config.JwtVerifier;
import com.example.network.dto.RegistrationRequest;
import com.example.network.entity.Easybox;
import com.example.network.exception.ConflictException;
import com.example.network.exception.GeocodingException;
import com.example.network.repository.EasyboxRepository;
import com.example.network.service.CompartmentSyncService;
import com.example.network.service.GeocodingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/device")
public class DeviceRegistrationController {

    private final EasyboxRepository easyboxRepository;
    private final GeocodingService  geocodingService;
    private final CompartmentSyncService syncService;
    private final JwtVerifier jwtVerifier;
    public DeviceRegistrationController(EasyboxRepository easyboxRepository,
                                        GeocodingService  geocodingService, CompartmentSyncService syncService, JwtVerifier jwtVerifier) {
        this.easyboxRepository = easyboxRepository;
        this.geocodingService  = geocodingService;
        this.syncService = syncService;
        this.jwtVerifier = jwtVerifier;
    }

    // ──────────────────────────────────────────────────────────────────────────────
    //  POST /api/device/register
    //  – idempotent for the same locker
    //  – rejects another locker within 100 m
    // ──────────────────────────────────────────────────────────────────────────────
    @PostMapping("/register")
    public Mono<ResponseEntity<Easybox>> registerDevice(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody RegistrationRequest req
    ) {
        // jwt.getSubject() is the clientId
        String extractedClientId = jwt.getSubject();

        if (!extractedClientId.equals(req.getClientId())) {
            return Mono.error(new SecurityException("ClientId in JWT does not match"));
        }
        if (req.getAddress() == null || req.getAddress().isBlank()) {
            return Mono.error(new GeocodingException("Address is blank"));
        }

        /* 1️⃣  Geocode the address */
        return geocodingService.geocodeAddress(req.getAddress())
                .flatMap(coords -> {
                    double lat = coords[0], lon = coords[1];

                    /* 2️⃣  Try to find the box by client-id first */
                    return easyboxRepository.findByClientId(req.getClientId())
                            /* 3️⃣  …or by ~10 m coordinate match                                       */
                            .switchIfEmpty(
                                    easyboxRepository.findAll()
                                            .filter(box -> geocodingService.distance(
                                                    lat, lon, box.getLatitude(), box.getLongitude()
                                            ) < 10)                               // same physical box
                                            .next()
                            )
                            .defaultIfEmpty(new Easybox())               // brand-new object
                            /* 4️⃣  Decide: update / conflict / insert                                   */
                            .flatMap(box -> {

                                /* 4a – UPDATE (same locker) */
                                if (box.getId() != null) {
                                    copyFields(box, req, lat, lon);
                                    return easyboxRepository.save(box)
                                            .map(saved -> {
                                                ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
                                                if (Boolean.TRUE.equals(saved.getApproved())) {
                                                    builder = builder.header("X-Device-Secret", saved.getSecretKey());
                                                }
                                                return builder.body(saved);
                                            });
                                }

                                /* 4b – CONFLICT check <100 m */
                                return easyboxRepository.findAll()
                                        .any(other -> geocodingService.distance(
                                                lat, lon,
                                                other.getLatitude(), other.getLongitude()
                                        ) < 100)
                                        .flatMap(tooClose -> {
                                            if (tooClose) {
                                                return Mono.error(new ConflictException(
                                                        "Another Easybox is within 100 m")
                                                );
                                            }

                                            /* 4c – INSERT brand-new box */
                                            Easybox newBox = new Easybox();
                                            copyFields(newBox, req, lat, lon);
                                            newBox.setApproved(false); // pending until manual approval
                                            newBox.setSecretKey(UUID.randomUUID().toString());
                                            newBox.setLastSecretRotation(LocalDateTime.now());
                                            return easyboxRepository.save(newBox)
                                                    .flatMap(savedBox ->
                                                            syncService.syncCompartmentsForEasybox(savedBox.getId())
                                                                    .then(Mono.just(ResponseEntity.ok()
                                                                            .header("X-Device-Secret", savedBox.getSecretKey() != null ? savedBox.getSecretKey() : "")
                                                                            .body(savedBox)))
                                                    );
                                        });
                            });
                });
    }

    // ──────────────────────────────────────────────────────────────────────────────
    private void copyFields(Easybox box, RegistrationRequest req,
                            double lat, double lon) {
        box.setAddress(req.getAddress());
        box.setClientId(req.getClientId());
        box.setStatus(req.getStatus());
        box.setLatitude(lat);
        box.setLongitude(lon);
    }


}

