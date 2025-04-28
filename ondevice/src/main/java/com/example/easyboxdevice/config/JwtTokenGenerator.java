package com.example.easyboxdevice.config;//package com.example.network.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

public class JwtTokenGenerator {

    public static void main(String[] args) {
        // Settings
        String secret = "keykeykeykeykeykeykeykeykeykeykeykeykey"; // from application.properties
        //String subject = "1"; // e.g. user email or bakery name
        List<String> roles = List.of("ADMIN"); // or List.of("BAKERY"), List.of("USER", "BAKERY"), etc.
        long validityInMillis = 1000L * 60 * 60 * 24 * 365; // 1 year

        // Generate
        String token = Jwts.builder()
                //.setSubject(subject)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + validityInMillis))
                .signWith(
                        SignatureAlgorithm.HS256,
                        new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")
                )
                .compact();

        System.out.println("Generated JWT:");
        System.out.println(token);
    }
}
////import io.jsonwebtoken.Jwts;
////import io.jsonwebtoken.SignatureAlgorithm;
////
////import java.util.Date;
////
////public class JwtTokenGenerator {
////    public static void main(String[] args) {
////        String secret = "keykeykeykeykeykeykeykeykeykeykeykeykey";  // replace with real one
////        String clientId = "easybox-1";
////
////        String token = Jwts.builder()
////                .setSubject(clientId)
////                .setExpiration(new Date(System.currentTimeMillis() + 600_000)) // 10 min
////                .signWith(SignatureAlgorithm.HS256, secret.getBytes())
////                .compact();
////
////        System.out.println(token);
////    }
////}
