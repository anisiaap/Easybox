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
        return Jwts.parser()
                .setSigningKey(secret.getBytes())
                .parseClaimsJws(token)
                .getBody()
                .getSubject();

    }

}
