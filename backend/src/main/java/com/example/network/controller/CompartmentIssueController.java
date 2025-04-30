package com.example.network.controller;

import com.example.network.entity.Easybox;
import com.example.network.entity.Reservation;
import com.example.network.repository.CompartmentRepository;
import com.example.network.repository.EasyboxRepository;
import com.example.network.repository.ReservationRepository;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/app/compartments")
public class CompartmentIssueController {

    private final CompartmentRepository compartmentRepository;
    private final ReservationRepository reservationRepository;
    private final EasyboxRepository easyboxRepository;
    public CompartmentIssueController(CompartmentRepository compartmentRepository, ReservationRepository reservationRepository, EasyboxRepository easyboxRepository) {
        this.compartmentRepository = compartmentRepository;
        this.reservationRepository = reservationRepository;
        this.easyboxRepository = easyboxRepository;
    }

    @PostMapping("/{id}/report-condition")
    public Mono<Void> reportCondition(@PathVariable Long id, @RequestParam String issue) {
        return compartmentRepository.findById(id)
                .flatMap(comp -> {
                    if ("broken".equalsIgnoreCase(issue) || "dirty".equalsIgnoreCase(issue)) {
                        comp.setCondition(issue.toLowerCase());
                        return compartmentRepository.save(comp).then();
                    } else {
                        return Mono.error(new IllegalArgumentException("Invalid issue type"));
                    }
                });
    }

    @PostMapping("/compartments/{compartmentId}/report-and-reevaluate")
    public Mono<Reservation> reportAndReevaluate(
            @PathVariable Long compartmentId,
            @RequestParam String issue,
            @RequestParam Long reservationId
    ) {
        return reservationRepository.findById(reservationId)
                .flatMap(order -> {
                    // Step 1: mark compartment as dirty/broken
                    return compartmentRepository.findById(compartmentId)
                            .flatMap(comp -> {
                                comp.setCondition(issue.toLowerCase());
                                return compartmentRepository.save(comp).thenReturn(order);
                            });
                })
                .flatMap(order -> {
                    // Step 2: find another compartment
                    return easyboxRepository.findById(order.getEasyboxId())
                            .flatMap(box -> findReplacementCompartment(
                                            box,
                                            order.getReservationStart(),
                                            order.getReservationEnd(),
                                            order.getCompartmentId(), // ignore current comp
                                            order.getId()
                                    )
                                            .flatMap(newCompId -> {
                                                order.setCompartmentId(newCompId);
                                                return reservationRepository.save(order); // Step 3: swap to new comp
                                            })
                                            .switchIfEmpty(Mono.defer(() -> {
                                                order.setStatus("canceled");
                                                return reservationRepository.save(order); // Step 4: cancel if no alt found
                                            }))
                            );
                });
    }
    private Mono<Long> findReplacementCompartment(
            Easybox box,
            LocalDateTime start,
            LocalDateTime end,
            Long excludeCompartmentId,
            Long reservationId
    ) {
        return compartmentRepository.findByEasyboxId(box.getId())
                .filter(c -> !"broken".equalsIgnoreCase(c.getCondition()) && !"dirty".equalsIgnoreCase(c.getCondition()))
                .filter(c -> !"busy".equalsIgnoreCase(c.getStatus()))
                .filter(c -> !c.getId().equals(excludeCompartmentId))
                .concatMap(c ->
                        reservationRepository.findByCompartmentId(c.getId())
                                .filter(r -> !"canceled".equalsIgnoreCase(r.getStatus()))
                                .filter(r -> r.getReservationStart().isBefore(end) && r.getReservationEnd().isAfter(start))
                                .hasElements()
                                .map(hasConflict -> !hasConflict)
                                .filter(canUse -> canUse)
                                .map(ok -> c.getId())
                )
                .next();
    }

}
