package com.example.contentservice.service;

import com.example.contentservice.domain.Points;
import com.example.contentservice.dto.ChatDto;
import com.example.contentservice.dto.point.DonationDto;
import com.example.contentservice.dto.point.DonationResponseDto;
import com.example.contentservice.repository.MemberRepository;
import com.example.contentservice.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final Map<String, List<RSocketRequester>> participants = new ConcurrentHashMap<>();
    private final Map<String, Sinks.Many<Integer>> participantCountSinks = new ConcurrentHashMap<>();
    private final PointRepository pointRepository;
    private final MemberRepository memberRepository;
    private final DatabaseClient databaseClient;

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

    public Mono<DonationResponseDto> donation(DonationDto donationDto) {
        return usePoint(donationDto);
    }

    public Mono<DonationResponseDto> usePoint(DonationDto donationDto) {
        log.info("usePoint 진입");
        if (donationDto.getStreamer().equals(donationDto.getNickname())) {
            log.info("본인 후원 안됨");
            return Mono.error(() -> new IllegalArgumentException("스트리머는 자신의 방송에 후원할 수 없습니다. 다른 스트리머를 응원해보세요!"));
        }

        if (donationDto.getPoints() <= 0) {
            log.info("0원 이하 후원 안됨");
            return Mono.error(() -> new IllegalArgumentException("0원 이하는 후원할 수 없습니다."));
        }

        return memberRepository.findByNickname(donationDto.getStreamer())
                .switchIfEmpty(Mono.error(() -> new NoSuchElementException("존재하지 않는 사용자입니다.")))
                .flatMap(streamer -> pointRepository.findByNickname(donationDto.getNickname())
                        .switchIfEmpty(Mono.error(() -> new NoSuchElementException("존재하지 않는 사용자입니다.")))
                        .flatMap(memberPoints -> {
                            if (memberPoints.getPoints() - donationDto.getPoints() < 0) {
                                return Mono.error(() -> new NoSuchElementException("포인트가 부족합니다."));
                            }

                            return databaseClient.sql("SELECT * FROM points WHERE user_id = :userId FOR UPDATE")
                                    .bind("userId", streamer.getUserId())
                                    .fetch()
                                    .one()
                                    .flatMap(row -> {
                                        Points updatedStreamerPoints = new Points(
                                                (Long) row.get("id"),
                                                (String) row.get("user_id"),
                                                (int) row.get("points")
                                        );
                                        updatedStreamerPoints.addPoints(donationDto.getPoints());
                                        return pointRepository.save(memberPoints.usePoints(donationDto.getPoints()))
                                                .flatMap(savedMemberPoints -> {
                                                    return pointRepository.save(updatedStreamerPoints)
                                                            .flatMap(res -> sendDonation(donationDto))
                                                            .thenReturn(DonationResponseDto.builder()
                                                                    .type(donationDto.getType())
                                                                    .nickname(donationDto.getNickname())
                                                                    .points(donationDto.getPoints())
                                                                    .remainPoints(savedMemberPoints.getPoints())
                                                                    .message(donationDto.getMessage())
                                                                    .build());
                                                });
                                    });
                        }));
    }

    public Mono<Void> sendDonation(DonationDto donationDto) {
        return Flux.fromIterable(participants.get(donationDto.getChattingAddress()))
                .flatMap(ea -> ea.route("")
                        .data(donationDto)
                        .send())
                .then();
    }
}
