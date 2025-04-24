package com.example.network.repository;

import com.example.network.entity.Easybox;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface EasyboxRepository extends ReactiveCrudRepository<Easybox, Long> {
    // Custom query method (reactively)

    Mono<Easybox> findByAddressIgnoreCase(String address);
}
