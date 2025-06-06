package com.example.network.repository;

import com.example.network.model.Compartment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface CompartmentRepository extends ReactiveCrudRepository<Compartment, Long> {

    // Optionally define queries if you want to look up compartments by easyboxId:
    Flux<Compartment> findByEasyboxId(Long easyboxId);
}
