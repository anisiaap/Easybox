package com.example.easyboxdevice.service;

import com.example.easyboxdevice.config.JwtUtil;
import com.example.easyboxdevice.dto.RegistrationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
        scheduler.scheduleAtFixedRate(() -> attemptRegistration().subscribe(),
                                                     0, 10, TimeUnit.MINUTES); }

    /**
     * Attempts to register the device as active.
     */
    @Retry(name = "deviceRegistration", fallbackMethod = "registrationFallback")
    private Mono<Void> attemptRegistration() {
        System.out.println("🔄 Attempting to register device...");
        RegistrationRequest request = new RegistrationRequest();
        request.setAddress(deviceAddress);
        request.setClientId(clientId);
        request.setStatus("active");

        try {
            String jsonPayload = new ObjectMapper().writeValueAsString(request);
            System.out.println("Sending payload: " + jsonPayload);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String token = jwtUtil.generateToken(clientId);


        return webClient.post()
                .uri(centralBackendUrl + "device/register")
                .header("Authorization", "Bearer " + token)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.isError(), response -> {
                    System.err.println("❌ Registration failed with status: " + response.statusCode());
                    return response.bodyToMono(String.class)
                            .doOnNext(body -> System.err.println("❌ Error body: " + body))
                            .flatMap(errorBody -> response.createException());
                })
                .bodyToMono(String.class)
                .doOnSuccess(response -> System.out.println("✅ Registration successful: " + response))
                .doOnError(error -> System.err.println("⚠️ Registration HTTP error: " + error.getMessage()))
                .then();

    }

    /**
     * Fallback method triggered by Resilience4j when registration fails.
     */
    private void registrationFallback(Throwable t) {
        System.err.println("Registration fallback triggered: " + t.getMessage());
        RegistrationRequest fallbackRequest = new RegistrationRequest();
        fallbackRequest.setAddress(deviceAddress);
        fallbackRequest.setClientId(clientId);
        fallbackRequest.setStatus("inactive");

        webClient.post()
                .uri(centralBackendUrl + "device/register")
                .bodyValue(fallbackRequest)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        response -> System.out.println("Fallback registration (inactive) successful: " + response),
                        error -> System.err.println("Fallback registration (inactive) failed: " + error.getMessage())
                );
    }

    /**
     * Shutdown method to stop the scheduler and send a final deregistration update.
     */
    @PreDestroy
    public void shutdown() {
        System.out.println("Device is shutting down. Initiating deregistration...");

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
