package com.example.authservice.repository;

import com.example.authservice.domain.Member;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface MemberRepository extends ReactiveCrudRepository<Member, Long> {
    Mono<Member> findByUserId(String userId);
    Mono<Boolean> existsByUserId(String userId);
}
