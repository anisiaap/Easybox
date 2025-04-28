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
                "http://192.168.1.133:3000",
                "http://192.168.1.133:3001",
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:3030",
                "http://localhost:5500",
                "http://192.168.1.133:8081"
        ));
        config.setAllowedMethods(Arrays.asList("GET","POST","PATCH","PUT","DELETE","OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization","Content-Type","*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

