package com.example.network.controller;

import com.example.network.dto.*;
import com.example.network.model.Reservation;
import com.example.network.model.Compartment;
import com.example.network.repository.EasyboxRepository;
import com.example.network.repository.ReservationRepository;
import com.example.network.repository.CompartmentRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @GetMapping("/compartments-by-condition")
    public Mono<List<Map<String, Object>>> getCompartmentConditionRadarData() {
        return compartmentRepository.findAll()
                .collect(Collectors.groupingBy(Compartment::getEasyboxId))
                .map(map -> map.entrySet().stream().map(entry -> {
                    Long easyboxId = entry.getKey();
                    List<Compartment> compartments = entry.getValue();

                    Map<String, Object> result = new HashMap<>();
                    result.put("easybox", "Easybox " + easyboxId);
                    result.put("good", compartments.stream().filter(c -> "good".equalsIgnoreCase(c.getCondition())).count());
                    result.put("dirty", compartments.stream().filter(c -> "dirty".equalsIgnoreCase(c.getCondition())).count());
                    result.put("broken", compartments.stream().filter(c -> "broken".equalsIgnoreCase(c.getCondition())).count());

                    return result;
                }).collect(Collectors.toList()));
    }

    @GetMapping("/orders-status")
    public Flux<OrdersStatusDto> getOrdersStatus() {
        return reservationRepository.findAll()
                .groupBy(Reservation::getStatus)
                .flatMap(group -> group.count()
                        .map(count -> new OrdersStatusDto(group.key(), count))
                );
    }

    @GetMapping("/compartments-status")
    public Mono<CompartmentsStatusDto> getCompartmentsStatus() {
        return compartmentRepository.findAll()
                .collect(Collectors.groupingBy(Compartment::getStatus, Collectors.counting()))
                .map(map -> {
                    long free = map.getOrDefault("free", 0L);
                    long busy = map.getOrDefault("busy", 0L);
                    return new CompartmentsStatusDto(free, busy);
                });
    }
    @GetMapping("/orders-weekly")
    public Mono<List<OrdersWeeklyDto>> getOrdersWeekly() {
        LocalDate today = LocalDate.now();
        LocalDate weekLater = today.plusDays(6);

        return reservationRepository.findAll()
                .filter(r -> r.getDeliveryTime() != null)
                .filter(r -> {
                    LocalDate date = r.getDeliveryTime().toLocalDate();
                    return !date.isBefore(today) && !date.isAfter(weekLater);
                })
                .collect(Collectors.groupingBy(r -> r.getDeliveryTime().toLocalDate(), Collectors.counting()))
                .map(countsByDate -> {
                    List<OrdersWeeklyDto> result = new java.util.ArrayList<>();
                    for (int i = 0; i < 7; i++) {
                        LocalDate date = today.plusDays(i);
                        long count = countsByDate.getOrDefault(date, 0L);
                        result.add(new OrdersWeeklyDto(date.toString(), count));
                    }
                    return result;
                });
    }


}
