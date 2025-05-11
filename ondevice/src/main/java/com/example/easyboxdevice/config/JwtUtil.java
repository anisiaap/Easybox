package com.example.easyboxdevice.config;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.security.PrivateKey;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class JwtUtil {

    private final PrivateKey privateKey;

    public JwtUtil(@Value("${jwt.device-private-key}") String privateKeyPem) throws Exception {
        this.privateKey = PemUtils.parsePrivateKeyFromPem(privateKeyPem);
    }

    private static final long EXPIRATION_TIME = 1000 * 60 * 10; // 10 minutes

    public String generateToken(String clientId) {
        return Jwts.builder()
                .setSubject(clientId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }
}