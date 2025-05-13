package com.example.network.config;

import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebFluxSecurity
@Order(0)
public class DeviceSecurityConfig {

    @Bean
    public SecurityWebFilterChain deviceWebFilterChain(
            ServerHttpSecurity http,
            ReactiveJwtDecoder deviceJwtDecoder,
            ReactiveJwtAuthenticationConverter authConverter
    ) {
        return http
                .securityMatcher(new PathPatternParserServerWebExchangeMatcher("/api/device/**"))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // require a valid JWT on /api/device/register
                .authorizeExchange(ex -> ex
                        .pathMatchers("/api/device/register").permitAll()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt
                                .jwtDecoder(deviceJwtDecoder)
                                .jwtAuthenticationConverter(authConverter)
                        )
                )
                .build();
    }
    @Bean
    public ReactiveJwtDecoder deviceJwtDecoder(
            @Value("${jwt.device-secret}") String secret
    ) {
        SecretKeySpec key = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                SignatureAlgorithm.HS256.getJcaName()
        );
        return NimbusReactiveJwtDecoder.withSecretKey(key).build();
    }

}

