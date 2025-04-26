package com.example.network.mqtt;

import com.example.network.dto.MqttProperties;
import com.example.network.dto.CompartmentDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Component
public class MqttClientManager {
    private final MqttProperties properties;
    private MqttClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MonoSink<List<CompartmentDto>> currentRequestSink; // Only one active at a time
    private String currentExpectedClientId; // Remember for which device we are waiting

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

        // Subscribe to ALL device responses
        client.subscribe("easybox/response/#");

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                System.err.println("❌ MQTT connection lost: " + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                if (topic.startsWith("device/response/")) {
                    String clientId = topic.substring("device/response/".length());

                    if (currentRequestSink != null && clientId.equals(currentExpectedClientId)) {
                        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                        List<CompartmentDto> compartments = Arrays.asList(objectMapper.readValue(payload, CompartmentDto[].class));
                        currentRequestSink.success(compartments);

                        // Clear sink after success
                        currentRequestSink = null;
                        currentExpectedClientId = null;
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Nothing needed
            }
        });
    }

    public Mono<List<CompartmentDto>> requestCompartments(String clientId) {
        return Mono.create(sink -> {
            try {
                String commandTopic = "easybox/" + clientId + "/commands";
                String payload = "{\"type\":\"request-compartments\"}";

                this.currentRequestSink = sink;
                this.currentExpectedClientId = clientId;

                client.publish(commandTopic, new MqttMessage(payload.getBytes(StandardCharsets.UTF_8)));
                System.out.println("📤 Sent request-compartments to " + commandTopic);

                // Timeout in 5 seconds
                Mono.delay(Duration.ofSeconds(5)).subscribe(timeout -> {
                    if (currentRequestSink != null && clientId.equals(currentExpectedClientId)) {
                        currentRequestSink.error(new RuntimeException("Timeout waiting for compartments response"));
                        currentRequestSink = null;
                        currentExpectedClientId = null;
                    }
                });

            } catch (Exception e) {
                sink.error(e);
            }
        });
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
