//// src/main/java/com/example/network/controller/DeviceRegistrationController.java
package com.example.network.controller;

import com.example.network.config.JwtVerifier;
import com.example.network.dto.RegistrationRequest;
import com.example.network.entity.Easybox;
import com.example.network.exception.ConflictException;
import com.example.network.exception.GeocodingException;
import com.example.network.repository.CompartmentRepository;
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
    private final CompartmentRepository compartmentRepository;
    private final JwtVerifier jwtVerifier;
    public DeviceRegistrationController(EasyboxRepository easyboxRepository,
                                        GeocodingService  geocodingService, CompartmentSyncService syncService, CompartmentRepository compartmentRepository, JwtVerifier jwtVerifier) {
        this.easyboxRepository = easyboxRepository;
        this.geocodingService  = geocodingService;
        this.syncService = syncService;
        this.compartmentRepository = compartmentRepository;
        this.jwtVerifier = jwtVerifier;
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<Easybox>> registerDevice(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody RegistrationRequest req
    ) {
        if (!authHeader.startsWith("Bearer ")) {
            return Mono.error(new SecurityException("Missing or invalid Authorization header"));
        }

        String token = authHeader.substring("Bearer ".length());
        return jwtVerifier.verifyAndExtractClientId(token)
                .flatMap(extractedClientId -> {
                    if (!extractedClientId.equals(req.getClientId())) {
                        return Mono.error(new SecurityException("ClientId in JWT does not match"));
                    }

                    return geocodingService.geocodeAddress(req.getAddress())
                            .flatMap(coords -> {
                                double lat = coords[0], lon = coords[1];

                                return easyboxRepository.findByClientId(req.getClientId())
                                        .switchIfEmpty(
                                                easyboxRepository.findAll()
                                                        .filter(box -> geocodingService.distance(
                                                                lat, lon, box.getLatitude(), box.getLongitude()) < 10)
                                                        .next()
                                        )
                                        .defaultIfEmpty(new Easybox())
                                        .flatMap(box -> {
                                            if (box.getId() != null) {
                                                if (!box.getClientId().equals(req.getClientId())) {
                                                    return Mono.error(new SecurityException("Device clientId mismatch"));
                                                }

                                                copyFields(box, req, lat, lon);
                                                return easyboxRepository.save(box)
                                                        .flatMap(saved ->
                                                                compartmentRepository.findByEasyboxId(saved.getId())
                                                                        .hasElements()
                                                                        .flatMap(hasCompartments -> {
                                                                            if (hasCompartments) {
                                                                                return Mono.just(saved);
                                                                            }
                                                                            return syncService.syncCompartmentsForEasybox(saved.getId())
                                                                                    .onErrorResume(e -> {
                                                                                        System.err.println("❌ Failed to sync compartments on registration: " + e.getMessage());
                                                                                        return Mono.empty();
                                                                                    })
                                                                                    .thenReturn(saved);
                                                                        })
                                                        )
                                                        .map(saved -> {
                                                            ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
                                                            if (Boolean.TRUE.equals(saved.getApproved()) && saved.getSecretKey() != null) {
                                                                builder = builder.header("X-Device-Secret", saved.getSecretKey());
                                                            }
                                                            return builder.body(saved);
                                                        });
                                            }

                                            return easyboxRepository.findAll()
                                                    .any(other -> geocodingService.distance(
                                                            lat, lon,
                                                            other.getLatitude(), other.getLongitude()) < 100)
                                                    .flatMap(tooClose -> {
                                                        if (tooClose) {
                                                            return Mono.error(new ConflictException("Another Easybox is within 100 m"));
                                                        }

                                                        Easybox newBox = new Easybox();
                                                        copyFields(newBox, req, lat, lon);
                                                        newBox.setApproved(false);
                                                        return easyboxRepository.save(newBox)
                                                                .map(saved -> ResponseEntity.ok(saved));
                                                    });
                                        });
                            });
                });
    }
    // ──────────────────────────────────────────────────────────────────────────────
    private void copyFields(Easybox box, RegistrationRequest req,
                            double lat, double lon) {
        box.setAddress(req.getAddress());
        box.setClientId(req.getClientId());
        box.setLatitude(lat);
        box.setLongitude(lon);
        box.setStatus(req.getStatus());
    }

    @GetMapping("/{clientId}/secret")
    public Mono<ResponseEntity<String>> getDeviceSecret(
            @PathVariable String clientId,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (!authHeader.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(403).body("Missing or invalid Authorization header"));
        }

        String token = authHeader.substring("Bearer ".length());
        return jwtVerifier.verifyAndExtractClientId(token)
                .flatMap(extractedClientId -> {
                    if (!extractedClientId.equals(clientId)) {
                        return Mono.just(ResponseEntity.status(403).body("Invalid clientId"));
                    }

                    return easyboxRepository.findByClientId(clientId)
                            .flatMap(box -> {
                                if (!Boolean.TRUE.equals(box.getApproved()) || box.getSecretKey() == null) {
                                    return Mono.just(ResponseEntity.status(403).body("Device not approved or secret not assigned"));
                                }

                                return Mono.just(ResponseEntity.ok(box.getSecretKey()));
                            })
                            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
                })
                .onErrorResume(e -> {
                    return Mono.just(ResponseEntity.status(403).body("Invalid token: " + e.getMessage()));
                });
    }
}

