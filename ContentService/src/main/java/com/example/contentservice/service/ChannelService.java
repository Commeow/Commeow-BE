package com.example.contentservice.service;

import com.example.contentservice.dto.channel.ChannelDetailResponseDto;
import com.example.contentservice.dto.channel.ChannelResponseDto;
import com.example.contentservice.dto.channel.StreamerCheckRequestDto;
import com.example.contentservice.repository.ChannelRepository;
import com.example.contentservice.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.naming.AuthenticationException;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class ChannelService {
    private final ChannelRepository channelRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Mono<ResponseEntity<Flux<ChannelResponseDto>>> getAllOnAirChannels() {
        Flux<ChannelResponseDto> channelResponseFlux = channelRepository
                .findAllByOnAirTrue()
                .map(ChannelResponseDto::new);

        return Mono.just(ResponseEntity.ok(channelResponseFlux));
    }

    @Transactional(readOnly = true)
    public Mono<ResponseEntity<ChannelDetailResponseDto>> getChannelDetail(Long id) {
        return channelRepository
                .findById(id)
                .switchIfEmpty(Mono.error(new NoSuchElementException("존재하지 않는 채널입니다.")))
                .map(channel -> ResponseEntity.ok(new ChannelDetailResponseDto(channel)));
    }

    public Mono<Boolean> checkBroadcast(String streamer, StreamerCheckRequestDto streamerCheckRequestDto) {
        String streamKey = streamerCheckRequestDto.getStreamKey();

        return memberRepository.findByNickname(streamer)
                .switchIfEmpty(Mono.error(new NoSuchElementException("존재하지 않는 사용자입니다.")))
                .filter(member -> member.getStreamKey().equals(streamKey))
                .switchIfEmpty(Mono.error(new AuthenticationException("스트림 키를 다시 확인해주세요.")))
                .flatMap(member -> {
                    return Mono.just(true);
                });
    }

    public Mono<Boolean> startBroadcast(String streamer) {
        return channelRepository
                .findByStreamer(streamer)
                .switchIfEmpty(Mono.error(new NoSuchElementException("존재하지 않는 채널입니다.")))
                .flatMap(channel -> {
                    return channelRepository.save(channel.channelOn())
                            .thenReturn(true);
                });
    }

    public Mono<Boolean> endBroadcast(String streamer) {
        return channelRepository
                .findByStreamer(streamer)
                .switchIfEmpty(Mono.error(new NoSuchElementException("존재하지 않는 채널입니다.")))
                .flatMap(channel -> {
                    return channelRepository.save(channel.channelOff())
                            .thenReturn(true);
                });
    }
}
