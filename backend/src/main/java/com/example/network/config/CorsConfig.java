package com.example.network.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() { // ⚡ NOT CorsConfigurationSource
        CorsConfiguration config = new CorsConfiguration();
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
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // ⚡ Allow all API paths
        return new CorsWebFilter(source);
    }
}
