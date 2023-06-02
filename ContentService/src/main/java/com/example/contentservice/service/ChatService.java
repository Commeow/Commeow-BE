package com.example.contentservice.service;

import com.example.contentservice.dto.ChatDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final Map<String, List<RSocketRequester>> participants = new ConcurrentHashMap<>();

    public void onConnect(RSocketRequester requester, String chattingAddress) {
        requester.rsocket()
                .onClose()
                .doFirst(() -> {
                    if (participants.containsKey(chattingAddress))
                        participants.get(chattingAddress).add(requester);
                    else
                        participants.put(chattingAddress, Collections.synchronizedList(new ArrayList<>(Arrays.asList(requester))));

                    log.info("Successfully connected to the socket");
                })
                .doOnError(error -> {
                    log.info(error.getMessage());
                })
                .doFinally(consumer -> {
                    log.info("Socket Connection Closed");
                    participants.get(chattingAddress).remove(requester);
                })
                .subscribe();
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

/* StringBuilder sb = new StringBuilder();
   for (Map.Entry<String, List<RSocketRequester>> entry : participants.entrySet()) {
            sb.append("ChattingAddress: " + entry.getKey() + "\n");
            for (RSocketRequester rSocketRequester : entry.getValue()) {
                  sb.append("requester: " + rSocketRequester + "\n");
            }
   }
   log.info(sb.toString()); */

