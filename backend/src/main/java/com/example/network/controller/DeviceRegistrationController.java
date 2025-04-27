// src/main/java/com/example/network/controller/DeviceRegistrationController.java
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
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
    public Mono<ResponseEntity<Easybox>> registerDevice(@RequestHeader("Authorization") String authHeader, @RequestBody RegistrationRequest req) {

        if (req.getAddress() == null || req.getAddress().isBlank()) {
            return Mono.error(new GeocodingException("Address is blank"));
        }
        // 🔒 Verify the JWT token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.error(new SecurityException("Missing or invalid Authorization header"));
        }
        String token = authHeader.substring(7); // skip "Bearer "
        String extractedClientId = jwtVerifier.verifyAndExtractClientId(token);

        // 🔒 Check clientId matches
        if (!extractedClientId.equals(req.getClientId())) {
            return Mono.error(new SecurityException("ClientId in JWT does not match RegistrationRequest"));
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
                                            .map(ResponseEntity::ok);
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
                                            return easyboxRepository.save(newBox)
                                                    .flatMap(savedBox ->
                                                            // Then immediately trigger sync!
                                                            syncService.syncCompartmentsForEasybox(savedBox.getId())
                                                                    .thenReturn(ResponseEntity.ok(savedBox))
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
