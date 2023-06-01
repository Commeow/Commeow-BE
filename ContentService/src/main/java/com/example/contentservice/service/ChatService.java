package com.example.contentservice.service;

import com.example.contentservice.dto.ChatDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final Map<String, List<RSocketRequester>> participants = new ConcurrentHashMap<>();

    public void onConnect(RSocketRequester requester, String chattingAddress) {
        log.info("추출중........");
        log.info("onConnect에 들어온걸 환영한다,,,,,");
        requester.rsocket()
                .onClose()
                .doFirst(() -> {
                    log.info("당신은 doooFirst...중.......");
//                    CLIENTS.add(requester);
                    if(participants.containsKey(chattingAddress))
                        participants.get(chattingAddress).add(requester);
                    else participants.put(chattingAddress, new ArrayList<>(Arrays.asList(requester)));

                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, List<RSocketRequester>> entry : participants.entrySet()) {
                        sb.append("key: " + entry.getKey() + "\n");
                        for (RSocketRequester rSocketRequester : entry.getValue()) {
                            sb.append("requester: " + rSocketRequester + "\n");
                        }
                    }
                    log.info(sb.toString());
                })
                .doOnError(error -> {
                    log.info("당신은 doOnError...중.......");
                    log.info(error.getMessage());
                })
                .doFinally(consumer -> {
                    log.info("당신은 doFinally...중.......");
//                    CLIENTS.remove(requester);
                    participants.get(chattingAddress).remove(requester);
                })
                .subscribe();
        log.info("subscribe..........완.....료");
    }

    public Mono<ChatDto> message(ChatDto chatDto) {
        this.sendMessage(chatDto);
        return Mono.just(chatDto);
    }

    public void sendMessage(ChatDto chatDto) {
        Flux.fromIterable(participants.get(chatDto.getChattingAddress()))
                .doOnNext(ea -> {
                    ea.route("")
                            .data(chatDto)
                            .send()
                            .subscribe();
                })
                .subscribe();
    }
}