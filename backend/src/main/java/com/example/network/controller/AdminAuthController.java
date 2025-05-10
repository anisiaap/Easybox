package com.example.network.controller;

import com.example.network.config.JwtUtil;
import com.example.network.dto.LoginRequest;
import com.example.network.dto.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AdminAuthController {

    private final JwtUtil jwtUtil;

    @Value("${ADMIN_USERNAME}")
    private String adminUsername;

    @Value("${ADMIN_PASSWORD_HASH}")
    private String adminPasswordHash;

    public AdminAuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Mono<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        if (adminUsername.equals(loginRequest.getUsername()) &&
                BCrypt.checkpw(loginRequest.getPassword(), adminPasswordHash)) {

            String token = jwtUtil.generateShortToken(
                    1L,                      // dummy adminId
                    adminUsername,
                    List.of("ADMIN")
            );
            return Mono.just(new LoginResponse(token));
        }

        return Mono.error(new RuntimeException("Invalid credentials"));
    }
}