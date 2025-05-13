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
        Easybox box;

        try {
            // Try extracting clientId from shared secret
            Claims bootstrapClaims = Jwts.parser()
                    .setSigningKey(sharedSecret.getBytes())
                    .parseClaimsJws(token)
                    .getBody();

            clientId = bootstrapClaims.getSubject();
            box = easyboxRepository.findByClientId(clientId).block();

        } catch (JwtException e) {
            // ⛔ Fallback failed: maybe it was signed directly with device secret
            // Try brute-force checking all Easyboxes (NOT recommended in prod)
            throw new SecurityException("Invalid token or not signed with shared secret", e);
        }

        if (box == null || box.getSecretKey() == null || !Boolean.TRUE.equals(box.getApproved())) {
            throw new SecurityException("Device not approved or not found");
        }

        // ✅ Re-verify with per-device secret
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
}
