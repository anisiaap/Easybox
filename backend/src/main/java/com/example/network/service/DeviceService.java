package com.example.network.service;

import com.example.network.entity.Compartment;
import com.example.network.exception.ConflictException;
import com.example.network.mqtt.MqttClientManager;
import com.example.network.repository.CompartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DeviceService {

    private final CompartmentRepository compartmentRepository;
    @Autowired
    private MqttClientManager mqttClientManager;
    public DeviceService(
            CompartmentRepository compartmentRepository
    ) {
        this.compartmentRepository = compartmentRepository;
    }
    public Mono<Long> findAndReserveCompartment(String clientId, Long easyboxId, Integer minTemp, Integer totalDim) {
        if (clientId == null || clientId.isEmpty()) {
            return Mono.error(new ConflictException("Device clientId is missing or empty."));
        }

        return compartmentRepository.findByEasyboxId(easyboxId)
                .filter(this::isSuitableAndFree)
                .filter(comp -> minTemp == null || comp.getTemperature() >= minTemp)
                .filter(comp -> totalDim == null || comp.getSize() >= totalDim)
                .next()
                .switchIfEmpty(Mono.error(new ConflictException("No suitable compartments available.")))
                .flatMap(comp -> markCompartmentBusy(comp).map(Compartment::getId));
    }
    private boolean isSuitableAndFree(Compartment c) {
        if (!"free".equalsIgnoreCase(c.getStatus())) {
            return false;
        }
        if (c.getCondition() == null) {
            return false;
        }
        String cond = c.getCondition().toLowerCase();
        return ("good".equals(cond) || "clean".equals(cond));
    }

    private Mono<Compartment> markCompartmentBusy(Compartment comp) {
        comp.setStatus("busy");
        return compartmentRepository.save(comp);
    }

    private Mono<Compartment> revertCompartmentToFree(Compartment comp) {
        comp.setStatus("free");
        return compartmentRepository.save(comp);
    }
}
