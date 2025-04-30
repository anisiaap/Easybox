package com.example.network.config;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class JwtUtil {

    @Value("${jwt.device-secret}")
    private String jwtSecret;
    @Value("${jwt.dashboard-secret}")
    private String jwtSecret_dashboard;

    private static final long EXPIRATION_TIME = 1000 * 60 * 10; // 10 minutes

    public String generateToken(String clientId) {
        return Jwts.builder()
                .setSubject(clientId)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes())
                .compact();
    }
    public String generateTokenBakery(Long userId, String subject, List<String> roles) {
        long validityMs = 1000L * 60 * 60 * 24 * 365; // 1 year
        return Jwts.builder()
                .setSubject(subject)
                .claim("roles", roles)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + validityMs))
                .signWith(SignatureAlgorithm.HS256, jwtSecret_dashboard.getBytes())
                .compact();
    }

}
