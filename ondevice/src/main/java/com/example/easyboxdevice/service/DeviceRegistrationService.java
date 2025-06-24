package com.example.easyboxdevice.service;

import com.example.easyboxdevice.config.JwtUtil;
import com.example.easyboxdevice.config.SecretStorageUtil;
import com.example.easyboxdevice.controller.DeviceDisplayController;
import com.example.easyboxdevice.dto.RegistrationRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
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
    private volatile boolean mqttStarted = false;
    private final JwtUtil jwtUtil;
    private final MqttService mqttService;  // injected
    private final WebClient webClient;
    private final DeviceDisplayController display;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public DeviceRegistrationService(
            JwtUtil jwtUtil,
            MqttService mqttService,
            WebClient.Builder webClientBuilder, DeviceDisplayController display
    ) {
        this.jwtUtil = jwtUtil;
        this.mqttService = mqttService;
        this.webClient = webClientBuilder.build();
        this.display = display;
    }

    @PostConstruct
    public void init() {
        keepTryingUntilApproved();
    }
    private void keepTryingUntilApproved() {
        attemptRegistration()
                .flatMap(approved -> {
                    if (approved) {
                        System.out.println(" Registration response received: approved=" + approved);
                        display.showStatus("Device approved - waiting for MQTT to start...");
                         display.showStatus("MQTT connected — ready to scan QR");
                    display.showStatus("Please scan your QR code...");
                        return Mono.delay(Duration.ofMinutes(30)).then(Mono.fromRunnable(this::keepTryingUntilApproved));
                    } else {
                        return Mono.delay(Duration.ofMinutes(1)).then(Mono.fromRunnable(this::keepTryingUntilApproved));
                    }
                })
                .onErrorResume(e -> {
                    System.err.println("Registration error: " + e.getMessage());
                    return Mono.delay(Duration.ofMinutes(1))
                            .then(Mono.fromRunnable(this::keepTryingUntilApproved));
                })
                .subscribe();
    }
    private Mono<Boolean> attemptRegistration() {
        RegistrationRequest req = new RegistrationRequest();
        req.setAddress(deviceAddress);
        req.setClientId(clientId);
        req.setStatus(SecretStorageUtil.exists() ? "active" : "in_approval");

        String token = createToken();
        display.showStatus("Connecting to server..."); // before webClient.post


        return webClient.post()
                .uri(centralBackendUrl + "device/register")
                .header("Authorization", "Bearer " + token)
                .bodyValue(req)
                .retrieve()
                .onStatus(status -> status.value() == 403 || status.value() == 400, resp ->
                        resp.bodyToMono(String.class).flatMap(errorBody -> {
                            System.err.println("Server returned " + resp.statusCode().value() + ": " + errorBody);

                            if (!SecretStorageUtil.exists()) {
                                // This means we tried to register with fallback, got rejected, but MAY be approved already
                                System.err.println(" No stored secret — trying to fetch assigned secret...");
                                return fetchNewSecretAndRetry().flatMap(approved -> {
                                    if (approved) return Mono.empty();
                                    return Mono.error(new SecurityException("Secret fetch failed or device not approved"));
                                });
                            }

                            System.err.println("JWT rejected with stored secret — possibly rotated, trying refresh...");
                            return fetchNewSecretAndRetry().flatMap(approved -> {
                                if (approved) return Mono.empty();
                                return Mono.error(new SecurityException("Secret refresh failed"));
                            });
                        })
                )
                .toEntity(Map.class)
                .flatMap(resp -> {
                    Map<String, Object> body = resp.getBody();
                    String newSecret = resp.getHeaders().getFirst("X-Device-Secret");
                    boolean isApproved = Boolean.TRUE.equals(body.get("approved"));

                    if (isApproved && newSecret != null && !newSecret.isBlank() && !SecretStorageUtil.exists()) {
                        try {
                            SecretStorageUtil.storeSecret(newSecret);
                            System.out.println("Approved secret stored");
                        } catch (Exception e) {
                            System.err.println("Failed to store device secret: " + e.getMessage());
                        }
                    }

                    if (isApproved && !mqttStarted) {
                        try {
                            mqttStarted = true;
                            mqttService.start();
                            System.out.println("Device approved — MQTT started");
                        } catch (MqttException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("Device approved — MQTT started");
                        display.showStatus("Device approved — connecting to MQTT...");
                    } else {
                        System.out.println(" Still waiting for admin approval…");
                        display.showStatus("Awaiting approval...");
                        //  If we somehow have a stored secret, delete it to force fallback usage
//                        if (SecretStorageUtil.exists()) {
//                            try {
//                                SecretStorageUtil.deleteSecret();  // implement this method
//                                System.out.println("🧹 Removed secret since device is not yet approved");
//                            } catch (Exception e) {
//                                System.err.println("❌ Failed to delete stale secret: " + e.getMessage());
//                            }
//                        }
                    }

                    return Mono.just(isApproved);
                });
    }

    private String createToken() {
        // Only use stored secret if we're approved
        if (SecretStorageUtil.exists()) {
            try {
                // attempt to load and validate it
                String secret = SecretStorageUtil.loadSecret();
                return Jwts.builder()
                        .setSubject(clientId)
                        .setIssuedAt(new Date())
                        .setExpiration(new Date(System.currentTimeMillis() + 10 * 60_000))
                        .signWith(SignatureAlgorithm.HS256, secret.getBytes())
                        .compact();
            } catch (Exception e) {
                System.err.println("⚠️ Stored secret unusable, fallback to general key");
            }
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
    private Mono<Boolean> fetchNewSecretAndRetry() {
        String fallbackToken = createToken();  // fallback is safe to use here
        return webClient.get()
                .uri(centralBackendUrl + "device/" + clientId + "/secret")
                .header("Authorization", "Bearer " + fallbackToken)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(secret -> {
                    try {
                        SecretStorageUtil.storeSecret(secret);
                        System.out.println("Fetched and stored new secret after rotation");
                        return attemptRegistration(); // retry with fresh secret
                    } catch (Exception e) {
                        System.err.println(" Failed to store fetched secret: " + e.getMessage());
                        return Mono.just(false);
                    }
                })
                .onErrorResume(e -> {
                    System.err.println("Failed to fetch new secret: " + e.getMessage());
                    return Mono.just(false);
                });
    }
}