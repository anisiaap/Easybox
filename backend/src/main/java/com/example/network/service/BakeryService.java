package com.example.network.service;

import com.example.network.config.JwtUtil;
import com.example.network.model.Bakery;
import com.example.network.repository.BakeryRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Service
public class BakeryService {

    private final BakeryRepository bakeryRepository;
    private final JwtUtil jwtUtil;
    private final JwtParser jwtParser;

    public BakeryService(BakeryRepository bakeryRepository, JwtUtil jwtUtil) {
        this.bakeryRepository = bakeryRepository;
        this.jwtUtil = jwtUtil;
        this.jwtParser = Jwts.parserBuilder().build(); // no signature validation, only parsing
    }

    public Mono<Bakery> refreshTokenIfExpired(Bakery bakery) {
        String token = bakery.getToken();

        boolean expired = false;

        try {
            jwtParser.parseClaimsJwt(token).getBody();
        } catch (ExpiredJwtException ex) {
            expired = true;
        } catch (Exception ex) {
            expired = true; // treat invalid/malformed tokens as expired
        }

        if (expired) {
            String newToken = jwtUtil.generateLongLivedToken(
                    bakery.getId(),
                    bakery.getPhone(),
                    Collections.singletonList("BAKERY")
            );
            bakery.setToken(newToken);
            return bakeryRepository.save(bakery)
                    .doOnSuccess(b -> System.out.println("🔑 Refreshed token for bakery " + b.getId()));
        }

        return Mono.just(bakery);
    }
}