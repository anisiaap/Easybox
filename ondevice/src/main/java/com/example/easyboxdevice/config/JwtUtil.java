package com.example.easyboxdevice.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    public String generateToken(String clientId) {
        try {
            String jwtSecret = SecretStorageUtil.loadSecret(); // 🔄 Load here
            return Jwts.builder()
                    .setSubject(clientId)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 10 * 60_000))
                    .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes())
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to load secret when generating token", e);
        }
    }
}