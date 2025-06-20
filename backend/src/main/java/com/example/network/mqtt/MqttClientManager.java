package com.example.network.mqtt;

import com.example.network.config.JwtUtil;
import com.example.network.config.JwtVerifier;
import com.example.network.dto.CompartmentDto;
import com.example.network.dto.MqttProperties;
import com.example.network.model.Easybox;
import com.example.network.repository.EasyboxRepository;
import com.example.network.service.QrCodeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    private final EasyboxRepository easyboxRepository;
    private volatile MonoSink<List<CompartmentDto>> currentRequestSink;
    private volatile String currentExpectedClientId;
    private final QrCodeService qrCodeService;
    private final JwtUtil jwtUtil;
    public MqttClientManager(MqttProperties properties, JwtVerifier jwtVerifier, EasyboxRepository easyboxRepository, QrCodeService qrCodeService, JwtUtil jwtUtil) {
        this.properties = properties;
        this.jwtVerifier = jwtVerifier;
        this.easyboxRepository = easyboxRepository;
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

            currentRequestSink = sink;
            currentExpectedClientId = clientId;

            try {
                client.subscribe(responseTopic, 1);
                System.out.println("📡 Subscribed to response topic for clientId: " + clientId);

                String cmdTopic = properties.getTopicPrefix() + "/commands/" + clientId;
                MqttMessage msg = new MqttMessage(
                        "{\"type\":\"request-compartments\"}".getBytes(StandardCharsets.UTF_8)
                );
                msg.setQos(1);
                msg.setRetained(true);
                client.publish(cmdTopic, msg);
                System.out.println("📤 Sent 'request-compartments' command to " + cmdTopic);
            } catch (MqttException e) {
                sink.error(e);
            }
        }).timeout(Duration.ofSeconds(30)) // ⏱️ Reactor handles timeout for you
                .doOnError(TimeoutException.class, ex -> {
                    System.err.println("⏰ Timeout waiting for response from clientId: " + clientId);
                    cleanup(responseTopic);
                });
    }

    private void cleanup( String responseTopic) {
        try {
            client.unsubscribe(responseTopic);
            System.out.println("🚫 Unsubscribed from " + responseTopic);
        } catch (Exception ignored) {}
        currentRequestSink = null;
        currentExpectedClientId = null;
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
            public void messageArrived(String topic, MqttMessage message) {
                System.out.println("📥 MQTT message arrived on topic: " + topic);

                // One decode, visible everywhere
                String raw = new String(message.getPayload(), StandardCharsets.UTF_8);

                /* ─────────────────────────────  A.  QR-SCAN PATH  ───────────────────────────── */
                String qrPrefix = properties.getTopicPrefix() + "/qrcode/";
                if (topic.startsWith(qrPrefix)) {
                    String clientId = topic.substring(qrPrefix.length());

                    String[] parts = raw.split("::", 2);
                    if (parts.length != 2) {
                        System.err.println("Invalid signed message format");
                        return;
                    }
                    String token     = parts[0];
                    String qrContent = parts[1];

                    jwtVerifier.verifyWithPerDeviceSecretOnly(token)
                            .flatMap(cid -> {
                                if (!cid.equals(clientId)) {
                                    System.err.println("Token clientId mismatch");
                                    return Mono.empty();
                                }
                                return qrCodeService.handleQrScan(qrContent)
                                        .flatMap(r -> sendQrCodeResponse(clientId, true,
                                                r.getCompartmentId(),
                                                r.getNewReservationStatus(),
                                                null))
                                        .onErrorResume(ex ->
                                                sendQrCodeResponse(clientId, false, null, null, ex.getMessage()));
                            })
                            .subscribe();

                    return;                 // done with /qrcode/**
                }

                /* ─────────────────────────────  B.  RESPONSE PATH  ───────────────────────────── */
                String respPrefix = properties.getTopicPrefix() + "/response/";
                if (topic.startsWith(respPrefix)) {
                    String clientId = topic.substring(respPrefix.length());

                    String[] parts = raw.split("::", 2);
                    if (parts.length != 2) {
                        System.err.println("Invalid signed message format");
                        return;
                    }
                    String token   = parts[0];
                    String payload = parts[1];

                    jwtVerifier.verifyWithPerDeviceSecretOnly(token)
                            .flatMap(cid -> {
                                if (!cid.equals(clientId)) {
                                    System.err.println("Token clientId mismatch");
                                    return Mono.empty();
                                }

                                /* ---- 1. confirmations (placed / picked) ---- */
                                if (payload.startsWith("confirm:placed:") ||
                                        payload.startsWith("confirm:picked:")) {

                                    Long id = Long.parseLong(payload.split(":")[2]);
                                    return qrCodeService.handleConfirmation(id);
                                }

                                /* ---- 2. compartment list response ---- */
                                if (currentRequestSink != null && clientId.equals(currentExpectedClientId)) {
                                    try {
                                        List<CompartmentDto> list = Arrays.asList(
                                                mapper.readValue(payload, CompartmentDto[].class)
                                        );
                                        currentRequestSink.success(list);
                                        currentRequestSink      = null;
                                        currentExpectedClientId = null;
                                        try {
                                            MqttMessage empty = new MqttMessage(new byte[0]);
                                            empty.setQos(1);
                                            empty.setRetained(true);
                                            client.publish(topic, empty); // topic is the one we just received from
                                            System.out.println("🧹 Cleared retained response on " + topic);
                                        } catch (Exception ex) {
                                            System.err.println("⚠️ Failed to clear retained message: " + ex.getMessage());
                                        }

                                    } catch (Exception ex) {
                                        System.err.println("❌ Failed to parse compartments: " + ex.getMessage());
                                    }
                                }
                                return Mono.empty();
                            })
                            .subscribe();

                    return;                 // done with /response/**
                }

                /* ─────────────────────────────  C.  OTHER TOPICS (ignored)  ─────────────────── */
                System.out.println("ℹ️  Unhandled topic: " + topic);
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

            Easybox box = easyboxRepository.findByClientId(clientId).block();
            if (box == null || box.getSecretKey() == null) {
                throw new IllegalStateException("Device not found or secret missing");
            }
            String token = jwtUtil.generateToken(clientId, box.getSecretKey());
            String signedPayload = token + "::" + payload;
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