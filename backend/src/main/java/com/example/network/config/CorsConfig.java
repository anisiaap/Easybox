package com.example.network.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;       // ← reactive
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;

import java.util.Arrays;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 1) exactly the origins you need:
        config.setAllowedOrigins(Arrays.asList(
                "https://widget.easybox-food.xyz",
                "https://admin.easybox-food.xyz",
                "http://localhost:3001"
        ));
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
    config.setAllowCredentials(true);

    // Allow all origins for /api/device/**
    CorsConfiguration deviceConfig = new CorsConfiguration();
    deviceConfig.setAllowedOrigins(Arrays.asList("*")); // Allow all origins
    deviceConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
    deviceConfig.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
    deviceConfig.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/device/**", deviceConfig); // Apply to /api/device/**
    source.registerCorsConfiguration("/**", config); // Apply to all other endpoints
    return source;
}
}

