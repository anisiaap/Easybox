package com.example.network.service;

import com.example.network.repository.CompartmentRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CompartmentSyncService {

    private final CompartmentRepository compartmentRepository;

    public CompartmentSyncService(
            CompartmentRepository compartmentRepository
    ) {
        this.compartmentRepository = compartmentRepository;
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
