// src/main/java/com/example/network/controller/ReservationController.java
package com.example.network.controller;

import com.example.network.dto.CreateReservationRequest;
import com.example.network.dto.RecommendedBoxesResponse;
import com.example.network.dto.ReservationQueryRequest;
import com.example.network.entity.Reservation;
import com.example.network.exception.ConflictException;
import com.example.network.service.ReservationService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/available")
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

    @PostMapping("/hold")
    public Mono<Reservation> hold(@RequestBody CreateReservationRequest req) {
        return reservationService.holdReservation(req);
    }

    @PatchMapping("/{id}/confirm")
    public Mono<Reservation> confirm(@PathVariable Long id) {
        return reservationService.confirmReservation(id);
    }


    // If you want endpoints for listing or reading reservations, add them here...
}
