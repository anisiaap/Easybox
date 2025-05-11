package com.example.easyboxdevice.service;

import com.example.easyboxdevice.config.JwtUtil;
import com.example.easyboxdevice.config.SecretStorageUtil;
import com.example.easyboxdevice.dto.RegistrationRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class DeviceRegistrationService {

    @Value("${central.backend.url}")
    private String centralBackendUrl;

    @Value("${device.address}")
    private String deviceAddress;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${jwt.device-secret}")
    private String fallbackSecret;

    private final JwtUtil jwtUtil;
    private final MqttService mqttService;  // 👈 injected
    private final WebClient webClient;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public DeviceRegistrationService(
            JwtUtil jwtUtil,
            MqttService mqttService,
            WebClient.Builder webClientBuilder
    ) {
        this.jwtUtil = jwtUtil;
        this.mqttService = mqttService;
        this.webClient = webClientBuilder.build();
    }

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(
                () -> attemptRegistration()
                        .onErrorResume(e -> Mono.empty())   // swallow, retry next tick
                        .subscribe(),
                0, 10, TimeUnit.MINUTES);
    }

    private Mono<Void> attemptRegistration() {
        RegistrationRequest req = new RegistrationRequest();
        req.setAddress(deviceAddress);
        req.setClientId(clientId);
        req.setStatus("active");

        String token = createToken();

        return webClient.post()
                .uri(centralBackendUrl + "device/register")
                .header("Authorization", "Bearer " + token)
                .bodyValue(req)
                .retrieve()
                .toEntity(Map.class)  // 👈 we read approval from body JSON: { approved: true/false }
                .flatMap(resp -> {
                    Map<String, Object> body = resp.getBody();
                    String newSecret = resp.getHeaders().getFirst("X-Device-Secret");
                    boolean isApproved = Boolean.TRUE.equals(body.get("approved"));

                    if (isApproved && newSecret != null && !newSecret.isBlank() && !SecretStorageUtil.exists()) {
                        try {
                            SecretStorageUtil.storeSecret(newSecret);
                            mqttService.start();         // 👈 connect to MQTT *only* after approval
                            scheduler.shutdown();        // 👈 stop polling
                            System.out.println("🔐 Device approved — secret saved and broker started");
                        } catch (Exception e) {
                            System.err.println("❌ Failed to store device secret: " + e.getMessage());
                        }
                    }

                    return Mono.empty();
                });
    }

    private String createToken() {
        if (SecretStorageUtil.exists()) {
            return jwtUtil.generateToken(clientId);
        }

        // First boot — use fallback secret
        return Jwts.builder()
                .setSubject(clientId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 10 * 60_000))
                .signWith(SignatureAlgorithm.HS256, fallbackSecret.getBytes())
                .compact();
    }

    @PreDestroy
    public void shutdown() {
        System.out.println("Device is shutting down. Initiating deregistration...");
        String token = createToken();
        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }

        RegistrationRequest req = new RegistrationRequest();
        req.setAddress(deviceAddress);
        req.setClientId(clientId);
        req.setStatus("inactive");

        try {
            String response = webClient.post()
                    .uri(centralBackendUrl + "device/register")
                    .header("Authorization", "Bearer " + token)
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            System.out.println("Deregistration successful: " + response);
        } catch (Exception e) {
            System.err.println("Deregistration failed: " + e.getMessage());
        }
    }
}