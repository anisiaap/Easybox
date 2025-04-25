package com.example.network.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.util.Collection;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/widget/**").hasRole("BAKERY")
                        .pathMatchers("/api/mobile/**").hasAnyRole("USER", "BAKERY")
                        .pathMatchers("/api/device/**").permitAll() // MQTT devices use token in payload
                        .anyExchange().permitAll()
                )

                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                ).build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${jwt.dashboard-secret}") String secret) {
        return NimbusReactiveJwtDecoder
                .withSecretKey(new SecretKeySpec(secret.getBytes(), "HmacSHA256"))
                .build();
    }

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        return jwt -> {
            Collection<GrantedAuthority> authorities = authoritiesConverter.convert(jwt);
            AbstractAuthenticationToken auth = new JwtAuthenticationToken(jwt, authorities);
            return Mono.just(auth);
        };
    }

}
