package com.example.contentservice.controller;

import com.example.contentservice.dto.channel.ChannelDetailResponseDto;
import com.example.contentservice.dto.channel.ChannelResponseDto;
import com.example.contentservice.dto.channel.StreamerCheckRequestDto;
import com.example.contentservice.service.ChannelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@RequestMapping("broadcasts")
@RequiredArgsConstructor
public class ChannelController {
    private final ChannelService channelService;

    @GetMapping
    public Mono<ResponseEntity<Flux<ChannelResponseDto>>> getAllOnAirChannels() {
        return channelService.getAllOnAirChannels();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ChannelDetailResponseDto>> getChannelDetail(@PathVariable("id") Long id) {
        return channelService.getChannelDetail(id);
    }

    @PostMapping("/{streamer}/check")
    public Mono<Boolean> checkStreamer(@PathVariable("streamer") String streamer, @RequestBody StreamerCheckRequestDto streamerCheckRequestDto) {
        return channelService.checkBroadcast(streamer, streamerCheckRequestDto);
    }

    @PostMapping("/{streamer}/onair")
    public Mono<Boolean> startBroadcast(@PathVariable("streamer") String streamer) {
        return channelService.startBroadcast(streamer);
    }

    @PostMapping("/{streamer}/offair")
    public Mono<Boolean> endBroadcast(@PathVariable("streamer") String streamer) {
        return channelService.endBroadcast(streamer);
    }
}
