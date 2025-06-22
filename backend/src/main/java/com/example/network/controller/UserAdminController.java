// src/main/java/com/example/network/controller/UserAdminController.java
package com.example.network.controller;

import com.example.network.model.User;
import com.example.network.exception.NotFoundException;
import com.example.network.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final UserRepository userRepository;

    public UserAdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public Flux<User> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        return userRepository.findAllByOrderByIdDesc()
                .skip((long) page * size)
                .take(size);
    }
    // GET total count of users (for frontend to compute total pages)
    @GetMapping("/count")
    public Mono<Long> getUserCount() {
        return userRepository.count();
    }
    @PostMapping
    public Mono<User> createUser(@RequestBody User user) {
        return userRepository.save(user);
    }

    @GetMapping("/{id}")
    public Mono<User> getUserById(@PathVariable Long id) {
        return userRepository.findById(id);
    }

    @PutMapping("/{id}")
    public Mono<User> updateUser(@PathVariable Long id, @RequestBody User userUpdate) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("User not found")))
                .flatMap(user -> {
                    if (!user.getVersion().equals(userUpdate.getVersion())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Entity was modified by someone else."));
                    }
                    user.setName(userUpdate.getName());
                    user.setPhoneNumber(userUpdate.getPhoneNumber());
                    return userRepository.save(user);
                });
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long version = body.get("version") instanceof Number ? ((Number) body.get("version")).longValue() : null;
        if (version == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Version is required."));
        }

        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("User not found")))
                .flatMap(user -> {
                    if (!user.getVersion().equals(version)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "User was modified by someone else."));
                    }
                    return userRepository.delete(user);
                });
    }

}
