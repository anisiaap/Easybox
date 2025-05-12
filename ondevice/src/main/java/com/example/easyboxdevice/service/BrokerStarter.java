package com.example.easyboxdevice.service;

import com.example.easyboxdevice.config.SecretStorageUtil;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BrokerStarter {

    private final MqttService mqttService;

    public BrokerStarter(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (SecretStorageUtil.exists()) {
            try {
                SecretStorageUtil.loadSecret(); // ensure it's valid
                mqttService.start();
                System.out.println("✅ MQTT broker started at boot");
            } catch (Exception e) {
                System.err.println("❌ Invalid stored secret, skipping MQTT startup");
            }
        }
    }
}