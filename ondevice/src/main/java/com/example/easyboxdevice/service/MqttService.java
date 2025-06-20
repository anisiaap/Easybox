package com.example.easyboxdevice.service;

import com.example.easyboxdevice.config.JwtVerifier;
import com.example.easyboxdevice.config.JwtUtil;
import com.example.easyboxdevice.config.SecretStorageUtil;
import com.example.easyboxdevice.controller.DeviceDisplayController;
import com.example.easyboxdevice.controller.LockController;
import com.example.easyboxdevice.dto.MqttProperties;
import com.example.easyboxdevice.dto.QrResponse;
import com.example.easyboxdevice.model.Compartment;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class MqttService {

    private final MqttProperties properties;
    private final CompartmentService compartmentService;
    private final JwtUtil jwtUtil;
    private final JwtVerifier jwtVerifier;

    private MqttClient client;
    private boolean started = false;
    private final DeviceDisplayController display;

    public MqttService(MqttProperties properties,
                       CompartmentService compartmentService,
                       JwtUtil jwtUtil,
                       JwtVerifier jwtVerifier, DeviceDisplayController display) {
        this.properties = properties;
        this.compartmentService = compartmentService;
        this.jwtUtil = jwtUtil;
        this.jwtVerifier = jwtVerifier;
        this.display = display;
    }

    /** ✅ Call this only after device has been approved. Safe to call multiple times. */
    public synchronized void start() throws MqttException {
        if (!SecretStorageUtil.exists()) {
            throw new IllegalStateException("❌ Cannot start MQTT: device not yet approved");
        }

        if (started || client != null && client.isConnected()) {
            return;
        }

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(properties.getUsername());
        options.setPassword(properties.getPassword().toCharArray());
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setKeepAliveInterval(30);

        client = new MqttClient(properties.getBrokerUrl(), properties.getClientId());
        System.out.println("🔗 Connecting to broker URL: " + properties.getBrokerUrl() + " with clientId: " + properties.getClientId());

        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                try {
                    System.out.println((reconnect ? "🔁 Reconnected to " : "✅ Connected to ") + serverURI);
                    String cmdTopic = properties.getTopicPrefix() + "/commands/" + properties.getClientId();
                    client.subscribe(cmdTopic, 1, MqttService.this::handleCommand);
                    System.out.println("📡 Subscribed to topic: " + cmdTopic);

                    String qrRespTopic = properties.getTopicPrefix() + "/qrcode-response/" + properties.getClientId();
                    client.subscribe(qrRespTopic, 1, MqttService.this::handleQrResponse);
                    System.out.println("📡 Subscribed to QR-response topic: " + qrRespTopic);
                    display.showStatus("MQTT connected — ready to scan QR");
                    display.showStatus("Please scan your QR code...");

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
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });

        client.connect(options);
        started = true;
        System.out.println("✅ MQTT client connected and subscriptions registered");
    }

    private void handleCommand(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            System.out.println("📩 Received MQTT command: " + payload);

            if (payload.startsWith("{")) {
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
        display.showLoading();
        try {
            String raw = new String(message.getPayload(), StandardCharsets.UTF_8);
            System.out.println("📥 Received QR-response: " + raw);

            String[] parts = raw.split("::", 2);
            if (parts.length != 2) {
                System.err.println("❌ Invalid QR-response format");
                return;
            }

            String token = parts[0];
            String payload = parts[1];

            String fromClient = jwtVerifier.verifyAndExtractClientId(token);
            if (!fromClient.equals(properties.getClientId())) {
                System.err.println("❌ QR-response token clientId mismatch");
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            QrResponse resp = mapper.readValue(payload, QrResponse.class);

            if ("ok".equalsIgnoreCase(resp.getResult())) {
                display.showStatus("Opening locker.");

                LockController.openLock(resp.getCompartmentId());

                String prompt;
                String confirmCode;

                switch (resp.getNewReservationStatus()) {
                    case "waiting_bakery_drop_off" -> {
                        prompt      = "Did you place the order?";
                        confirmCode = "placed";
                    }
                    case "waiting_client_pick_up" -> {
                        prompt      = "Did you pick up your order?";
                        confirmCode = "picked";
                    }
                    default -> {
                        display.showMessage("⚠️ Reservation in unexpected state");
                        Thread.sleep(3_000);
                        return;
                    }
                }

                if (display.showConfirmationPrompt(prompt)) {
                    sendSimpleResponse("confirm:" + confirmCode + ":" + resp.getCompartmentId());
                }
            } else {
                display.showMessage("Invalid QR: " + resp.getReason());
                Thread.sleep(3_000);
            }


        } catch (Exception e) {
            e.printStackTrace();
            display.showMessage(" Error");
        } finally {
            display.showIdle(); // Return to idle state
        }
    }

    private void sendCompartments() {
        if (!SecretStorageUtil.exists()) {
            System.out.println("⏳ Approval pending — skip sending compartments");
            return;
        }

        try {
            List<Compartment> compartments = compartmentService.getAllCompartments();
            System.out.println("📦 Loaded compartments from database: " + compartments.size());

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(compartments);
            System.out.println("📝 Serialized compartments JSON: " + json);

            String topic = properties.getTopicPrefix() + "/response/" + properties.getClientId();
            String signedPayload = jwtUtil.generateToken(properties.getClientId());

            MqttMessage responseMsg = new MqttMessage((signedPayload + "::" + json).getBytes(StandardCharsets.UTF_8));
            responseMsg.setQos(1);
            responseMsg.setRetained(true);  // ✅ Make response retained
            client.publish(topic, responseMsg);

            System.out.println("📤 Sent compartments response");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendSimpleResponse(String payload) {
        if (!SecretStorageUtil.exists()) {
            System.out.println("⏳ Approval pending — skip response");
            return;
        }

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
        return jwtUtil.generateToken(properties.getClientId()) + "::" + payload;
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                System.out.println("🛑 MQTT disconnected");
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
        if (!SecretStorageUtil.exists()) {
            System.out.println("⏳ Approval pending — skip publish QR");
            return;
        }

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

    public boolean isStarted() {
        return started;
    }
}