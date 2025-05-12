package com.example.easyboxdevice.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.time.Duration;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class JwtUtil {
    @Value("${central.backend.url}")
    private String centralBackendUrl;
    @Value("${jwt.device-secret}")     // the shared bootstrap secret
    private String fallbackSecret;

    public String generateToken(String subject, JwtRecoveryHandler onRecover) {
        String secret;
        try {
            secret = SecretStorageUtil.exists()
                    ? SecretStorageUtil.loadSecret()
                    : fallbackSecret;
        } catch (Exception ex) {
            System.err.println("❌ Failed to load secret: " + ex.getMessage());
            secret = fallbackSecret;
        }

        try {
            return Jwts.builder()
                    .setSubject(subject)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 10 * 60_000))
                    .signWith(SignatureAlgorithm.HS256, secret.getBytes())
                    .compact();
        } catch (Exception e) {
            System.err.println("⚠️ JWT signing failed, attempting recovery…");
            if (onRecover != null) {
                try {
                    String fallback = fallbackSecret;
                    // fetch new secret
                    String newSecret = fetchNewSecretFromServer(subject, fallback);
                    SecretStorageUtil.storeSecret(newSecret);
                    onRecover.onSecretRotated(newSecret);
                    return generateToken(subject, null);  // try again without recovery
                } catch (Exception ex2) {
                    System.err.println("❌ Failed secret recovery: " + ex2.getMessage());
                }
            }
            throw new RuntimeException("JWT signing failed", e);
        }
    }
    private String fetchNewSecretFromServer(String clientId, String fallbackToken) throws Exception {
        WebClient client = WebClient.builder().build();
        return client.get()
                .uri(centralBackendUrl + "device/" + clientId + "/secret")
                .header("Authorization", "Bearer " + fallbackToken)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(5));
    }
    public interface JwtRecoveryHandler {
        void onSecretRotated(String newSecret);
    }
}