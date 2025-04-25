package com.example.network.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /* ------------------------------------------------------------------ *
     *  MAIN FILTER CHAIN
     * ------------------------------------------------------------------ */
    @Bean
    public SecurityWebFilterChain webFilterChain(ServerHttpSecurity http,
                                                 ReactiveJwtDecoder jwtDecoder,
                                                 ReactiveJwtAuthenticationConverter jwtAuthConverter) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                .authorizeExchange(ex -> ex
                        .pathMatchers("/api/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/widget/**").hasRole("BAKERY")
                        .pathMatchers("/api/mobile/**").hasAnyRole("USER", "BAKERY")
                        .pathMatchers("/api/device/**").permitAll()   // devices send token inside MQTT, not here
                        .anyExchange().permitAll()
                )

                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder)                 // ✅ correct method name
                                .jwtAuthenticationConverter(jwtAuthConverter)
                        )
                )
                .build();
    }

    /* ------------------------------------------------------------------ *
     *  JWT DECODER
     * ------------------------------------------------------------------ */
    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${jwt.dashboard-secret}") String secret) {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key).build();
    }

    /* ------------------------------------------------------------------ *
     *  JWT → REACTIVE AUTHENTICATION CONVERTER
     * ------------------------------------------------------------------ */
    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthConverter() {

        // (1)  normal Spring converter that picks "roles" claim and prefixes them with ROLE_
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");

        // (2)  Reactive wrapper expected by WebFlux
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();

        // Spring Security 6 wants Converter<Jwt, **Flux<GrantedAuthority>**>
        converter.setJwtGrantedAuthoritiesConverter(
                (Converter<Jwt, Flux<GrantedAuthority>>) jwt ->
                        Flux.fromIterable( roles.convert(jwt) )        // wrap Collection → Flux
        );

        return converter;
    }
}
