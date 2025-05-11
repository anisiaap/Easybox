package com.example.network.config;

import com.example.network.entity.Easybox;
import com.example.network.repository.EasyboxRepository;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Date;

@Component
public class JwtVerifier {

    // must match device secret
    @Value("${jwt.device-secret}")
    private String sharedSecret;
    private final EasyboxRepository easyboxRepository;

    public JwtVerifier(EasyboxRepository easyboxRepository) {
        this.easyboxRepository = easyboxRepository;
    }
    public String verifyAndExtractClientId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(sharedSecret.getBytes()) // fallback
                    .parseClaimsJws(token)
                    .getBody();

            String clientId = claims.getSubject();

            Easybox box = easyboxRepository.findByClientId(clientId).block();

            if (box == null || box.getSecretKey() == null || !box.getApproved()) {
                throw new SecurityException("Device not approved or not found");
            }

            // Re-parse with per-device secret
            claims = Jwts.parser()
                    .setSigningKey(box.getSecretKey().getBytes())
                    .parseClaimsJws(token)
                    .getBody();

            // Check token not older than last rotation
            Date issuedAt = claims.getIssuedAt();
            if (issuedAt == null || issuedAt.toInstant().isBefore(box.getLastSecretRotation().atZone(ZoneId.systemDefault()).toInstant())) {
                throw new SecurityException("Token is too old (rotated)");
            }

            return claims.getSubject(); // clientId

        } catch (JwtException e) {
            throw new SecurityException("Invalid token", e);
        }
    }
}
