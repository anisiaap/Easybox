// src/main/java/com/example/network/controller/ReservationController.java
package com.example.network.controller;

import com.example.network.dto.CreateReservationRequest;
import com.example.network.dto.RecommendedBoxesResponse;
import com.example.network.dto.ReservationQueryRequest;
import com.example.network.entity.Reservation;
import com.example.network.service.ReservationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/widget")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reservation/available")
    public Mono<RecommendedBoxesResponse> getAvailableEasyboxes(
            @RequestBody ReservationQueryRequest request
    ) {
        LocalDateTime startTime = (request.getStart() != null) ? LocalDateTime.parse(request.getStart()) : null;
        LocalDateTime endTime   = (request.getEnd() != null) ? LocalDateTime.parse(request.getEnd()) : null;
        return reservationService.findAvailableBoxes(
                request.getAddress(),
                startTime,
                endTime,
                request.getMinTemperature(),
                request.getTotalDimension()
        );
    }

    @PostMapping("/reservation/hold")
    public Mono<Reservation> hold(@RequestBody CreateReservationRequest req, Authentication authentication) {
        Long bakeryId = Long.parseLong(authentication.getName()); // Now 'sub' is the bakery ID
        req.setBakeryId(bakeryId);  // Inject authenticated bakery ID
        return reservationService.holdReservation(req);
    }

    @PatchMapping("/reservation/{id}/confirm")
    public Mono<Reservation> confirm(@PathVariable Long id) {
        return reservationService.confirmReservation(id);
    }


    // If you want endpoints for listing or reading reservations, add them here...
}
