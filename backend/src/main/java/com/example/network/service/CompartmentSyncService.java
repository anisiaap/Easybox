package com.example.network.service;

import com.example.network.dto.CompartmentDto;
import com.example.network.model.Compartment;
import com.example.network.model.Easybox;
import com.example.network.exception.ConfigurationException;
import com.example.network.exception.InvalidRequestException;
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
                .switchIfEmpty(Mono.error(new InvalidRequestException("Compartment " + id + " not found")))
                .flatMap(comp -> {
                    comp.setStatus(status);
                    return compartmentRepository.save(comp);
                })
                .then();
    }
    public Mono<Void> syncCompartmentsForEasybox(Long easyboxId) {
        System.out.println(" Syncing compartments for easyboxId=" + easyboxId);

        return easyboxRepository.findById(easyboxId)
                .switchIfEmpty(Mono.error(new InvalidRequestException("Easybox " + easyboxId + " not found")))
                .flatMap(easybox -> {
                    String clientId = easybox.getClientId();
                    if (clientId == null || clientId.isBlank()) {
                        return Mono.error(new ConfigurationException("No clientId for easybox " + easyboxId));
                    }
                    return mqttClientManager.requestCompartments(clientId)
                            .flatMapMany(Flux::fromIterable)
                            .flatMap(dto -> upsertCompartment(easybox, dto))
                            .then();
                });
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
                    System.out.println("Upserting compartment: id=" + dto.getId() + ", size=" + dto.getSize());

                    return compartmentRepository.save(existing);
                });
    }


}
