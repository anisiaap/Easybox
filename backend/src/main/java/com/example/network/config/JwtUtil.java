package com.example.network.config;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class JwtUtil {

    @Value("${jwt.device-secret}")
    private String jwtSecret;

    private final PrivateKey privateKey;

    public JwtUtil(@Value("${JWT_RSA_PRIVATE}") String privateKeyPem) throws Exception {
        String pem = new String(Base64.getDecoder().decode(privateKeyPem));
        this.privateKey = PemUtils.parsePrivateKeyFromPem(pem);
    }
    private static final long EXPIRATION_TIME = 1000 * 60 * 10; // 10 minutes

    public String generateToken(String clientId) {
        return Jwts.builder()
                .setSubject(clientId)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes())
                .compact();
    }
    public String generateTokenDuration(Long userId, String phone, List<String> roles, Duration ttl) {
        return Jwts.builder()
                .setSubject(phone)
                .claim("userId", userId)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(ttl)))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public String generateShortToken(Long userId, String phone, List<String> roles) {
        return generateTokenDuration(userId, phone, roles, Duration.ofMinutes(15));
    }

    public String generateLongLivedToken(Long userId, String phone, List<String> roles) {
        return generateTokenDuration(userId, phone, roles, Duration.ofDays(90));
    }

}
