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

import javax.net.ssl.SSLSocketFactory;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Component
public class MqttClientManager {
    private final MqttProperties properties;
    private MqttClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MonoSink<List<CompartmentDto>> currentRequestSink; // Only one active request
    private String currentExpectedClientId;

    public MqttClientManager(MqttProperties properties) {
        this.properties = properties;
    }

    public MqttClient getClient() {
        return client;
    }

    @PostConstruct
    public void connect() throws Exception {
        String brokerUrl = "ssl://" + properties.getBrokerUrl() + ":" + properties.getPort();
        System.out.println("🔗 Connecting to broker: " + brokerUrl + " with clientId: " + properties.getClientId());

        client = new MqttClient(brokerUrl, properties.getClientId());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(properties.getUsername());
        options.setPassword(properties.getPassword().toCharArray());
        options.setCleanSession(false);   // Persistent session
        options.setAutomaticReconnect(true);
        options.setKeepAliveInterval(30);
        options.setSocketFactory(new SniSslSocketFactory(properties.getBrokerUrl(), properties.getPort()));
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectionLost(Throwable cause) {
                System.err.println("❌ MQTT connection lost: " + cause.getMessage());
                cause.printStackTrace();
            }

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                System.out.println((reconnect ? "🔁 Reconnected to " : "✅ Connected to ") + serverURI);
                try {
                    String subscribeTopic = properties.getTopicPrefix() + "/response/#";
                    client.subscribe(subscribeTopic);
                    System.out.println("📡 Subscribed to topic: " + subscribeTopic);
                } catch (MqttException e) {
                    System.err.println("❌ Failed to subscribe: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                System.out.println("📥 Incoming message on topic: " + topic);
                String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                System.out.println("📥 Payload: " + payload);

                if (topic.startsWith("easybox/response/")) {
                    String clientId = topic.substring("easybox/response/".length());
                    System.out.println("📥 Response from clientId: " + clientId);
                    if (currentRequestSink != null && clientId.equals(currentExpectedClientId)) {
                        List<CompartmentDto> compartments = Arrays.asList(objectMapper.readValue(payload, CompartmentDto[].class));
                        currentRequestSink.success(compartments);

                        // Clear sink after success
                        currentRequestSink = null;
                        currentExpectedClientId = null;
                        System.out.println("✅ Successfully processed response for clientId: " + clientId);
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Nothing needed
            }
        });

        client.connect(options);
        System.out.println("✅ MQTT connected successfully to broker: " + brokerUrl);
    }

    public Mono<List<CompartmentDto>> requestCompartments(String clientId) {
        return Mono.create(sink -> {
            try {
                if (client == null || !client.isConnected()) {
                    sink.error(new IllegalStateException("MQTT client not connected"));
                    return;
                }

                String commandTopic = "easybox/" + clientId + "/commands";
                String payload = "{\"type\":\"request-compartments\"}";

                System.out.println("📤 Publishing request-compartments to topic: " + commandTopic);
                System.out.println("📤 Payload: " + payload);

                this.currentRequestSink = sink;
                this.currentExpectedClientId = clientId;

                MqttMessage mqttMessage = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
                mqttMessage.setQos(1); // At-least-once delivery
                client.publish(commandTopic, mqttMessage);

                // Timeout after 10 seconds if no response
                Mono.delay(Duration.ofSeconds(10)).subscribe(timeout -> {
                    if (currentRequestSink != null && clientId.equals(currentExpectedClientId)) {
                        System.err.println("⏰ Timeout waiting for compartments response from clientId: " + clientId);
                        currentRequestSink.error(new RuntimeException("Timeout waiting for compartments response"));
                        currentRequestSink = null;
                        currentExpectedClientId = null;
                    }
                });

            } catch (Exception e) {
                System.err.println("❌ Failed to send request-compartments: " + e.getMessage());
                sink.error(e);
            }
        });
    }

    @PreDestroy
    public void disconnect() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            System.out.println("🔌 MQTT disconnected from broker.");
        }
    }

    public MqttProperties getProperties() {
        return properties;
    }
}
