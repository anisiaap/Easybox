package com.example.network.controller;

import com.example.network.exception.NotFoundException;
import com.example.network.model.Bakery;
import com.example.network.exception.InvalidRequestException;
import com.example.network.repository.BakeryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/bakeries")
public class BakeryAdminController {

    private final BakeryRepository bakeryRepository;

    public BakeryAdminController(BakeryRepository bakeryRepository) {
        this.bakeryRepository = bakeryRepository;
    }

    // GET all bakeries
    @GetMapping
    public Flux<Bakery> getAllBakeries(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return bakeryRepository.findAllByOrderByIdDesc()
                .skip((long) page * size)
                .take(size);
    }
    @GetMapping("/count")
    public Mono<Long> getUserCount() {
        return bakeryRepository.count();
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

    // PUT update existing bakery
    @PutMapping("/{id}")
    public Mono<Bakery> updateBakery(@PathVariable Long id, @RequestBody Bakery bakery) {
        return bakeryRepository.findById(id)
                .switchIfEmpty(Mono.error(new InvalidRequestException("Bakery not found")))
                .flatMap(existing -> {
                    if (!existing.getVersion().equals(bakery.getVersion())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Entity was modified by someone else."));
                    }

                    existing.setName(bakery.getName());
                    existing.setPhone(bakery.getPhone());
                    existing.setPluginInstalled(bakery.getPluginInstalled());
                    existing.setToken(bakery.getToken());
                    return bakeryRepository.save(existing);
                });
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long version = body.get("version") instanceof Number ? ((Number) body.get("version")).longValue() : null;
        if (version == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Version is required."));
        }

        return bakeryRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Bakery not found")))
                .flatMap(user -> {
                    if (!user.getVersion().equals(version)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Bakery was modified by someone else."));
                    }
                    return bakeryRepository.delete(user);
                });
    }


}
