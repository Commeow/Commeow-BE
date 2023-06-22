package com.example.contentservice.service;

import com.example.contentservice.dto.ChatDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final Map<String, List<RSocketRequester>> participants = new ConcurrentHashMap<>();
    private final Map<String, Sinks.Many<Integer>> participantCountSinks = new ConcurrentHashMap<>();

    public void onConnect(RSocketRequester requester, String chattingAddress) {
        requester.rsocket()
                .onClose()
                .doFirst(() -> {
                    if (participants.containsKey(chattingAddress))
                        participants.get(chattingAddress).add(requester);
                    else
                        participants.put(chattingAddress, Collections.synchronizedList(new ArrayList<>(Arrays.asList(requester))));

                    updateParticipantCount(chattingAddress);
                })
                .doOnError(error -> {
                    log.info(error.getMessage());
                })
                .doFinally(consumer -> {
                    participants.get(chattingAddress).remove(requester);
                    updateParticipantCount(chattingAddress);
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

    public Flux<Integer> stream(String chattingAddress) {
        String address = chattingAddress.substring(1, chattingAddress.length() - 1);
        return participantCountSinks.get(address).asFlux();
    }

    private void updateParticipantCount(String chattingAddress) {
        List<RSocketRequester> participantsList = participants.get(chattingAddress);
        int count = participantsList.size();
        participantCountSinks.computeIfAbsent(chattingAddress, key -> Sinks.many().replay().latest())
                .tryEmitNext(count);
    }
}
