// src/main/java/com/example/network/controller/DeviceRegistrationController.java
package com.example.network.controller;

import com.example.network.dto.RegistrationRequest;
import com.example.network.entity.Easybox;
import com.example.network.exception.ConflictException;
import com.example.network.exception.GeocodingException;
import com.example.network.repository.EasyboxRepository;
import com.example.network.service.GeocodingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/device")
public class DeviceRegistrationController {

    private final EasyboxRepository easyboxRepository;
    private final GeocodingService geocodingService; // <--- Injected



    public DeviceRegistrationController(
            EasyboxRepository easyboxRepository,
            GeocodingService geocodingService
    ) {
        this.easyboxRepository = easyboxRepository;
        this.geocodingService = geocodingService;
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<Easybox>> registerDevice(@RequestBody RegistrationRequest request) {
        if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return geocodingService.geocodeAddress(request.getAddress())
                .flatMap(coords -> {
                    // If geocode fails or returns [0,0], you may decide to treat it as an error:
                    if (coords[0] == 0.0 && coords[1] == 0.0) {
                        return Mono.error(new GeocodingException("No coordinates found for: " + request.getAddress()));
                    }

                    double newLat = coords[0];
                    double newLon = coords[1];

                    // First, try to find an existing Easybox by address
                    return easyboxRepository.findByAddressIgnoreCase(request.getAddress())
                            .flatMap(existing -> {
                                // Found an existing one => update
                                existing.setClientId(request.getClientId());
                                existing.setStatus(request.getStatus());
                                existing.setLatitude(newLat);
                                existing.setLongitude(newLon);
                                return easyboxRepository.save(existing);
                            })
                            .switchIfEmpty(
                                    // No existing record => conflict check with other boxes
                                    easyboxRepository.findAll().collectList().flatMap(allBoxes -> {
                                        for (Easybox box : allBoxes) {
                                            double dist = geocodingService.distance(
                                                    newLat, newLon,
                                                    box.getLatitude(), box.getLongitude()
                                            );
                                            // If any other box is too close, reject
                                            if (dist < 100) {
                                                return Mono.error(new ConflictException(
                                                        "An Easybox already exists within 100 meters of this location."
                                                ));
                                            }
                                        }
                                        // If no conflict => create new
                                        Easybox newEasybox = new Easybox();
                                        newEasybox.setAddress(request.getAddress());
                                        newEasybox.setClientId(request.getClientId());
                                        newEasybox.setStatus(request.getStatus());
                                        newEasybox.setLatitude(newLat);
                                        newEasybox.setLongitude(newLon);
                                        return easyboxRepository.save(newEasybox);
                                    })
                            );
                })
                .flatMap(savedBox -> {
                    return Mono.just(ResponseEntity.ok(savedBox)); });
    }
}
