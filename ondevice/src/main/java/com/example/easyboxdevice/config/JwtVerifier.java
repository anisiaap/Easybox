package com.example.easyboxdevice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
public class JwtVerifier {

    @Value("${central.backend.url}")
    private String centralBackendUrl;
    // must match device secret
    @Value("${jwt.device-secret}")
    private String jwtSecret;
    public String verifyAndExtractClientId(String token) {
        String secret;
        try {
            secret = SecretStorageUtil.exists()
                    ? SecretStorageUtil.loadSecret()
                    : jwtSecret;
        } catch (Exception ex) {
            System.err.println("❌ Failed to load stored secret, falling back to shared secret");
            secret = jwtSecret;
        }

        try {
            return Jwts.parser()
                    .setSigningKey(secret.getBytes())
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (JwtException e) {
            System.err.println("⚠️ JWT verification failed, attempting to refresh secret…");
            try {
                String fallbackToken = Jwts.builder()
                        .setSubject("fallback")
                        .signWith(io.jsonwebtoken.SignatureAlgorithm.HS256, jwtSecret.getBytes())
                        .compact();

                String newSecret = fetchNewSecretFromServer(extractClientIdWithoutVerify(token), fallbackToken);
                SecretStorageUtil.storeSecret(newSecret);

                return Jwts.parser()
                        .setSigningKey(newSecret.getBytes())
                        .parseClaimsJws(token)
                        .getBody()
                        .getSubject();
            } catch (Exception retryEx) {
                throw new SecurityException("JWT recovery failed", retryEx);
            }
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
    private String extractClientIdWithoutVerify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("Invalid JWT format");
            String payload = new String(java.util.Base64.getDecoder().decode(parts[1]));
            int start = payload.indexOf("\"sub\":\"") + 7;
            int end = payload.indexOf("\"", start);
            return payload.substring(start, end);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to extract clientId from token", e);
        }
    }
}
