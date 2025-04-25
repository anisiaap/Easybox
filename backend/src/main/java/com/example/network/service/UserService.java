package com.example.network.service;

import com.example.network.entity.User;
import com.example.network.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepo;

    public Mono<User> getOrCreate(String phone, String name) {
        return userRepo.findByPhoneNumber(phone)
                .switchIfEmpty(
                        userRepo.save(new User(null, name, phone))
                )
                /* handles race when two requests try to insert
                   the same phone concurrently */
                .onErrorResume(DuplicateKeyException.class,
                        ex -> userRepo.findByPhoneNumber(phone));
    }
}
