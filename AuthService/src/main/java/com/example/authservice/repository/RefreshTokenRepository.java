package com.example.authservice.repository;

import com.example.authservice.domain.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private final ReactiveRedisTemplate<String, RefreshToken> reactiveRedisTemplate;

    public Mono<RefreshToken> findByUserId(String userId) {
        return reactiveRedisTemplate.opsForValue().get(userId);
    }

    public Mono<Boolean> existsByUserId(String userId) {
        return reactiveRedisTemplate.hasKey(userId);
    }

    public Mono<RefreshToken> save(RefreshToken refreshToken) {
        return reactiveRedisTemplate
                .opsForValue()
                .set(refreshToken.getUserId(), refreshToken)
                .then(reactiveRedisTemplate.expire(refreshToken.getUserId(), Duration.ofDays(14L)))
                .flatMap(success -> {
                    if (success) return Mono.just(refreshToken);
                    else return Mono.error(new RuntimeException("Redis 저장 중 Error 발생!"));
                });
    }

    public Mono<Long> deleteByUserId(String userId) {
        return reactiveRedisTemplate.delete(userId);
    }
}
