package com.example.network.repository;

import com.example.network.model.Bakery;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BakeryRepository extends ReactiveCrudRepository<Bakery, Long> {

    Mono<Bakery> findByPhone(String phone);
    Flux<Bakery> findAllByOrderByIdDesc();
    @Query("""
        SELECT * FROM bakery
        WHERE (:name IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:phone IS NULL OR phone LIKE CONCAT('%', :phone, '%'))
        ORDER BY id DESC
        """)
    Flux<Bakery> findByFilters(@Param("name") String name, @Param("phone") String phone);

    @Query("""
        SELECT COUNT(*) FROM bakery
        WHERE (:name IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:phone IS NULL OR phone LIKE CONCAT('%', :phone, '%'))
        """)
    Mono<Long> countByFilters(@Param("name") String name, @Param("phone") String phone);

}
