package com.example.network.repository;

import com.example.network.model.Easybox;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface EasyboxRepository extends ReactiveCrudRepository<Easybox, Long> {

    Mono<Easybox> findByAddressIgnoreCase(String address);

    Mono<Easybox> findByClientId(String clientId);
}
