// MqttService.java
package com.example.easyboxdevice.service;

import com.example.easyboxdevice.dto.MqttProperties;
import com.example.easyboxdevice.dto.QrResponse;
import com.example.easyboxdevice.entity.Compartment;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.stereotype.Service;
import com.example.easyboxdevice.config.JwtUtil;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class MqttService {

    private final MqttProperties properties;
    private final CompartmentService compartmentService;
    private MqttClient client;
    private final JwtUtil jwtUtil;

    public MqttService(MqttProperties properties, CompartmentService compartmentService, JwtUtil jwtUtil) {
        this.properties = properties;
        this.compartmentService = compartmentService;
        this.jwtUtil = jwtUtil;
    }

    @PostConstruct
    public void connect() {
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(properties.getUsername());
            options.setPassword(properties.getPassword().toCharArray());
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);  // Keep session alive
            options.setKeepAliveInterval(30);

            client = new MqttClient(properties.getBrokerUrl(), properties.getClientId());
            System.out.println("🔗 Connecting to broker URL: " + properties.getBrokerUrl() + " with clientId: " + properties.getClientId());

            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    try {
                        System.out.println((reconnect ? "🔁 Reconnected to " : "✅ Connected to ") + serverURI);
                        String cmdTopic = properties.getTopicPrefix()  + "/commands" + "/" + properties.getClientId();
                        client.subscribe(cmdTopic, 1,MqttService.this::handleCommand);
                        System.out.println("📡 Subscribed to topic: " + cmdTopic);
                        String qrRespTopic = properties.getTopicPrefix()
                                + "/qrcode-response/"
                                + properties.getClientId();
                        client.subscribe(qrRespTopic,1, MqttService.this::handleQrResponse);
                        System.out.println("📡 Subscribed to QR-response topic: " + qrRespTopic);

                    } catch (Exception e) {
                        System.err.println("❌ Failed to subscribe after connect: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("❌ MQTT connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println("📥 Incoming message on topic: " + topic);
                    if (topic.endsWith("/qrcode-response/" + properties.getClientId())) {
                        handleQrResponse(topic, message);
                    } else {
                        handleCommand(topic, message);
                    }
                }


                @Override
                public void deliveryComplete(IMqttDeliveryToken token) { }
            });

            client.connect(options);

            System.out.println("✅ Client connected");

        } catch (Exception e) {
            System.err.println("❌ Connect failed: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private void handleCommand(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            System.out.println("📩 Received MQTT command: " + payload);

            if (payload.startsWith("{")) {
                // Handle JSON commands
                ObjectMapper mapper = new ObjectMapper();
                Command cmd = mapper.readValue(payload, Command.class);

                if ("request-compartments".equals(cmd.getType())) {
                    sendCompartments();
                }
            } else if (payload.startsWith("reserve:")) {
                Long compId = Long.parseLong(payload.split(":")[1]);
                boolean ok = compartmentService.reserveCompartment(compId);
                sendSimpleResponse("reserve-result:" + compId + ":" + (ok ? "ok" : "fail"));
            } else if (payload.startsWith("clean:")) {
                Long compId = Long.parseLong(payload.split(":")[1]);
                boolean ok = compartmentService.cleanCompartment(compId);
                sendSimpleResponse("clean-result:" + compId + ":" + (ok ? "ok" : "fail"));
            } else if (payload.equals("ping")) {
                sendSimpleResponse("pong");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void handleQrResponse(String topic, MqttMessage message) {
        try {
            String raw = new String(message.getPayload(), StandardCharsets.UTF_8);
            System.out.println("📥 Received QR-response: " + raw);

            // ── split off the JWT ──
            String[] parts = raw.split("::", 2);
            if (parts.length != 2) {
                System.err.println("❌ Invalid QR-response format");
                return;
            }
            String token   = parts[0];
            String payload = parts[1];

//            // ── verify it (requires JwtVerifier) ──
//            String fromClient = jwtVerifier.verifyAndExtractClientId(token);
//            if (!fromClient.equals(properties.getClientId())) {
//                System.err.println("❌ QR-response token clientId mismatch");
//                return;
//            }

            // ── parse JSON body ──
            ObjectMapper mapper = new ObjectMapper();
            QrResponse resp = mapper.readValue(payload, QrResponse.class);

            if ("ok".equalsIgnoreCase(resp.getResult())) {
                System.out.println("✅ QR handled: compartment="
                        + resp.getCompartmentId()
                        + ", newStatus="
                        + resp.getNewReservationStatus());
                // → trigger your device action here (LED, motor, display…)
            } else {
                System.err.println("⚠️ QR error: " + resp.getReason());
                // → maybe blink red, show error message…
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void sendCompartments() {
        try {
            List<Compartment> compartments = compartmentService.getAllCompartments();
            System.out.println("📦 Loaded compartments from database: " + compartments.size());

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(compartments);
            System.out.println("📝 Serialized compartments JSON: " + json);

            String topic = properties.getTopicPrefix() + "/response/" + properties.getClientId();
            String signedPayload = createSignedPayload(json);
            System.out.println("🔏 Created signed payload, length = " + signedPayload.length());

            Thread.sleep(1000);
            client.publish(topic, new MqttMessage(signedPayload.getBytes(StandardCharsets.UTF_8)));

            System.out.println("📤 Sent compartments response: " + json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendSimpleResponse(String payload) {
        try {
            String topic = properties.getTopicPrefix() + "/response/" + properties.getClientId();
            String signedPayload = createSignedPayload(payload);
            client.publish(topic, new MqttMessage(signedPayload.getBytes(StandardCharsets.UTF_8)));
            System.out.println("📤 Sent simple response: " + payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String createSignedPayload(String payload) {
        String token = jwtUtil.generateToken(properties.getClientId());
        return token + "::" + payload;
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
    private static class Command {
        private String type;
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
    public void publishQr(String qrValue) {
        try {
            String topic = properties.getTopicPrefix() + "/qrcode/" + properties.getClientId();
            String signedPayload = createSignedPayload("{\"qr\":\"" + qrValue + "\"}");
            MqttMessage msg = new MqttMessage(signedPayload.getBytes(StandardCharsets.UTF_8));
            msg.setQos(1);
            client.publish(topic, msg);
            System.out.println("📤 Published QR scan: " + qrValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}

