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
        String clientId;

        // Step 1: Try extracting subject (clientId) without verifying
        try {
            Claims claims = Jwts.parser()
                    .parseClaimsJws(token)
                    .getBody();
            clientId = claims.getSubject();
        } catch (JwtException e) {
            throw new SecurityException("Unable to parse token subject", e);
        }

        Easybox box = easyboxRepository.findByClientId(clientId).block();

        // Step 2: If device is known and approved, prefer device-specific verification
        if (box != null && box.getSecretKey() != null && Boolean.TRUE.equals(box.getApproved())) {
            try {
                Claims verifiedClaims = Jwts.parser()
                        .setSigningKey(box.getSecretKey().getBytes())
                        .parseClaimsJws(token)
                        .getBody();

                Date issuedAt = verifiedClaims.getIssuedAt();
                if (issuedAt == null || issuedAt.toInstant().isBefore(
                        box.getLastSecretRotation().atZone(ZoneId.systemDefault()).toInstant())) {
                    throw new SecurityException("Token is too old (rotated)");
                }

                return verifiedClaims.getSubject();
            } catch (JwtException e) {
                throw new SecurityException("Token invalid for device-specific secret", e);
            }
        }

        // Step 3: Fallback to shared secret if device not found or not yet approved
        try {
            Claims bootstrapClaims = Jwts.parser()
                    .setSigningKey(sharedSecret.getBytes())
                    .parseClaimsJws(token)
                    .getBody();

            return bootstrapClaims.getSubject();
        } catch (JwtException e) {
            throw new SecurityException("Token invalid for shared fallback secret", e);
        }
    }
}
