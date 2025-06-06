package com.example.network.service;

import com.example.network.model.User;
import com.example.network.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepo;

    public Mono<User> getOrCreate(String phone) {
        return userRepo.findByPhoneNumber(phone)
                .switchIfEmpty(
                        userRepo.save(new User(null, null, phone, null))
                )
                /* handles race when two requests try to insert
                   the same phone concurrently */
                .onErrorResume(DuplicateKeyException.class,
                        ex -> userRepo.findByPhoneNumber(phone));
    }
}
