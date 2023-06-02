package com.example.contentservice.controller;

import com.example.contentservice.dto.ChatDto;
import com.example.contentservice.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @ConnectMapping
    public void onConnect(RSocketRequester requester, @Payload Object chattingAddress) {
        log.info("onConnect");
        chatService.onConnect(requester, (String)chattingAddress);
    }

    @MessageMapping("message")
    Mono<ChatDto> message(ChatDto chatDto) {
        log.info("message method");
        return chatService.message(chatDto);
    }

    @MessageMapping("send")
    void sendMessage(ChatDto chatDto) {
        log.info("send method");
        chatService.sendMessage(chatDto);
    }
}

