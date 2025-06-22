package com.example.network.repository;

import com.example.network.model.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository
        extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByPhoneNumber(String phoneNumber);
    Flux<User> findAllByOrderByIdDesc(); // fallback if no createdAt field
    @Query("""
        SELECT * FROM users
        WHERE (:name IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:phone IS NULL OR phone_number LIKE CONCAT('%', :phone, '%'))
        ORDER BY id DESC
        """)
    Flux<User> findByFilters(@Param("name") String name, @Param("phone") String phone);

    @Query("""
        SELECT COUNT(*) FROM users
        WHERE (:name IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:phone IS NULL OR phone_number LIKE CONCAT('%', :phone, '%'))
        """)
    Mono<Long> countByFilters(@Param("name") String name, @Param("phone") String phone);

}