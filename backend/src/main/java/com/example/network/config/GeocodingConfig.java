/* ──────────────────────────────────────────────────────────────
   GeocodingConfig.java
   ────────────────────────────────────────────────────────────── */
package com.example.network.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class GeocodingConfig {

    /** LRU cache of resolved addresses → coordinates. */
    @Bean
    public Cache<String, double[]> geocodeCache() {
        return Caffeine.newBuilder()
                .maximumSize(20_000)
                .expireAfterWrite(Duration.ofDays(30))
                .build();
    }

    /** Global limiter: **≤ 1 request/second** total to public Nominatim. */
    @Bean
    public RateLimiter nominatimLimiter() {
        return RateLimiter.of("nominatim",
                RateLimiterConfig.custom()
                        .limitForPeriod(1)
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .timeoutDuration(Duration.ofSeconds(5))
                        .build());
    }

    /** Serialises outbound calls so only one is in flight at any time. */
    @Bean
    public Bulkhead geoBulkhead() {
        return Bulkhead.of("geoBulkhead",
                BulkheadConfig.custom()
                        .maxConcurrentCalls(1)
                        .maxWaitDuration(Duration.ofSeconds(10))
                        .build());
    }
}
