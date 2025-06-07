package com.example.easyboxdevice.service;

import com.example.easyboxdevice.model.GpioConfig;
import com.example.easyboxdevice.model.GpioMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfigBuilder;
import com.pi4j.io.gpio.digital.DigitalState;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class GpioService {

    private final Map<Long, DigitalOutput> gpioMap = new HashMap<>();
    private Context pi4j;

    @PostConstruct
    public void init() {
        try {
            pi4j = Pi4J.newAutoContext(); // Auto-detect platform

            InputStream is = getClass().getClassLoader().getResourceAsStream("gpio-config.json");
            if (is == null) throw new RuntimeException("gpio-config.json not found");

            GpioConfig config = new ObjectMapper().readValue(is, GpioConfig.class);

            for (GpioMapping mapping : config.getMappings()) {
                int pin = mapping.getPin();

                DigitalOutputConfigBuilder configBuilder = DigitalOutput.newConfigBuilder(pi4j)
                        .id("pin-" + pin)
                        .name("Compartment " + mapping.getCompartmentId())
                        .address(pin)
                        .shutdown(DigitalState.LOW)
                        .initial(DigitalState.LOW)
                        .provider("pigpio-digital-output"); // or "raspberrypi-digital-output"

                DigitalOutput output = pi4j.create(configBuilder);
                gpioMap.put((long) mapping.getCompartmentId(), output);

                System.out.println("📌 Mapped compartment " + mapping.getCompartmentId() + " to pin " + pin);
            }

        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to init GPIO service: " + e.getMessage(), e);
        }
    }

    public void openLock(Long compartmentId) {
        DigitalOutput output = gpioMap.get(compartmentId);
        if (output != null) {
            System.out.println("🔓 Unlocking compartment " + compartmentId + " via GPIO pin " + output.address());
            try {
                output.high();               // unlock
                Thread.sleep(1000);          // hold open for 1s
                output.low();                // lock
                System.out.println("✅ Lock pulse complete");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("❌ Lock pulse interrupted");
            }
        } else {
            System.err.println("❌ No GPIO mapping found for compartment " + compartmentId);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (pi4j != null) {
            System.out.println("🔌 Shutting down Pi4J");
            pi4j.shutdown();
        }
    }
}
