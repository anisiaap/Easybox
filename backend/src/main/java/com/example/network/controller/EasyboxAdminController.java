// src/main/java/com/example/network/controller/EasyboxAdminController.java
package com.example.network.controller;

import com.example.network.config.CurlUtils;
import com.example.network.dto.CompartmentDto;
import com.example.network.dto.DeviceDetailsDto;
import com.example.network.dto.PredefinedValuesDto;
import com.example.network.entity.Easybox;
import com.example.network.repository.EasyboxRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/admin/easyboxes")
public class EasyboxAdminController {

    @Autowired
    private EasyboxRepository easyboxRepository;

    private final WebClient webClient;

    // Inject the central predefined values bean
    @Autowired
    private PredefinedValuesDto predefinedValuesDto;

    public EasyboxAdminController(EasyboxRepository easyboxRepository, WebClient.Builder webClientBuilder) {
        this.easyboxRepository = easyboxRepository;
        this.webClient = webClientBuilder.build();
    }

    // GET all Easyboxes as a reactive stream
    @GetMapping()
    public Flux<Easybox> getAllEasyboxes() {
        return easyboxRepository.findAll();
    }

    // GET compartments for a specific Easybox
    @GetMapping("/{id}/compartments")
    public Mono<ResponseEntity<List<CompartmentDto>>> getCompartments(@PathVariable Long id) {
        return easyboxRepository.findById(id)
                .flatMap(easybox -> {
                    if (easybox.getDeviceUrl() == null || easybox.getDeviceUrl().trim().isEmpty()) {
                        return Mono.just(new ResponseEntity<List<CompartmentDto>>(HttpStatus.BAD_REQUEST));
                    }
                    String url = easybox.getDeviceUrl() + "/api/ondevice/compartments";
                    System.out.println("Calling device backend at: " + url);

                    return webClient.get()
                            .uri(url)
                            .retrieve()
                            .bodyToMono(CompartmentDto[].class)
                            .timeout(Duration.ofSeconds(5))
                            .map(array -> ResponseEntity.ok(Arrays.asList(array)))
                            .onErrorResume(e -> {
                                System.err.println("Error communicating with " + url + ": " + e.getMessage());
                                return Mono.just(new ResponseEntity<List<CompartmentDto>>(HttpStatus.INTERNAL_SERVER_ERROR));
                            });
                })
                .defaultIfEmpty(new ResponseEntity<List<CompartmentDto>>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/{id}/details")
    public Mono<ResponseEntity<DeviceDetailsDto>> getDeviceDetails(@PathVariable Long id) {
        return easyboxRepository.findById(id)
                .flatMap(easybox -> {
                    if (easybox.getDeviceUrl() == null || easybox.getDeviceUrl().trim().isEmpty()) {
                        return Mono.just(new ResponseEntity<DeviceDetailsDto>(HttpStatus.BAD_REQUEST));
                    }
                    String url = easybox.getDeviceUrl() + "/api/ondevice/compartments";
                    System.out.println("Calling device backend compartments at: " + url);

                    return webClient.get()
                            .uri(url)
                            .retrieve()
                            .bodyToMono(CompartmentDto[].class)
                            .timeout(Duration.ofSeconds(5))
                            .map(compartmentsArray -> {
                                DeviceDetailsDto details = new DeviceDetailsDto();
                                details.setCompartments(Arrays.asList(compartmentsArray));
                                // Use the centrally stored predefined values
                                details.setPredefinedValues(predefinedValuesDto);
                                return ResponseEntity.ok(details);
                            })
                            .onErrorResume(e -> {
                                System.err.println("Error communicating with " + url + ": " + e.getMessage());
                                return Mono.just(new ResponseEntity<DeviceDetailsDto>(HttpStatus.INTERNAL_SERVER_ERROR));
                            });
                })
                .defaultIfEmpty(new ResponseEntity<DeviceDetailsDto>(HttpStatus.NOT_FOUND));
    }
}
