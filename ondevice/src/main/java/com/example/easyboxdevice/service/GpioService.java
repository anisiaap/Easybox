package com.example.easyboxdevice.service;

import com.example.easyboxdevice.model.GpioConfig;
import com.example.easyboxdevice.model.GpioMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class GpioService {
    private final Map<Long, Integer> pinMap = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("gpio-config.json");
            GpioConfig config = new ObjectMapper().readValue(is, GpioConfig.class);
            for (GpioMapping mapping : config.getMappings()) {
                pinMap.put((long) mapping.getCompartmentId(), mapping.getPin());
                // Setup the pin if needed (using Pi4J or similar)
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load GPIO config", e);
        }
    }

    public void openLock(Long compartmentId) {
        Integer pin = pinMap.get(compartmentId);
        if (pin != null) {
            System.out.println("🔓 Unlocking compartment " + compartmentId + " on pin " + pin);
            // TODO: Use GPIO API to activate pin
        } else {
            System.err.println("❌ No GPIO pin mapped for compartment " + compartmentId);
        }
    }
}

