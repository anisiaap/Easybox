/* ──────────────────────────────────────────────────────────────
   GeocodingService.java
   ────────────────────────────────────────────────────────────── */
package com.example.network.service;

import com.example.network.exception.GeocodingException;
import com.github.benmanes.caffeine.cache.Cache;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import org.springframework.http.HttpStatus;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class GeocodingService {

    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=";
    private static final String USER_AGENT =
            "EasyboxBackend/1.0 (contact@easybox.ro)";

    private final WebClient  webClient;
    private final Cache<String, double[]> cache;
    private final RateLimiter rateLimiter;
    private final Bulkhead   bulkhead;

    public GeocodingService(WebClient.Builder builder,
                            Cache<String, double[]> cache,
                            RateLimiter rateLimiter,
                            Bulkhead bulkhead) {
        this.webClient   = builder.build();
        this.cache       = cache;
        this.rateLimiter = rateLimiter;
        this.bulkhead    = bulkhead;
    }

    /** Resolve an address to <code>[lat, lon]</code>. */
    public Mono<double[]> geocodeAddress(String address) {
        if (address == null || address.isBlank())
            return Mono.error(new GeocodingException("Address is blank or null."));

        /* 1️⃣ cache first */
        double[] hit = cache.getIfPresent(address);
        if (hit != null) return Mono.just(hit);

        String url = NOMINATIM_URL + URLEncoder.encode(address, StandardCharsets.UTF_8);

        /* 2️⃣ one outbound call – serialised, rate-limited, retry-aware */
        return webClient.get()
                .uri(url)
                .header("User-Agent", USER_AGENT)
                .retrieve()
                .onStatus(HttpStatusCode::isError, rsp ->
                        rsp.bodyToMono(String.class)
                                .defaultIfEmpty(reasonPhrase(rsp.statusCode()))
                                .flatMap(body -> Mono.error(new GeocodingException(
                                        "Nominatim " + rsp.statusCode() + " – " + body))))
                .bodyToMono(NominatimResponse[].class)

                /* Bulkhead → RateLimiter → Retry, in that order */
                .transformDeferred(BulkheadOperator.of(bulkhead))
                .transformDeferred(RateLimiterOperator.<NominatimResponse[]>of(rateLimiter))
                .retryWhen(retryStrategy())

                .flatMap(this::toCoordinates)
                .doOnSuccess(coords -> cache.put(address, coords));
    }

    /* ------------ helpers ------------------------------------------------ */
    private static String reasonPhrase(HttpStatusCode code) {
        return (code instanceof HttpStatus hs) ? hs.getReasonPhrase() : code.toString();
    }
    /** Retry only on transient network/HTTP 5xx/429 errors. */
    private Retry retryStrategy() {
        return Retry.backoff(2, Duration.ofSeconds(2))
                .filter(t -> {
                    if (t instanceof WebClientRequestException) return true;
                    if (t instanceof WebClientResponseException ex)
                        return ex.getStatusCode().is5xxServerError() ||
                                ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS;
                    return false;
                })
                .jitter(0.5)                              // 50 %–150 % back-off
                .maxBackoff(Duration.ofSeconds(30));
    }

    /** Map the first element to <code>[lat, lon]</code>; empty list → <code>Mono.empty()</code>. */
    private Mono<double[]> toCoordinates(NominatimResponse[] rsp) {
        if (rsp == null || rsp.length == 0) return Mono.empty();
        try {
            double lat = Double.parseDouble(rsp[0].lat);
            double lon = Double.parseDouble(rsp[0].lon);
            return Mono.just(new double[]{lat, lon});
        } catch (NumberFormatException nfe) {
            return Mono.error(new GeocodingException("Invalid lat/lon in Nominatim response."));
        }
    }

    /** Great-circle distance (metres) via the haversine formula. */
    public double distance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /* DTO for JSON mapping */
    private static class NominatimResponse {
        public String lat;
        public String lon;
    }
}
