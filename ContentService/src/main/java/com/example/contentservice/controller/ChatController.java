package com.example.contentservice.controller;

import com.example.contentservice.dto.ChatDto;
import com.example.contentservice.dto.point.DonationDto;
import com.example.contentservice.dto.point.DonationResponseDto;
import com.example.contentservice.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @ConnectMapping
    public void onConnect(RSocketRequester requester, @Payload Object chattingAddress) {
        chatService.onConnect(requester, (String) chattingAddress);
    }

    @MessageMapping("message")
    Mono<ChatDto> message(ChatDto chatDto) {
        return chatService.message(chatDto);
    }

    @MessageMapping("send")
    void sendMessage(ChatDto chatDto) {
        chatService.sendMessage(chatDto);
    }

    @MessageMapping("counting")
    Flux<Integer> stream(String chattingAddress) {
        return chatService.stream(chattingAddress);
    }

    @MessageMapping("donation")
    Mono<DonationResponseDto> donation(DonationDto donationDto) {
        return chatService.donation(donationDto);
    }
}

