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

/**
 * Minimal, battle‑tested MQTT manager that works with HiveMQ Cloud (TLS + SNI).
 * <p>
 * No custom {@code SocketFactory}, no exotic options – just the defaults that
 * work out‑of‑the‑box with JDK 11+ (SNI is automatic when you use a host‑name URI).
 */
@Component
public class MqttClientManager {

    private final MqttProperties properties;
    private MqttClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    private volatile MonoSink<List<CompartmentDto>> currentRequestSink;
    private volatile String currentExpectedClientId;

    public MqttClientManager(MqttProperties properties) {
        this.properties = properties;
    }

    /* ----------------------------------------------------------------
     *  Lifecycle
     * -------------------------------------------------------------- */
    @PostConstruct
    public void connect() throws MqttException {
        String brokerUri = "ssl://" + properties.getBrokerUrl() + ":" + properties.getPort();
        System.out.println("🔗 Connecting to MQTT broker " + brokerUri);

        client = new MqttClient(
                brokerUri,
                properties.getClientId(),
                new MqttDefaultFilePersistence(System.getProperty("java.io.tmpdir"))
        );

        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setUserName(properties.getUsername());
        opts.setPassword(properties.getPassword().toCharArray());
        opts.setAutomaticReconnect(true);          // let Paho handle reconnect
        opts.setCleanSession(true);                // simpler – no sticky session state
        opts.setKeepAliveInterval(45);             // < 60 s default timeout on HiveMQ Cloud

        client.setCallback(callback());
        client.connect(opts);                      // blocks until CONNACK
        System.out.println("✅ MQTT connected");
    }

    @PreDestroy
    public void disconnect() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            System.out.println("🔌 MQTT disconnected");
        }
    }

    /* ----------------------------------------------------------------
     *  Public API
     * -------------------------------------------------------------- */
    public Mono<List<CompartmentDto>> requestCompartments(String clientId) {
        return Mono.<List<CompartmentDto>>create(sink -> {
                    if (client == null || !client.isConnected()) {
                        sink.error(new IllegalStateException("MQTT not connected"));
                        return;
                    }
                    currentRequestSink = sink;
                    currentExpectedClientId = clientId;
                    try {
                        String topic = properties.getTopicPrefix() + "/" + clientId + "/commands";
                        MqttMessage msg = new MqttMessage("{\"type\":\"request-compartments\"}".getBytes(StandardCharsets.UTF_8));
                        msg.setQos(1);
                        client.publish(topic, msg);
                    } catch (MqttException e) {
                        sink.error(e);
                    }
                })
                .timeout(Duration.ofSeconds(10))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))) // 3 tries total
                ;
    }

    /* ----------------------------------------------------------------
     *  Internal helpers
     * -------------------------------------------------------------- */
    private MqttCallbackExtended callback() {
        return new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                System.out.println((reconnect ? "🔁 Reconnected to " : "✅ Connected to ") + serverURI);
                try {
                    String sub = properties.getTopicPrefix() + "/response/#";
                    client.subscribe(sub, 1);
                    System.out.println("📡 Subscribed to " + sub);
                } catch (MqttException e) {
                    System.err.println("❌ Subscription error: " + e.getMessage());
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                System.err.println("❌ connectionLost – " + (cause != null ? cause.getMessage() : "unknown"));
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String prefix = properties.getTopicPrefix() + "/response/";
                if (topic.startsWith(prefix) && currentRequestSink != null) {
                    String clientId = topic.substring(prefix.length());
                    if (clientId.equals(currentExpectedClientId)) {
                        List<CompartmentDto> list = Arrays.asList(
                                mapper.readValue(message.getPayload(), CompartmentDto[].class)
                        );
                        currentRequestSink.success(list);
                        currentRequestSink = null;
                        currentExpectedClientId = null;
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // no‑op
            }
        };
    }
}
