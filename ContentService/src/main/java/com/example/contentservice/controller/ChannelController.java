package com.example.contentservice.controller;

import com.example.contentservice.domain.Member;
import com.example.contentservice.dto.channel.ChannelDetailResponseDto;
import com.example.contentservice.dto.channel.ChannelRequestDto;
import com.example.contentservice.dto.channel.ChannelResponseDto;
import com.example.contentservice.security.UserDetailsImpl;
import com.example.contentservice.service.ChannelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("broadcasts")
@RequiredArgsConstructor
public class ChannelController {
    private final ChannelService channelService;

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
            return channelService.startBroadcast(getMemberFromPrincipal(principal), id, channelRequestDto);
        });
    }

    @PostMapping("/{id}/offair")
    public Mono<ResponseEntity<String>> endBroadcast(Mono<Principal> userDetails, @PathVariable("id") Long id){
        return userDetails.flatMap(principal-> {
            return channelService.endBroadcast(getMemberFromPrincipal(principal), id);
        });
    }

    private Member getMemberFromPrincipal(Principal principal){
        if (principal instanceof Authentication) {
            Authentication authentication = (Authentication) principal;
            Object principalObject = authentication.getPrincipal();
            if (principalObject instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) principalObject;
                return userDetails.getMember();
            }
        }
        throw new RuntimeException("당신 누구야! Σ(っ °Д °;)っ");
    }
}
