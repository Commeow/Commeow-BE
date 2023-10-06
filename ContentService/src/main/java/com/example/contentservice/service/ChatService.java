package com.example.contentservice.service;

import com.example.contentservice.domain.Points;
import com.example.contentservice.dto.ChatDto;
import com.example.contentservice.dto.point.DonationDto;
import com.example.contentservice.dto.point.DonationResponseDto;
import com.example.contentservice.repository.MemberRepository;
import com.example.contentservice.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final Map<String, List<RSocketRequester>> participants = new ConcurrentHashMap<>();
    private final Map<String, Sinks.Many<Integer>> participantCountSinks = new ConcurrentHashMap<>();
    private final PointRepository pointRepository;
    private final MemberRepository memberRepository;
    private final RedissonClient redissonClient;

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
        String lockKey = "POINT_LOCK_" + donationDto.getStreamer();
        final RLock lock = redissonClient.getLock(lockKey);

        try {
            if (!lock.tryLock(3, TimeUnit.SECONDS)) {
                throw new IllegalArgumentException("락을 얻지 못했습니다.");
            }

            return memberRepository.findByNickname(donationDto.getStreamer())
                    .switchIfEmpty(Mono.error(() -> new NoSuchElementException("존재하지 않는 사용자입니다.")))
                    .flatMap(streamer -> pointRepository.findByNickname(donationDto.getNickname())
                            .switchIfEmpty(Mono.error(new NoSuchElementException("포인트가 부족합니다.")))
                            .flatMap(memberPoints -> {
                                if (donationDto.getStreamer().equals(donationDto.getNickname())) {
                                    return Mono.error(() -> new IllegalArgumentException("스트리머는 자신의 방송에 후원할 수 없습니다. 다른 스트리머를 응원해보세요!"));
                                }

                                if (donationDto.getPoints() <= 0) {
                                    return Mono.error(() -> new IllegalArgumentException("0원 이하는 후원할 수 없습니다."));
                                }

                                if (memberPoints.getPoints() - donationDto.getPoints() < 0) {
                                    return Mono.error(() -> new NoSuchElementException("포인트가 부족합니다."));
                                }

                                return pointRepository.save(memberPoints.usePoints(donationDto.getPoints()))
                                        .flatMap(savedMemberPoints -> pointRepository.findByUserId(streamer.getUserId())
                                                .flatMap(streamerPoints -> {
                                                    Points updatedStreamerPoints = streamerPoints.addPoints(donationDto.getPoints());
                                                    return pointRepository.save(updatedStreamerPoints)
                                                            .flatMap(res -> sendDonation(donationDto))
                                                            .thenReturn(DonationResponseDto.builder()
                                                                    .type(donationDto.getType())
                                                                    .nickname(donationDto.getNickname())
                                                                    .points(donationDto.getPoints())
                                                                    .remainPoints(savedMemberPoints.getPoints())
                                                                    .message(donationDto.getMessage())
                                                                    .build());
                                                }));
                            }))
                    .doFinally(signalType -> {
                        if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                            log.info("doFinally: lock 해제");
                            lock.unlock();
                        }
                    });
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                log.info("finally: lock 해제");
                lock.unlock();
            }
        }
    }

    public Mono<Void> sendDonation(DonationDto donationDto) {
        return Flux.fromIterable(participants.get(donationDto.getChattingAddress()))
                .flatMap(ea -> ea.route("")
                        .data(donationDto)
                        .send())
                .then();
    }
}
