package com.example.network.mqtt;

import com.example.network.config.MqttProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.stereotype.Component;

@Component
public class MqttClientManager {
    private final MqttProperties properties;
    private MqttClient client;

    public MqttClientManager(MqttProperties properties) {
        this.properties = properties;
    }

    public MqttClient getClient() {
        return client;
    }

    @PostConstruct
    public void connect() throws MqttException {
        String brokerUrl = "ssl://" + properties.getBrokerUrl() + ":" + properties.getPort();
        client = new MqttClient(brokerUrl, properties.getClientId());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(properties.getUsername());
        options.setPassword(properties.getPassword().toCharArray());
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        client.connect(options);
        System.out.println("✅ MQTT connected to broker: " + brokerUrl);
    }

    public void publishCommand(String deviceId, String message) {
        try {
            String topic = properties.getTopicPrefix() + "/" + deviceId + "/commands";
            client.publish(topic, new MqttMessage(message.getBytes()));
            System.out.println("📤 Sent MQTT command to " + topic + ": " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void disconnect() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
        }
    }

    public MqttProperties getProperties() {
        return properties;
    }
}