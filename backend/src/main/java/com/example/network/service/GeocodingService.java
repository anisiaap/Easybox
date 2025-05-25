// src/main/java/com/example/network/service/GeocodingService.java
package com.example.network.service;

import com.example.network.exception.GeocodingException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class GeocodingService {

    private final WebClient webClient;
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=";

    public GeocodingService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Returns [latitude, longitude] for the given address, or [0,0] if not found.
     * If you want a strict error approach, you can throw a GeocodingException
     * if the array is empty or lat/lon can't be parsed.
     */
    // src/main/java/com/example/network/service/GeocodingService.java
    public Mono<double[]> geocodeAddress(String address) {
        if (address == null || address.isBlank()) {
            // If you also want to fail immediately when address is blank:
            return Mono.error(new GeocodingException("Address is blank or null."));
        }
        System.out.println("error2");
        String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String url = NOMINATIM_URL + encoded;
        System.out.println(address);
        return webClient.get()
                .uri(url)
                .header("User-Agent", "MyApp/1.0 (contact: your-email@example.com)")
                .retrieve()
                .bodyToMono(NominatimResponse[].class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))  // optional retry
                .flatMap(responses -> {
                    // Instead of returning [0,0], throw an error
                    if (responses == null || responses.length == 0) {
                        return Mono.error(new GeocodingException("No coordinates found for: " + address));
                    }
                    try {
                        double lat = Double.parseDouble(responses[0].lat);
                        double lon = Double.parseDouble(responses[0].lon);
                        System.out.println("error3");
                        return Mono.just(new double[]{lat, lon});
                    } catch (NumberFormatException e) {
                        System.out.println("error4");
                        return Mono.error(new GeocodingException(
                                "Invalid lat/lon from Nominatim for: " + address
                        ));
                    }
                })
                // If you do *not* want a fallback and prefer to bubble up the exception,
                // you can remove onErrorResume altogether. If you keep it, re-throw or adjust as needed:
                .onErrorResume(ex -> Mono.error(new GeocodingException(
                        "Error calling geocoding service for address: " + address + ". " + ex.getMessage()
                )));
    }

    /**
     * Haversine distance in meters between (lat1, lon1) and (lat2, lon2).
     */
    public double distance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    // Inner class for the JSON mapping
    private static class NominatimResponse {
        public String lat;
        public String lon;
    }
}
