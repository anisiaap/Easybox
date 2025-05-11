package com.example.easyboxdevice.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class JwtUtil {

    @Value("${jwt.device-secret}")     // the shared bootstrap secret
    private String fallbackSecret;

    public String generateToken(String subject) {
        String secret;

        if (SecretStorageUtil.exists()) {
            try {
                secret = SecretStorageUtil.loadSecret();   // the real per‑device key
            } catch (Exception ex) {                       // corrupt file, etc.
                secret = fallbackSecret;
            }
        } else {
            secret = fallbackSecret;                       // first‑boot path
        }

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 10 * 60_000))
                .signWith(SignatureAlgorithm.HS256, secret.getBytes())
                .compact();
    }
}