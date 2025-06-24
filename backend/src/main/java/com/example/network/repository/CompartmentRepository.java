package com.example.network.repository;

import com.example.network.model.Compartment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface CompartmentRepository extends ReactiveCrudRepository<Compartment, Long> {

    Flux<Compartment> findByEasyboxId(Long easyboxId);
}
