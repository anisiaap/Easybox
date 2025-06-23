package com.example.network.controller;

import com.example.network.dto.CompartmentDto;
import com.example.network.dto.CompartmentDtoWithAddress;
import com.example.network.repository.CompartmentRepository;
import com.example.network.repository.EasyboxRepository;
import com.example.network.service.CompartmentSyncService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/compartments")
public class CompartmentAdminController {

    private final CompartmentSyncService syncService;
    private final CompartmentRepository compartmentRepository;
    private final EasyboxRepository easyboxRepository;
    public CompartmentAdminController(CompartmentSyncService syncService, CompartmentRepository compartmentRepository, EasyboxRepository easyboxRepository) {
        this.syncService = syncService;
        this.compartmentRepository = compartmentRepository;
        this.easyboxRepository = easyboxRepository;
    }
    @PostMapping("/sync/{easyboxId}")
    public Mono<Void> syncCompartments(@PathVariable Long easyboxId) {
        return syncService.syncCompartmentsForEasybox(easyboxId);
    }

    @PostMapping("/{id}/mark-busy")
    public Mono<Void> markBusy(@PathVariable Long id) {
        return syncService.updateStatus(id, "busy");
    }

    @PostMapping("/{id}/mark-free")
    public Mono<Void> markFree(@PathVariable Long id) {
        return syncService.updateStatus(id, "free");
    }

    @PostMapping("/{id}/mark-dirty")
    public Mono<Void> markDirty(@PathVariable Long id) {
        return syncService.updateStatus(id, "dirty");
    }

    @PostMapping("/{id}/mark-clean")
    public Mono<Void> markClean(@PathVariable Long id) {
        return syncService.updateStatus(id, "free");
    }
    @GetMapping("")
    public Flux<CompartmentDtoWithAddress> getAllWithAddress() {
        return compartmentRepository.findAll()
                .flatMap(comp -> easyboxRepository.findById(comp.getEasyboxId())
                        .switchIfEmpty(Mono.empty()) // skip compartments with missing easybox
                        .map(box -> new CompartmentDtoWithAddress(
                                comp.getId(),
                                comp.getStatus(),
                                comp.getCondition(),
                                comp.getSize(),
                                comp.getTemperature(),
                                box.getAddress()
                        ))
                );
    }


}
