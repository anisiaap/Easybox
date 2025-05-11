package com.example.easyboxdevice.config;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class JwtUtil {
    private final String jwtSecret;

    public JwtUtil() {
        try {
            if (!SecretStorageUtil.exists()) throw new RuntimeException("No device secret found!");
            jwtSecret = SecretStorageUtil.loadSecret();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load secret", e);
        }
    }

    public String generateToken(String clientId) {
        return Jwts.builder()
                .setSubject(clientId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 10 * 60_000))
                .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes())
                .compact();
    }
}
