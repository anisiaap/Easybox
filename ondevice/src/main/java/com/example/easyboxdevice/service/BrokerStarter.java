package com.example.easyboxdevice.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BrokerStarter {

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        // Do nothing — MQTT will start only after registration confirms approval
        System.out.println("⏳ Waiting for device approval before MQTT startup...");
    }
}