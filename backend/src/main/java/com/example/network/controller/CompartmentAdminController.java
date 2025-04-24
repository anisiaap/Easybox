package com.example.network.controller;

import com.example.network.service.CompartmentSyncService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/compartments")
public class CompartmentAdminController {

    private final CompartmentSyncService syncService;

    public CompartmentAdminController(CompartmentSyncService syncService) {
        this.syncService = syncService;
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
}
