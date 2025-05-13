package com.example.network.config;

import com.example.network.entity.Easybox;
import com.example.network.repository.EasyboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Base64;
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

        // Step 1: Decode JWT payload (middle part) manually, without verifying signature
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new SecurityException("Invalid JWT format");
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            ObjectMapper mapper = new ObjectMapper();
            clientId = mapper.readTree(payloadJson).get("sub").asText(); // "sub" = subject
        } catch (Exception e) {
            throw new SecurityException("Unable to extract clientId from token", e);
        }

        // Step 2: Fetch Easybox by clientId
        Easybox box = easyboxRepository.findByClientId(clientId).block();

        // Step 3: If approved and secret exists → verify using device-specific key
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

        // Step 4: Fallback to shared secret (bootstrap)
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
