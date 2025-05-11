package com.example.easyboxdevice.service;

import com.example.easyboxdevice.config.JwtUtil;
import com.example.easyboxdevice.config.SecretStorageUtil;
import com.example.easyboxdevice.dto.RegistrationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.annotation.Retry;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;
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

    private final WebClient webClient;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public DeviceRegistrationService(JwtUtil jwtUtil, WebClient.Builder webClientBuilder) {
        this.jwtUtil = jwtUtil;
        this.webClient = webClientBuilder.build();
    }

    /**
     * Initialize the heartbeat scheduler to send a registration update every 10 minutes.
     */

    @PostConstruct
    public void init() {
        System.out.println("Starting device heartbeat scheduler...");

        scheduler.scheduleAtFixedRate(() -> attemptRegistration()
                        .doOnError(e -> System.err.println("⚠️ Registration failed: " + e.getMessage()))
                        .subscribe(),                       // NB: keep the chain reactive
                0, 30, TimeUnit.MINUTES);
    }
    /**
     * Attempts to register the device as active.
     */
    @Retry(name = "deviceRegistration", fallbackMethod = "registrationFallback")
    private Mono<Void> attemptRegistration() {
        RegistrationRequest req = new RegistrationRequest();
        req.setAddress(deviceAddress);
        req.setClientId(clientId);
        req.setStatus("active");

        String token = createToken();   // NEW

        return webClient.post()
                .uri(centralBackendUrl + "device/register")
                .header("Authorization", "Bearer " + token)
                .bodyValue(req)
                .retrieve()
                .toEntity(String.class)
                .flatMap(resp -> {
                    // save fresh secret whenever we get one
                    String newSecret = resp.getHeaders().getFirst("X-Device-Secret");
                    if (newSecret != null && !newSecret.isBlank()) {
                        try {
                            SecretStorageUtil.storeSecret(newSecret);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("🔐 Stored new device secret");
                    }
                    System.out.println("✅ Registration OK: " + resp.getBody());
                    return Mono.empty();
                });
    }

    private String createToken() {
        // If we already have a per‑device secret, use it
        if (SecretStorageUtil.exists()) {
            return jwtUtil.generateToken(clientId);
        }
        // First contact → sign with shared “fallback” secret
        return Jwts.builder()
                .setSubject(clientId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 10 * 60_000))
                .signWith(SignatureAlgorithm.HS256, fallbackSecret.getBytes())
                .compact();
    }

    /**
     * Fallback method triggered by Resilience4j when registration fails.
     */
//    private void registrationFallback(Throwable t) {
//        System.err.println("Registration fallback triggered: " + t.getMessage());
//        RegistrationRequest fallbackRequest = new RegistrationRequest();
//        fallbackRequest.setAddress(deviceAddress);
//        fallbackRequest.setClientId(clientId);
//        fallbackRequest.setStatus("inactive");
//        String token = jwtUtil.generateToken(clientId);
//        webClient.post()
//                .uri(centralBackendUrl + "device/register")
//                .header("Authorization", "Bearer " + token)
//                .bodyValue(fallbackRequest)
//                .retrieve()
//                .bodyToMono(String.class)
//                .subscribe(
//                        response -> System.out.println("Fallback registration (inactive) successful: " + response),
//                        error -> System.err.println("Fallback registration (inactive) failed: " + error.getMessage())
//                );
//    }

    /**
     * Shutdown method to stop the scheduler and send a final deregistration update.
     */
    @PreDestroy
    public void shutdown() {
        System.out.println("Device is shutting down. Initiating deregistration...");
        String token = jwtUtil.generateToken(clientId);
        // Stop the heartbeat scheduler gracefully
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                System.err.println("Scheduler did not terminate within the specified time. Forcing shutdown...");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.err.println("Scheduler shutdown interrupted.");
            scheduler.shutdownNow();
        }

        // Build the deregistration (inactive) request
        RegistrationRequest request = new RegistrationRequest();
        request.setAddress(deviceAddress);
        request.setClientId(clientId);
        request.setStatus("inactive");

        try {
            String response = webClient.post()
                    .uri(centralBackendUrl + "device/register")
                    .header("Authorization", "Bearer " + token)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            System.out.println("Deregistration successful: " + response);
        } catch (Exception e) {
            System.err.println("Deregistration failed: " + e.getMessage());
        }

    }
}
