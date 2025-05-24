package com.example.network.repository;

import com.example.network.entity.Bakery;
import com.example.network.entity.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BakeryRepository extends ReactiveCrudRepository<Bakery, Long> {

    Mono<Bakery> findByPhone(String phone);
    Flux<Bakery> findAllByOrderByIdDesc();
}
