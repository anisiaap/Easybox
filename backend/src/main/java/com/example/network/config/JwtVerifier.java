package com.example.network.config;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtVerifier {

    // must match device secret
    @Value("${jwt.device-secret}")
    private String jwtSecret;
    public String verifyAndExtractClientId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret.getBytes())
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject(); // clientId
        } catch (JwtException e) {
            throw new SecurityException("Invalid JWT token", e);
        }
    }
}
