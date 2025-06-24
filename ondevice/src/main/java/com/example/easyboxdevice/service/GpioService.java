package com.example.easyboxdevice.service;

import com.example.easyboxdevice.model.GpioConfig;
import com.example.easyboxdevice.model.GpioMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class GpioService {

    private final Map<Long, Integer> gpioMap = new HashMap<>();
    private final String gpioChip = "gpiochip4"; // Confirm with `gpioinfo`

    @PostConstruct
    public void init() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("gpio-config.json");
            if (is == null) throw new RuntimeException("gpio-config.json not found");

            GpioConfig config = new ObjectMapper().readValue(is, GpioConfig.class);

            for (GpioMapping mapping : config.getMappings()) {
                int pin = mapping.getPin();
                gpioMap.put((long) mapping.getCompartmentId(), pin);  
                System.out.println(" Mapped compartment " + mapping.getCompartmentId() + " to pin " + pin);
            }


        } catch (Exception e) {
            throw new RuntimeException(" Failed to init GPIO service: " + e.getMessage(), e);
        }
    }

    public void openLock(Long compartmentId) {
        Integer pin = gpioMap.get(compartmentId);
        if (pin == null) {
            System.err.println(" No GPIO mapping for compartment " + compartmentId);
            return;
        }

        try {
            System.out.println("Unlocking compartment " + compartmentId + " on pin " + pin);
            Runtime.getRuntime().exec("gpioset " + gpioChip + " " + pin + "=0").waitFor();
            Thread.sleep(10000);
            Runtime.getRuntime().exec("gpioset " + gpioChip + " " + pin + "=1").waitFor();
            System.out.println(" Lock pulse complete for pin " + pin);
        } catch (Exception e) {
            System.err.println(" GPIO control error for compartment " + compartmentId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void shutdown() {
        System.out.println("Shutdown: No Pi4J cleanup needed for gpioset");
    }
}
