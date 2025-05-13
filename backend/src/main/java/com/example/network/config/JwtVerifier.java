package com.example.network.config;

import com.example.network.entity.Easybox;
import com.example.network.repository.EasyboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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
    public Mono<String> verifyAndExtractClientId(String token) {
        String clientId;

        // Step 1: Decode JWT payload manually (without signature verification)
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return Mono.error(new SecurityException("Invalid JWT format"));
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            ObjectMapper mapper = new ObjectMapper();
            clientId = mapper.readTree(payloadJson).get("sub").asText();
        } catch (Exception e) {
            return Mono.error(new SecurityException("Unable to extract clientId from token", e));
        }

        // Step 2: Fetch Easybox reactively
        return easyboxRepository.findByClientId(clientId)
                .flatMap(box -> {
                    // Step 3: Try device-specific key if approved
                    if (box != null && box.getSecretKey() != null && Boolean.TRUE.equals(box.getApproved())) {
                        try {
                            Claims verifiedClaims = Jwts.parser()
                                    .setSigningKey(box.getSecretKey().getBytes())
                                    .parseClaimsJws(token)
                                    .getBody();

                            Date issuedAt = verifiedClaims.getIssuedAt();
                            if (issuedAt == null || issuedAt.toInstant().isBefore(
                                    box.getLastSecretRotation().atZone(ZoneId.systemDefault()).toInstant())) {
                                return Mono.error(new SecurityException("Token is too old (rotated)"));
                            }

                            return Mono.just(verifiedClaims.getSubject());
                        } catch (JwtException e) {
                            return Mono.error(new SecurityException("Token invalid for device-specific secret", e));
                        }
                    }

                    // Step 4: Fallback to shared secret (bootstrap)
                    try {
                        Claims fallbackClaims = Jwts.parser()
                                .setSigningKey(sharedSecret.getBytes())
                                .parseClaimsJws(token)
                                .getBody();

                        return Mono.just(fallbackClaims.getSubject());
                    } catch (JwtException e) {
                        return Mono.error(new SecurityException("Token invalid for shared fallback secret", e));
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Still allow fallback in case no Easybox yet
                    try {
                        Claims fallbackClaims = Jwts.parser()
                                .setSigningKey(sharedSecret.getBytes())
                                .parseClaimsJws(token)
                                .getBody();

                        return Mono.just(fallbackClaims.getSubject());
                    } catch (JwtException e) {
                        return Mono.error(new SecurityException("Token invalid for shared fallback secret", e));
                    }
                }));
    }
}
