package com.example.network.repository;

import com.example.network.entity.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository
        extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByPhoneNumber(String phoneNumber);
}