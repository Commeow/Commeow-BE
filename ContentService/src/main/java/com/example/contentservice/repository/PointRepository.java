package com.example.contentservice.repository;

import com.example.contentservice.domain.Points;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PointRepository extends ReactiveCrudRepository<Points, Long> {
    Mono<Points> findByUserId(String userId);

    @Query("SELECT p.* FROM points p JOIN member m ON p.user_id = m.user_id WHERE m.nickname = :nickname")
    Mono<Points> findByNickname(String nickname);
}
