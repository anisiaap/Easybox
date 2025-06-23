// src/main/java/com/example/network/controller/EasyboxAdminController.java
package com.example.network.controller;

import com.example.network.dto.CompartmentDto;
import com.example.network.dto.DeviceDetailsDto;
import com.example.network.dto.PredefinedValuesDto;
import com.example.network.model.Compartment;
import com.example.network.model.Easybox;
import com.example.network.exception.ConflictException;
import com.example.network.exception.NotFoundException;
import com.example.network.repository.CompartmentRepository;
import com.example.network.repository.EasyboxRepository;
import com.example.network.repository.ReservationRepository;
import com.example.network.service.CompartmentSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/easyboxes")
public class EasyboxAdminController {

    private EasyboxRepository easyboxRepository;
    @Autowired
    private PredefinedValuesDto predefinedValuesDto;
    private final CompartmentRepository compartmentRepository;
    private final CompartmentSyncService syncService;
    private final ReservationRepository reservationRepository;
    public EasyboxAdminController(EasyboxRepository easyboxRepository, CompartmentRepository compartmentRepository, CompartmentSyncService syncService, ReservationRepository reservationRepository) {
        this.easyboxRepository = easyboxRepository;
        this.compartmentRepository = compartmentRepository;
        this.syncService = syncService;
        this.reservationRepository = reservationRepository;
    }

    // GET all Easyboxes as a reactive stream
    @GetMapping()
    public Flux<Easybox> getAllEasyboxes() {
        return easyboxRepository.findAll();
    }

    @GetMapping("/{id}/details")
    public Mono<DeviceDetailsDto> getEasyboxDetails(@PathVariable Long id) {
        return easyboxRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Easybox not found")))
                .flatMap(easybox ->
                        compartmentRepository.findByEasyboxId(id)
                                .map(compartment -> mapToDto(compartment))
                                .collectList()
                                .map(compartmentDtos -> {
                                    DeviceDetailsDto details = new DeviceDetailsDto();
                                    details.setCompartments(compartmentDtos);
                                    details.setPredefinedValues(predefinedValuesDto); // ✨ use the injected predefined values
                                    return details;
                                })
                );
    }

    private CompartmentDto mapToDto(Compartment compartment) {
        return new CompartmentDto(
                compartment.getId(),
                compartment.getSize(),
                compartment.getTemperature(),
                compartment.getStatus(),
                compartment.getCondition()
        );
    }
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteEasybox(@PathVariable Long id) {
        return easyboxRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Easybox not found")))
                .flatMap(easybox ->
                        reservationRepository.findByEasyboxId(id)
                                .filter(res -> {
                                    LocalDateTime now = LocalDateTime.now();
                                    return res.getReservationStart() == null || !res.getReservationStart().isBefore(now);
                                })
                                .flatMap(res -> {
                                    res.setStatus("cancelled");
                                    res.setEasyboxId(null);
                                    return reservationRepository.save(res);
                                })
                                .thenMany(compartmentRepository.findByEasyboxId(id)
                                        .flatMap(compartmentRepository::delete))
                                .then(easyboxRepository.delete(easybox))
                                .then(Mono.just(ResponseEntity.noContent().build()))
                );
    }

    @PostMapping("/{id}/approve")
    public Mono<ResponseEntity<String>> approveBox(@PathVariable Long id) {
        return easyboxRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Easybox not found")))
                .flatMap(box -> {
                    if (Boolean.TRUE.equals(box.getApproved())) {
                        return Mono.error(new ConflictException("Already approved"));
                    }

                    box.setStatus("inactive");
                    box.setApproved(true);
                    box.setSecretKey(UUID.randomUUID().toString());
                    box.setLastSecretRotation(LocalDateTime.now());

                    return easyboxRepository.save(box)
                            .flatMap(saved ->
                                    syncService.syncCompartmentsForEasybox(saved.getId())
                                            .timeout(Duration.ofSeconds(30)) // optional extra safety
                                            .onErrorResume(e -> {
                                                System.err.println("❌ Compartment sync failed: " + e.getMessage());
                                                return Mono.empty();
                                            })
                                            .thenReturn(ResponseEntity.ok(saved.getSecretKey()))
                            );
                })
                .onErrorResume(e -> {
                    System.err.println("❌ Unexpected error in approval: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(500).body("Approval failed: " + e.getMessage()));
                });
    }
}
