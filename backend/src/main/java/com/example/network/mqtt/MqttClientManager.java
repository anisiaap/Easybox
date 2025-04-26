// src/main/java/com/example/network/mqtt/MqttClientManager.java
package com.example.network.mqtt;

import com.example.network.dto.CompartmentDto;
import com.example.network.dto.MqttProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.util.retry.Retry;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Component
public class MqttClientManager {

    private final MqttProperties properties;
    private MqttClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MonoSink<List<CompartmentDto>> currentRequestSink;
    private String currentExpectedClientId;

    public MqttClientManager(MqttProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void connect() throws Exception {
        String brokerUrl = "ssl://" + properties.getBrokerUrl() + ":" + properties.getPort();
        System.out.println("🔗 Connecting to MQTT broker: " + brokerUrl);

        // File persistence for QoS1 survive reconnect
        String tmp = System.getProperty("java.io.tmpdir") + "/mqtt-persistence";
        MqttClientPersistence persistence = new MqttDefaultFilePersistence(tmp);
        client = new MqttClient(brokerUrl, properties.getClientId(), persistence);

        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setUserName(properties.getUsername());
        opts.setPassword(properties.getPassword().toCharArray());
        opts.setCleanSession(false);
        opts.setAutomaticReconnect(true);
        opts.setKeepAliveInterval(30);
        opts.setSocketFactory(new SniSslSocketFactory(properties.getBrokerUrl(), properties.getPort()));
        opts.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectionLost(Throwable cause) {
                System.err.println("❌ MQTT lost: " + cause.getMessage());
            }
            @Override
            public void connectComplete(boolean reconnect, String uri) {
                System.out.println((reconnect ? "🔁 Reconnected to " : "✅ Connected to ") + uri);
                try {
                    String sub = properties.getTopicPrefix() + "/response/#";
                    client.subscribe(sub);
                    System.out.println("📡 Subscribed to " + sub);
                } catch (MqttException e) {
                    System.err.println("❌ Subscription error: " + e.getMessage());
                }
            }
            @Override
            public void messageArrived(String topic, MqttMessage msg) throws Exception {
                String payload = new String(msg.getPayload(), StandardCharsets.UTF_8);
                String prefix = properties.getTopicPrefix() + "/response/";
                if (topic.startsWith(prefix) && currentRequestSink != null) {
                    String clientId = topic.substring(prefix.length());
                    if (clientId.equals(currentExpectedClientId)) {
                        List<CompartmentDto> list = Arrays.asList(
                                objectMapper.readValue(payload, CompartmentDto[].class)
                        );
                        currentRequestSink.success(list);
                        currentRequestSink = null;
                        currentExpectedClientId = null;
                    }
                }
            }
            @Override public void deliveryComplete(IMqttDeliveryToken token) {}
        });

        client.connect(opts);
        System.out.println("✅ MQTT connected");
    }

    public Mono<List<CompartmentDto>> requestCompartments(String clientId) {
        return Mono.<List<CompartmentDto>>create(sink -> {
                    if (client == null || !client.isConnected()) {
                        sink.error(new IllegalStateException("MQTT not connected"));
                        return;
                    }
                    this.currentRequestSink = sink;
                    this.currentExpectedClientId = clientId;

                    try {
                        String topic = properties.getTopicPrefix() + "/" + clientId + "/commands";
                        byte[] payload = "{\"type\":\"request-compartments\"}"
                                .getBytes(StandardCharsets.UTF_8);
                        MqttMessage message = new MqttMessage(payload);
                        message.setQos(1);
                        client.publish(topic, message);
                    } catch (MqttException e) {
                        sink.error(e);
                    }
                })
                .timeout(Duration.ofSeconds(10))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(10))
                        .filter(err -> err instanceof RuntimeException || err instanceof MqttException)
                );
    }

    @PreDestroy
    public void disconnect() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            System.out.println("🔌 MQTT disconnected");
        }
    }
}
