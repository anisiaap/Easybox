package com.example.network.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import reactor.core.publisher.Flux;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

@Configuration
@EnableWebFluxSecurity
@Order(1) //  lower priority (after DeviceSecurityConfig)
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain webFilterChain(ServerHttpSecurity http,
                                                 ReactiveJwtDecoder jwtDecoder,
                                                 ReactiveJwtAuthenticationConverter jwtAuthConverter,
                                                 CorsConfigurationSource corsSource) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsSource))
                .authorizeExchange(ex -> ex
                        .pathMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .pathMatchers("/api/admin/**").hasRole("ADMIN")
                        .pathMatchers("/api/widget/**").hasRole("BAKERY")
                        .pathMatchers("/api/mobile/**").hasAnyRole("USER", "BAKERY")
                        .anyExchange().permitAll()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthConverter)
                        )
                )
                .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${JWT_RSA_PUBLIC}") String publicKeyPem) throws Exception {
        String pem = new String(Base64.getDecoder().decode(publicKeyPem));
        PublicKey key = PemUtils.parsePublicKeyFromPem(pem);

        if (!(key instanceof RSAPublicKey)) {
            throw new IllegalArgumentException("Provided public key is not an RSA key");
        }

        return NimbusReactiveJwtDecoder.withPublicKey((RSAPublicKey) key).build();
    }


    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");

        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                (Converter<Jwt, Flux<GrantedAuthority>>) jwt ->
                        Flux.fromIterable(roles.convert(jwt))
        );
        return converter;
    }
}
