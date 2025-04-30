package com.example.network.controller;

import com.example.network.config.PasswordConfig;
import com.example.network.entity.Bakery;
import com.example.network.entity.User;
import com.example.network.repository.BakeryRepository;
import com.example.network.repository.UserRepository;
import com.example.network.config.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/app/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final BakeryRepository bakeryRepo;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepo, BakeryRepository bakeryRepo, JwtUtil jwtUtil, BCryptPasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.bakeryRepo = bakeryRepo;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    // Client sign up
    @PostMapping("/register-client")
    public Mono<ResponseEntity<String>> registerClient(@RequestBody User user) {
        String phone = user.getPhoneNumber();
        String name = user.getName();
        String password = user.getPassword();

        return userRepo.findByPhoneNumber(phone)
                .flatMap(existing -> {
                    if (existing.getPassword() != null) {
                        return Mono.just(ResponseEntity.badRequest().body("Client already registered"));
                    }
                    existing.setName(name); // update name if needed
                    existing.setPassword(passwordEncoder.encode(password)); // set password now
                    return userRepo.save(existing)
                            .thenReturn(ResponseEntity.ok("Client upgraded to full account"));
                })
                .switchIfEmpty(
                        userRepo.save(new User(null, name, phone, passwordEncoder.encode(password)))
                                .thenReturn(ResponseEntity.ok("New client registered"))
                );
    }



    // Bakery sign up
    private Mono<ResponseEntity<Bakery>> badRequestBakery() {
        return Mono.just(ResponseEntity.badRequest().<Bakery>build());
    }

    @PostMapping("/register-bakery")
    public Mono<ResponseEntity<Bakery>> registerBakery(@RequestBody Bakery bakery) {
        return bakeryRepo.findByPhone(bakery.getPhone())
                .flatMap(existing -> badRequestBakery())
                .switchIfEmpty(
                        Mono.defer(() -> {
                            String token = jwtUtil.generateToken(
                                    bakery.getPhone(),
                                    List.of("BAKERY")
                            );
                            bakery.setPluginInstalled(false);
                            bakery.setToken(token);
                            return bakeryRepo.save(bakery)
                                    .map(ResponseEntity::ok);
                        })
                );
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<String>> login(@RequestBody Map<String, String> payload) {
        String phone = payload.get("phone");
        String password = payload.get("password");
        String role = payload.get("role");

        if ("BAKERY".equalsIgnoreCase(role)) {
            return bakeryRepo.findByPhone(phone)
                    .flatMap(bakery -> {
                        if (!Boolean.TRUE.equals(bakery.getPluginInstalled())) {
                            return Mono.just(ResponseEntity.status(403).body("Not approved yet"));
                        }
                        if (!passwordEncoder.matches(password, bakery.getPassword())) {
                            return Mono.just(ResponseEntity.status(401).body("Invalid password"));
                        }
                        return Mono.just(ResponseEntity.ok(bakery.getToken()));
                    })
                    .switchIfEmpty(Mono.just(ResponseEntity.status(404).body("Bakery not found")));
        }

        if ("USER".equalsIgnoreCase(role)) {
            return userRepo.findByPhoneNumber(phone)
                    .flatMap(user -> {
                        if (!passwordEncoder.matches(password, user.getPassword())) {
                            return Mono.just(ResponseEntity.status(401).body("Invalid password"));
                        }
                        String token = jwtUtil.generateToken(user.getPhoneNumber(), List.of("USER"));
                        return Mono.just(ResponseEntity.ok(token)); // Still needed for JWT-based auth
                    })
                    .switchIfEmpty(Mono.just(ResponseEntity.status(404).body("User not found")));
        }

        return Mono.just(ResponseEntity.badRequest().body("Unknown role"));
    }

}
