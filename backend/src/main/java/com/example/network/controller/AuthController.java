package com.example.network.controller;

import com.example.network.dto.BakeryRegistrationRequest;
import com.example.network.dto.UserRegistrationRequest;
import com.example.network.model.Bakery;
import com.example.network.model.User;
import com.example.network.exception.ConflictException;
import com.example.network.repository.BakeryRepository;
import com.example.network.repository.UserRepository;
import com.example.network.config.JwtUtil;
import com.example.network.service.BakeryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import com.example.network.dto.ProfileResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/app/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final BakeryRepository bakeryRepo;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    private final BakeryService bakeryService;

    public AuthController(
            UserRepository userRepo,
            BakeryRepository bakeryRepo,
            JwtUtil jwtUtil,
            BCryptPasswordEncoder passwordEncoder,
            BakeryService bakeryService
    ) {
        this.userRepo = userRepo;
        this.bakeryRepo = bakeryRepo;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.bakeryService = bakeryService;
    }

    // Client sign up
    @PostMapping("/register-client")
    public Mono<ResponseEntity<String>> registerClient(@Valid @RequestBody UserRegistrationRequest dto) {
        String phone = dto.getPhone();
        String name = dto.getName();
        String password = dto.getPassword();

        return userRepo.findByPhoneNumber(phone)
                .flatMap(existing -> {
                    if (existing.getPassword() != null) {
                        return Mono.error(new ConflictException("Client already registered"));
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
    public Mono<ResponseEntity<String>> registerBakery(@Valid @RequestBody BakeryRegistrationRequest dto) {
        String phone = dto.getPhone();
        String name = dto.getName();
        String password = dto.getPassword();

        return bakeryRepo.findByPhone(phone)
                .flatMap(existing -> {
                    if (existing.getPassword() != null) {
                        return Mono.error(new ConflictException("Bakery already registered"));
                    }

                    // Ensure the bakery has an ID before proceeding
                    if (existing.getId() == null) {
                        return Mono.error(new IllegalStateException("Existing bakery has no ID"));
                    }

                    existing.setName(name);
                    existing.setPassword(passwordEncoder.encode(password));
                    existing.setPluginInstalled(false);

                    // Generate token only after making sure ID is not null
                    String token = jwtUtil.generateLongLivedToken(existing.getId(), existing.getPhone(), List.of("BAKERY"));
                    existing.setToken(token);

                    return bakeryRepo.save(existing)
                            .thenReturn(ResponseEntity.ok("Bakery upgraded to full account"));
                })
                .switchIfEmpty(
                        Mono.defer(() -> {
                            Bakery newBakery = new Bakery();
                            newBakery.setName(name);
                            newBakery.setPhone(phone);
                            newBakery.setPassword(passwordEncoder.encode(password));
                            newBakery.setPluginInstalled(false);

                            return bakeryRepo.save(newBakery)
                                    .flatMap(savedBakery -> {
                                        Long newId = savedBakery.getId();
                                        if (newId == null) {
                                            return Mono.error(new IllegalStateException("Saved bakery has null ID"));
                                        }

                                        return bakeryRepo.findById(newId)
                                                .flatMap(bakeryWithId -> {
                                                    String token = jwtUtil.generateLongLivedToken(
                                                            bakeryWithId.getId(),
                                                            bakeryWithId.getPhone(),
                                                            List.of("BAKERY")
                                                    );
                                                    bakeryWithId.setToken(token);
                                                    return bakeryRepo.save(bakeryWithId)
                                                            .thenReturn(ResponseEntity.ok("New bakery registered"));
                                                });
                                    });
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
                            return Mono.error(new SecurityException("Not approved yet"));
                        }
                        if (!passwordEncoder.matches(password, bakery.getPassword())) {
                            return Mono.error(new SecurityException("Invalid password"));
                        }
                        return bakeryService.refreshTokenIfExpired(bakery)
                                .flatMap(updated -> {
                                    String shortToken = jwtUtil.generateShortToken(
                                            updated.getId(),
                                            updated.getPhone(),
                                            List.of("BAKERY")
                                    );
                                    return Mono.just(ResponseEntity.ok(shortToken));
                                });
                    })
                    .switchIfEmpty(Mono.error(new SecurityException("Bakery not found")));
        }

        if ("USER".equalsIgnoreCase(role)) {
            return userRepo.findByPhoneNumber(phone)
                    .flatMap(user -> {
                        if (!passwordEncoder.matches(password, user.getPassword())) {
                            return Mono.error(new SecurityException("Invalid password"));
                        }
                        String token = jwtUtil.generateShortToken(user.getId(), user.getPhoneNumber(), List.of("USER"));
                        return Mono.just(ResponseEntity.ok(token)); // Still needed for JWT-based auth
                    })
                    .switchIfEmpty(Mono.error(new SecurityException("User not found")));
        }

        return Mono.error(new ConflictException("Unknown role"));
    }
    @GetMapping("/me")
    public Mono<ResponseEntity<ProfileResponse>> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        String phone = jwt.getSubject();
        List<String> roles = jwt.getClaim("roles");

        if (roles.contains("BAKERY")) {
            return bakeryRepo.findById(userId)
                    .map(b -> ResponseEntity.ok(new ProfileResponse(
                            b.getId(),
                            b.getName(),
                            b.getPhone(),
                            "bakery",
                            b.getToken() // ✱ include token
                    )));
        } else if (roles.contains("USER")) {
            return userRepo.findById(userId)
                    .map(u -> ResponseEntity.ok(new ProfileResponse(
                            u.getId(),
                            u.getName(),
                            u.getPhoneNumber(),
                            "client",
                            null // ✱ no token for clients
                    )));
        } else {
            return Mono.just(ResponseEntity.status(403).build());
        }
    }
    @GetMapping("/refresh-token")
    public Mono<ResponseEntity<String>> refreshToken(@AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        String phone = jwt.getSubject();
        List<String> roles = jwt.getClaim("roles");

        String newToken = jwtUtil.generateShortToken(userId, phone, roles);
        return Mono.just(ResponseEntity.ok(newToken));
    }
}