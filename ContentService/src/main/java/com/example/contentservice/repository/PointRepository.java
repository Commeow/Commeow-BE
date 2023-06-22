package com.example.contentservice.repository;

import com.example.contentservice.domain.Points;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PointRepository extends ReactiveCrudRepository<Points, Long> {
    Mono<Points> findByUserId(String userId);
}
