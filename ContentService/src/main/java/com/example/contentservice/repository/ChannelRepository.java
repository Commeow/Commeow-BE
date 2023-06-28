package com.example.contentservice.repository;

import com.example.contentservice.domain.Channel;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ChannelRepository extends ReactiveCrudRepository<Channel, Long> {
    Flux<Channel> findAllByOnAirTrue();
    Mono<Channel> findByStreamer(String streamer);
    @Query("SELECT c.* FROM channel c JOIN member m ON c.streamer = m.nickname WHERE m.user_id = :id")
    Mono<Channel> findById(String id);
}
