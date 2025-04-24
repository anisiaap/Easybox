package com.example.network.controller;

import com.example.network.entity.Bakery;
import com.example.network.repository.BakeryRepository;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/bakeries")
public class BakeryAdminController {

    private final BakeryRepository bakeryRepository;

    public BakeryAdminController(BakeryRepository bakeryRepository) {
        this.bakeryRepository = bakeryRepository;
    }

    // GET all bakeries
    @GetMapping
    public Flux<Bakery> getAllBakeries() {
        return bakeryRepository.findAll();
    }

    // GET one bakery by ID
    @GetMapping("/{id}")
    public Mono<Bakery> getOneBakery(@PathVariable Long id) {
        return bakeryRepository.findById(id);
    }

    // POST create new bakery
    @PostMapping
    public Mono<Bakery> createBakery(@RequestBody Bakery bakery) {
        return bakeryRepository.save(bakery);
    }
}
