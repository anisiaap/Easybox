package com.example.easyboxdevice.service;

import com.example.easyboxdevice.model.Compartment;
import com.example.easyboxdevice.config.DeviceConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Service
public class CompartmentService {

    private DeviceConfig deviceConfig;

    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass().getClassLoader().getResourceAsStream("device-config.json");
            if (is == null) {
                throw new RuntimeException("device-config.json not found in resources");
            }
            deviceConfig = mapper.readValue(is, DeviceConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load device configuration", e);
        }
    }

    public List<Compartment> getAllCompartments() {
        return deviceConfig.getCompartments();
    }

    public Optional<Compartment> getCompartmentById(Long id) {
        return deviceConfig.getCompartments().stream().filter(c -> c.getId().equals(id)).findFirst();
    }

    // Reserve a compartment: if status is "free", update it to "busy".
    public boolean reserveCompartment(Long id) {
        Optional<Compartment> opt = getCompartmentById(id);
        if (opt.isPresent()) {
            Compartment comp = opt.get();
            if ("free".equalsIgnoreCase(comp.getStatus())) {
                comp.setStatus("busy");
                return true;
            }
        }
        return false;
    }

    // Clean a compartment: mark condition as "good" and status as "free".
    public boolean cleanCompartment(Long id) {
        Optional<Compartment> opt = getCompartmentById(id);
        if (opt.isPresent()) {
            Compartment comp = opt.get();
            comp.setCondition("good");
            comp.setStatus("free");
            return true;
        }
        return false;
    }

    public DeviceConfig getDeviceConfig() {
        return deviceConfig;
    }
}
