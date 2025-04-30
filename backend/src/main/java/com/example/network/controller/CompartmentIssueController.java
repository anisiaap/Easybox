package com.example.network.controller;

import com.example.network.repository.CompartmentRepository;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/app/compartments")
public class CompartmentIssueController {

    private final CompartmentRepository compartmentRepository;

    public CompartmentIssueController(CompartmentRepository compartmentRepository) {
        this.compartmentRepository = compartmentRepository;
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
}
