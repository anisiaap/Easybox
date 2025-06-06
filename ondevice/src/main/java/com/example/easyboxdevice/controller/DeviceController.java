package com.example.easyboxdevice.controller;

import com.example.easyboxdevice.model.Compartment;
import com.example.easyboxdevice.config.DeviceConfig;
import com.example.easyboxdevice.service.CompartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/api/ondevice")
public class DeviceController {

    @Autowired
    private CompartmentService compartmentService;

    @GetMapping("/status")
    public Mono<ResponseEntity<String>> getStatus() {
        return Mono.fromCallable(() -> {
            int total = compartmentService.getAllCompartments().size();
            return ResponseEntity.ok("Easybox Device is online. Total compartments: " + total);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/compartments")
    public Flux<Compartment> getCompartments() {
        List<Compartment> compartments = compartmentService.getAllCompartments();
        return Flux.fromIterable(compartments);
    }

    @GetMapping("/config")
    public Mono<ResponseEntity<DeviceConfig>> getConfig() {
        return Mono.just(ResponseEntity.ok(compartmentService.getDeviceConfig()));
    }

    @PostMapping("/compartments/{id}/reserve")
    public Mono<ResponseEntity<String>> reserveCompartment(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            boolean reserved = compartmentService.reserveCompartment(id);
            if (reserved) {
                return ResponseEntity.ok("Compartment " + id + " reserved successfully.");
            } else {
                return ResponseEntity.badRequest().body("Compartment " + id + " is not available for reservation.");
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/compartments/{id}/clean")
    public Mono<ResponseEntity<String>> cleanCompartment(@PathVariable Long id) {
        return Mono.fromCallable(() -> {
            boolean cleaned = compartmentService.cleanCompartment(id);
            if (cleaned) {
                return ResponseEntity.ok("Compartment " + id + " cleaned successfully.");
            } else {
                return ResponseEntity.badRequest().body("Compartment " + id + " could not be cleaned (not found).");
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
