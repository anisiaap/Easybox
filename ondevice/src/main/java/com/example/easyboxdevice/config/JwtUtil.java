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

    public String generateToken(String subject) {
        String secret;
        try {
            secret = SecretStorageUtil.exists()
                    ? SecretStorageUtil.loadSecret()
                    : fallbackSecret;
        } catch (Exception ex) {
            System.err.println(" Failed to load secret: " + ex.getMessage());
            secret = fallbackSecret;
        }
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 10 * 60_000))
                .signWith(SignatureAlgorithm.HS256, secret.getBytes())
                .compact();

    }
}