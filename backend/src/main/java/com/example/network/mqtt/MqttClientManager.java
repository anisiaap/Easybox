package com.example.network.mqtt;

import com.example.network.config.JwtUtil;
import com.example.network.config.JwtVerifier;
import com.example.network.dto.CompartmentDto;
import com.example.network.dto.MqttProperties;
import com.example.network.service.QrCodeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.netty.http.server.HttpServer;
import reactor.util.retry.Retry;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private final JwtVerifier jwtVerifier;

    private volatile MonoSink<List<CompartmentDto>> currentRequestSink;
    private volatile String currentExpectedClientId;
    private final QrCodeService qrCodeService;
    private final JwtUtil jwtUtil;
    public MqttClientManager(MqttProperties properties, JwtVerifier jwtVerifier, QrCodeService qrCodeService, JwtUtil jwtUtil) {
        this.properties = properties;
        this.jwtVerifier = jwtVerifier;
        this.qrCodeService = qrCodeService;
        this.jwtUtil = jwtUtil;
    }

    @PostConstruct
    public void connect() throws MqttException {
        String brokerUri = properties.getBrokerUrl();
        System.out.println("🔗 Connecting to MQTT broker " + brokerUri);

        client = new MqttClient(
                brokerUri,
                properties.getClientId(),
                new MqttDefaultFilePersistence(System.getProperty("java.io.tmpdir"))
        );

        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setUserName(properties.getUsername());
        opts.setPassword(properties.getPassword().toCharArray());
        opts.setAutomaticReconnect(true);
        opts.setCleanSession(true);
        opts.setKeepAliveInterval(45);

        client.setCallback(callback());
        client.connect(opts);

        System.out.println("✅ MQTT connected");
    }


    @PreDestroy
    public void disconnect() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            System.out.println("🔌 MQTT disconnected");
        }
    }

    public Mono<List<CompartmentDto>> requestCompartments(String clientId) {

        String responseTopic = properties.getTopicPrefix() + "/response/" + clientId;
        System.out.println("📡 Preparing to subscribe to " + responseTopic);


        return Mono.<List<CompartmentDto>>create(sink -> {
                    if (client == null || !client.isConnected()) {
                        sink.error(new IllegalStateException("MQTT not connected"));
                        return;
                    }
                    currentRequestSink      = sink;
                    currentExpectedClientId = clientId;

                    try {
                        // ─── 1️⃣  subscribe to the exact response topic
                        client.subscribe(responseTopic, 1);

                        System.out.println("📡 Subscribed to response topic for clientId: " + clientId);

                        String cmdTopic = properties.getTopicPrefix() + "/commands"+ "/" + clientId;
                        MqttMessage msg = new MqttMessage(
                                "{\"type\":\"request-compartments\"}".getBytes(StandardCharsets.UTF_8));
                        msg.setQos(1);

                        // ─── 2️⃣  publish the command
                        client.publish(cmdTopic, msg);
                        System.out.println("📤 Sent 'request-compartments' command to " + cmdTopic);


                    } catch (MqttException e) {
                        sink.error(e);
                    }
                })
                .timeout(Duration.ofSeconds(10))
                .doFinally(sig -> {
                    // 3️⃣  always unsubscribe and clear state
                    try { client.unsubscribe(responseTopic); } catch (Exception ignored) {}
                    currentRequestSink      = null;
                    currentExpectedClientId = null;
                })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)));
    }


    private MqttCallbackExtended callback() {
        return new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                System.out.println((reconnect ? "🔁 Reconnected to " : "✅ Connected to ") + serverURI);
            }

            @Override
            public void connectionLost(Throwable cause) {
                System.err.println("❌ connectionLost – " + cause);   // prints full class + msg
                if (cause != null) cause.printStackTrace();           // <-- add this line
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                System.out.println("📥 MQTT message arrived on topic: " + topic);
                String qrPrefix = properties.getTopicPrefix() + "/qrcode/";
                if (topic.startsWith(qrPrefix)) {
                    String clientId = topic.substring(qrPrefix.length());

                    String receivedMessage = new String(message.getPayload(), StandardCharsets.UTF_8);
                    String[] parts = receivedMessage.split("::", 2);
                    if (parts.length != 2) {
                        System.err.println("Invalid signed message format.");
                        return;
                    }
                    String token = parts[0];
                    String qrContent = parts[1];

                    // Verify JWT
                    String tokenClientId = jwtVerifier.verifyAndExtractClientId(token);
                    System.out.println("🔑 Token verified, extracted clientId: " + tokenClientId);

                    if (!tokenClientId.equals(clientId)) {
                        System.err.println("Token clientId does not match expected clientId.");
                        return;
                    }
                    qrCodeService.handleQrScan(qrContent)
                            .flatMap(result -> sendQrCodeResponse(clientId, true, result.getCompartmentId(), result.getNewReservationStatus(), null))
                            .onErrorResume(ex -> sendQrCodeResponse(clientId, false, null, null, ex.getMessage()))
                            .subscribe();
                    return; // Important: stop processing further
                }

                String prefix = properties.getTopicPrefix() + "/response/";
                if (topic.startsWith(prefix) && currentRequestSink != null) {
                    String clientId = topic.substring(prefix.length());
                    System.out.println("📥 Matched expected clientId: " + clientId);

                    if (clientId.equals(currentExpectedClientId)) {
                        String receivedMessage = new String(message.getPayload(), StandardCharsets.UTF_8);
                        // 🔥 Split token and payload
                        System.out.println("📝 Raw message received, length = " + receivedMessage.length());

                        String[] parts = receivedMessage.split("::", 2);
                        if (parts.length != 2) {
                            System.err.println("Invalid signed message format.");
                            return;
                        }
                        String token = parts[0];
                        String payload = parts[1];
                        try {
                            // 🔥 Verify token
                            String tokenClientId = jwtVerifier.verifyAndExtractClientId(token);
                            System.out.println("🔑 Token verified, extracted clientId: " + tokenClientId);

                            if (!tokenClientId.equals(clientId)) {

                                System.err.println("Token clientId does not match expected clientId.");
                                return;
                            }
                        } catch (Exception e) {
                            System.err.println("JWT verification failed: " + e.getMessage());
                            return;
                        }
                        List<CompartmentDto> list = Arrays.asList(
                                mapper.readValue(payload, CompartmentDto[].class)
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
    ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void startConnectionMonitor() {
        monitor.scheduleAtFixedRate(() -> {
            if (client != null) {
                System.out.println(client.isConnected() ? "✅ MQTT connection healthy" : "❌ MQTT disconnected");
            }
        }, 0, 30, TimeUnit.SECONDS);  // every 30 seconds
    }
    private Mono<Void> sendQrCodeResponse(String clientId, boolean success,
                                          Long compartmentId, String newStatus, String errorReason) {
        try {
            String responseTopic = properties.getTopicPrefix() + "/qrcode-response/" + clientId;

            String payload;
            if (success) {
                payload = "{\"result\":\"ok\",\"compartmentId\":" + compartmentId + ",\"newReservationStatus\":\"" + newStatus + "\"}";
            } else {
                payload = "{\"result\":\"error\",\"reason\":\"" + errorReason + "\"}";
            }

            String signedPayload = jwtUtil.generateToken(clientId) + "::" + payload;
            MqttMessage message = new MqttMessage(signedPayload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            client.publish(responseTopic, message);

            System.out.println("📤 Sent QR response to " + responseTopic + ": " + payload);

            return Mono.empty();
        } catch (Exception e) {
            System.err.println("❌ Failed to send QR response: " + e.getMessage());
            return Mono.empty();
        }
    }

}
