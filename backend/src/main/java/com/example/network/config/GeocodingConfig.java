package com.example.network.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class GeocodingConfig {

    @Bean
    public Cache<String, double[]> geocodeCache() {
        return Caffeine.newBuilder()
                .maximumSize(20_000)              // tune as you like
                .expireAfterWrite(Duration.ofDays(30))
                .build();
    }

    @Bean
    public RateLimiter nominatimLimiter() {
        return RateLimiter.of("nominatim",
                RateLimiterConfig.custom()
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .limitForPeriod(1)                     // ← 1 request/second
                        .timeoutDuration(Duration.ofSeconds(2))
                        .build());
    }
}
