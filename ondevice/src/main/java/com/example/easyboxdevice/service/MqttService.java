// MqttService.java
package com.example.easyboxdevice.service;

import com.example.easyboxdevice.config.MqttProperties;
import com.example.easyboxdevice.service.CompartmentService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.stereotype.Service;

@Service
public class MqttService {

    private final MqttProperties properties;
    private final CompartmentService compartmentService;
    private MqttClient client;

    public MqttService(MqttProperties properties, CompartmentService compartmentService) {
        this.properties = properties;
        this.compartmentService = compartmentService;
    }

    @PostConstruct
    public void connect() {
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(properties.getUsername());
            options.setPassword(properties.getPassword().toCharArray());
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            client = new MqttClient(properties.getBrokerUrl(), properties.getClientId());
            client.connect(options);

            String commandTopic = properties.getTopicPrefix() + "/" + properties.getClientId() + "/commands";
            client.subscribe(commandTopic, this::handleCommand);

            System.out.println("✅ MQTT connected and subscribed to " + commandTopic);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleCommand(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload());
            System.out.println("📩 Received MQTT command: " + payload);

            if (payload.startsWith("reserve:")) {
                Long compId = Long.parseLong(payload.split(":")[1]);
                boolean ok = compartmentService.reserveCompartment(compId);
                sendResponse("reserve-result:" + compId + ":" + (ok ? "ok" : "fail"));
            } else if (payload.startsWith("clean:")) {
                Long compId = Long.parseLong(payload.split(":")[1]);
                boolean ok = compartmentService.cleanCompartment(compId);
                sendResponse("clean-result:" + compId + ":" + (ok ? "ok" : "fail"));
            } else if (payload.equals("ping")) {
                sendResponse("pong");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void publishEvent(String subTopic, String payload) {
        try {
            String topic = properties.getTopicPrefix() + "/" + properties.getClientId() + "/" + subTopic;

            String token = JwtUtil.generateToken(properties.getClientId());
            String signedPayload = token + "::" + payload;
            MqttMessage msg = new MqttMessage(signedPayload.getBytes());
            msg.setQos(1); // at-least-once
            client.publish(topic, msg);
            System.out.println("📤 Published to " + topic + ": " + payload);
        } catch (Exception e) {
            e.printStackTrace();
    }
}


    private void sendResponse(String payload) {
        try {
            String topic = properties.getTopicPrefix() + "/" + properties.getClientId() + "/response";
            client.publish(topic, new MqttMessage(payload.getBytes()));
            System.out.println("📤 Sent response: " + payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
