package com.example.contentservice.repository;

import com.example.contentservice.domain.Channel;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChannelRepository extends ReactiveCrudRepository<Channel, Long> {
    Flux<Channel> findAllByOnAirTrue();
    Mono<Channel> findById(Long id);
}
