package com.example.contentservice.controller;

import com.example.contentservice.dto.channel.ChannelDetailResponseDto;
import com.example.contentservice.dto.channel.ChannelRequestDto;
import com.example.contentservice.dto.channel.ChannelResponseDto;
import com.example.contentservice.security.PrincipalUtil;
import com.example.contentservice.service.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;

@RestController
@RequestMapping("broadcasts")
@RequiredArgsConstructor
public class ChannelController {
    private final ChannelService channelService;
    private final PrincipalUtil principalUtil;


    @GetMapping
    public Mono<ResponseEntity<Flux<ChannelResponseDto>>> getAllOnAirChannels(){
        return channelService.getAllOnAirChannels();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ChannelDetailResponseDto>> getChannelDetail(@PathVariable("id") Long id){
        return channelService.getChannelDetail(id);
    }

    @PostMapping("/{id}/onair")
    public Mono<ResponseEntity<String>> startBroadcast(Mono<Principal> userDetails, @PathVariable("id") Long id, @RequestBody ChannelRequestDto channelRequestDto){
        return userDetails.flatMap(principal-> {
            return channelService.startBroadcast(principalUtil.getPrincipal(principal), id, channelRequestDto);
        });
    }

    @PostMapping("/{id}/offair")
    public Mono<ResponseEntity<String>> endBroadcast(Mono<Principal> userDetails, @PathVariable("id") Long id){
        return userDetails.flatMap(principal-> {
            return channelService.endBroadcast(principalUtil.getPrincipal(principal), id);
        });
    }
}
