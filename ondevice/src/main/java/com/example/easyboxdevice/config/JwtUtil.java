package com.example.easyboxdevice.config;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;

public class JwtUtil {

    @Value("${jwt.device-secret}")
    private String jwtSecret;
    
    private static final long EXPIRATION_TIME = 1000 * 60 * 10; // 10 minutes

    public static String generateToken(String clientId) {
        return Jwts.builder()
                .setSubject(clientId)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY.getBytes())
                .compact();
    }
}
