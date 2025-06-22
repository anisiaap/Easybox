package com.example.network.repository;

import com.example.network.model.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository
        extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByPhoneNumber(String phoneNumber);
    Flux<User> findAllByOrderByIdDesc(); // fallback if no createdAt field
}