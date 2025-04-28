// src/main/java/com/example/network/controller/AdminDashboardController.java
package com.example.network.controller;

import com.example.network.dto.DashboardStatsDto;
import com.example.network.repository.EasyboxRepository;
import com.example.network.repository.ReservationRepository;
import com.example.network.repository.CompartmentRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final EasyboxRepository easyboxRepository;
    private final ReservationRepository reservationRepository;
    private final CompartmentRepository compartmentRepository;

    public AdminDashboardController(EasyboxRepository easyboxRepository,
                                    ReservationRepository reservationRepository,
                                    CompartmentRepository compartmentRepository) {
        this.easyboxRepository = easyboxRepository;
        this.reservationRepository = reservationRepository;
        this.compartmentRepository = compartmentRepository;
    }

    @GetMapping("/stats")
    public Mono<DashboardStatsDto> getStats() {
        return Mono.zip(
                        easyboxRepository.count(),
                        compartmentRepository.count(),
                        reservationRepository.count(),
                        reservationRepository.findAll().filter(r -> "expired".equalsIgnoreCase(r.getStatus())).count()
                )
                .map(tuple -> new DashboardStatsDto(
                        tuple.getT1(), // easyboxes
                        tuple.getT2(), // compartments
                        tuple.getT3(), // total orders
                        tuple.getT4()  // expired orders
                ));
    }
}
