// src/main/java/com/example/network/controller/EasyboxAdminController.java
package com.example.network.controller;

import com.example.network.dto.CompartmentDto;
import com.example.network.dto.DeviceDetailsDto;
import com.example.network.dto.PredefinedValuesDto;
import com.example.network.entity.Compartment;
import com.example.network.entity.Easybox;
import com.example.network.repository.CompartmentRepository;
import com.example.network.repository.EasyboxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/admin/easyboxes")
public class EasyboxAdminController {

    private EasyboxRepository easyboxRepository;
    @Autowired
    private PredefinedValuesDto predefinedValuesDto;
    private final CompartmentRepository compartmentRepository;

    public EasyboxAdminController(EasyboxRepository easyboxRepository, CompartmentRepository compartmentRepository) {
        this.easyboxRepository = easyboxRepository;
        this.compartmentRepository = compartmentRepository;
    }

    // GET all Easyboxes as a reactive stream
    @GetMapping()
    public Flux<Easybox> getAllEasyboxes() {
        return easyboxRepository.findAll();
    }

    @GetMapping("/{id}/details")
    public Mono<DeviceDetailsDto> getEasyboxDetails(@PathVariable Long id) {
        return easyboxRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Easybox not found")))
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
}
