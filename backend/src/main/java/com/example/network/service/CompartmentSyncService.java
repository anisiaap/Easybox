package com.example.network.service;

import com.example.network.dto.CompartmentDto;
import com.example.network.entity.Compartment;
import com.example.network.entity.Easybox;
import com.example.network.repository.CompartmentRepository;
import com.example.network.repository.EasyboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@Service
public class CompartmentSyncService {

    private final WebClient webClient;
    private final EasyboxRepository easyboxRepository;
    private final CompartmentRepository compartmentRepository;

    public CompartmentSyncService(
            WebClient.Builder webClientBuilder,
            EasyboxRepository easyboxRepository,
            CompartmentRepository compartmentRepository
    ) {
        this.webClient = webClientBuilder.build();
        this.easyboxRepository = easyboxRepository;
        this.compartmentRepository = compartmentRepository;
    }

    /**
     * Sync compartments for a single Easybox by ID.
     * 1) Find the easybox (must have deviceUrl).
     * 2) Call device backend /api/ondevice/compartments
     * 3) Upsert them in the central DB.
     */
    public Mono<Void> syncCompartmentsForEasybox(Long easyboxId) {
        return easyboxRepository.findById(easyboxId)
                .flatMapMany(easybox -> {
                    String deviceUrl = easybox.getDeviceUrl();
                    if (deviceUrl == null || deviceUrl.isBlank()) {
                        return Flux.error(new RuntimeException("No deviceUrl for easybox " + easyboxId));
                    }

                    String compartmentsUrl = deviceUrl + "/api/ondevice/compartments";
                    return webClient.get()
                            .uri(compartmentsUrl)
                            .retrieve()
                            .bodyToFlux(CompartmentDto.class)
                            // For each compartment from the device, map to our entity
                            .flatMap(dto -> upsertCompartment(easybox, dto));
                })
                .then();
    }

    private Mono<Compartment> upsertCompartment(Easybox easybox, CompartmentDto dto) {
        return compartmentRepository.findById(dto.getId())
                .defaultIfEmpty(new Compartment()) // if not found, create new
                .flatMap(existing -> {
                    existing.setId(dto.getId());  // device ID might be the same
                    existing.setEasyboxId(easybox.getId());
                    existing.setSize(dto.getSize());
                    existing.setTemperature(dto.getTemperature());
                    existing.setStatus(dto.getStatus());
                    existing.setCondition(dto.getCondition());
                    return compartmentRepository.save(existing);
                });
    }
    public Mono<Void> updateStatus(Long id, String status) {
        return compartmentRepository.findById(id)
                .flatMap(comp -> {
                    comp.setStatus(status);
                    return compartmentRepository.save(comp);
                })
                .then(); // Mono<Void>
    }
}
