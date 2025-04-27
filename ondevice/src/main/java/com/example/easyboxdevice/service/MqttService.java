// MqttService.java
package com.example.easyboxdevice.service;

import com.example.easyboxdevice.dto.MqttProperties;
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
                        String commandTopic = properties.getTopicPrefix() + "/" + properties.getClientId() + "/commands";
                       client.subscribe(commandTopic, MqttService.this::handleCommand);
                        System.out.println("📡 Subscribed to topic: " + commandTopic);
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
                    handleCommand(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) { }
            });

            client.connect(options);
//            Thread.sleep(2000);  // 💬 Give some time
//            System.out.println("✅ Client connected? " + client.isConnected());
            String cmdTopic = properties.getTopicPrefix()  + "/commands" + "/" + properties.getClientId();
            client.subscribe(cmdTopic, this::handleCommand);
            System.out.println("📡 Subscribed to topic: " + cmdTopic);

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
    private void sendCompartments() {
        try {
            List<Compartment> compartments = compartmentService.getAllCompartments();
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(compartments);

            String topic = properties.getTopicPrefix() + "/response/" + properties.getClientId();
            String signedPayload = createSignedPayload(json);
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


    public void publishEvent(String subTopic, String payload) {
        try {
            String topic = properties.getTopicPrefix() + "/" + properties.getClientId() + "/" + subTopic;

            String token = jwtUtil.generateToken(properties.getClientId());
            String signedPayload = token + "::" + payload;
            MqttMessage msg = new MqttMessage(signedPayload.getBytes());
            msg.setQos(1); // at-least-once
            client.publish(topic, msg);
            System.out.println("📤 Published to " + topic + ": " + payload);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String createSignedPayload(String payload) {
        String token = jwtUtil.generateToken(properties.getClientId());
        return token + "::" + payload;
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
    private static class Command {
        private String type;
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

}

