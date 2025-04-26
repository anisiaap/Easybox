package com.example.network.service;

import com.example.network.dto.CompartmentDto;
import com.example.network.entity.Compartment;
import com.example.network.entity.Easybox;
import com.example.network.mqtt.MqttClientManager;
import com.example.network.repository.CompartmentRepository;
import com.example.network.repository.EasyboxRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CompartmentSyncService {

    private final CompartmentRepository compartmentRepository;
    private final EasyboxRepository easyboxRepository;
    private final MqttClientManager mqttClientManager;
    public CompartmentSyncService(
            CompartmentRepository compartmentRepository, EasyboxRepository easyboxRepository, MqttClientManager mqttClientManager
    ) {
        this.compartmentRepository = compartmentRepository;
        this.easyboxRepository = easyboxRepository;
        this.mqttClientManager = mqttClientManager;
    }
    public Mono<Void> updateStatus(Long id, String status) {
        return compartmentRepository.findById(id)
                .flatMap(comp -> {
                    comp.setStatus(status);
                    return compartmentRepository.save(comp);
                })
                .then(); // Mono<Void>
    }
    public Mono<Void> syncCompartmentsForEasybox(Long easyboxId) {
        return easyboxRepository.findById(easyboxId)
                .flatMapMany(easybox -> {
                    String clientId = easybox.getClientId();
                    if (clientId == null || clientId.isBlank()) {
                        return Flux.error(new RuntimeException("No clientId for easybox " + easyboxId));
                    }

                    return mqttClientManager.requestCompartments(clientId) // <--- NEW
                            .flatMapMany(Flux::fromIterable) // because the result is a List<CompartmentDto>
                            .flatMap(dto -> upsertCompartment(easybox, dto))
                            .then();
                })
                .then();
    }
    private Mono<Compartment> upsertCompartment(Easybox easybox, CompartmentDto dto) {
        return compartmentRepository.findById(dto.getId()) // look up by ID
                .defaultIfEmpty(new Compartment()) // if not found, create new
                .flatMap(existing -> {
                    existing.setId(dto.getId());
                    existing.setEasyboxId(easybox.getId());
                    existing.setSize(dto.getSize());
                    existing.setTemperature(dto.getTemperature());
                    existing.setStatus(dto.getStatus());
                    existing.setCondition(dto.getCondition());
                    return compartmentRepository.save(existing); // <<< save it to the database
                });
    }


}
