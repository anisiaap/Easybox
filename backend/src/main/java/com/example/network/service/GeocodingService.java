package com.example.network.service;

import com.example.network.exception.GeocodingException;
import com.github.benmanes.caffeine.cache.Cache;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class GeocodingService {

    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=";
    private static final String USER_AGENT =
            "EasyboxBackend/1.0 (contact@easybox.ro)";

    private final WebClient webClient;
    private final Cache<String, double[]> cache;
    private final RateLimiter rateLimiter;

    public GeocodingService(WebClient.Builder builder,
                            Cache<String, double[]> cache,
                            RateLimiter rateLimiter) {
        this.webClient   = builder.build();
        this.cache       = cache;
        this.rateLimiter = rateLimiter;
    }

    public Mono<double[]> geocodeAddress(String address) {
        if (address == null || address.isBlank())
            return Mono.error(new GeocodingException("Address is blank or null."));

        /* 1️⃣  cache first */
        double[] hit = cache.getIfPresent(address);
        if (hit != null) return Mono.just(hit);

        String url = NOMINATIM_URL + URLEncoder.encode(address, StandardCharsets.UTF_8);

        /* 2️⃣  one outbound call, rate-limited */
        /* 2️⃣  one outbound call, rate-limited */
        /* 2️⃣ one outbound call, rate-limited */
        return webClient.get()
                .uri(url)
                .header("User-Agent", USER_AGENT)
                .retrieve()
                .bodyToMono(NominatimResponse[].class)
                .transformDeferred(RateLimiterOperator.<NominatimResponse[]>of(rateLimiter))
                // If your reactor-core is < 3.5, replace transformDeferred(...) with transform(...)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(2)).jitter(0.5))
                .flatMap(this::toCoordinates)
                .doOnSuccess(coords -> cache.put(address, coords))
                .onErrorResume(ex ->
                        Mono.error(new GeocodingException(
                                "Error calling geocoding service for address '" + address +
                                        "': " + ex.getMessage())));
    }   // ← closes geocodeAddress

    /* ---------- helpers -------------------------------------------------- */

    private Mono<double[]> toCoordinates(NominatimResponse[] rsp) {
        if (rsp == null || rsp.length == 0)
            return Mono.error(new GeocodingException("No coordinates found for: " + rsp));

        try {
            double lat = Double.parseDouble(rsp[0].lat);
            double lon = Double.parseDouble(rsp[0].lon);
            return Mono.just(new double[] { lat, lon });
        } catch (NumberFormatException nfe) {
            return Mono.error(new GeocodingException("Invalid lat/lon in Nominatim response."));
        }
    }


    public double distance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /* DTO for JSON mapping */
    private static class NominatimResponse {
        public String lat;
        public String lon;
    }
}
