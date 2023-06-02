package com.example.contentservice.service;

import com.example.contentservice.domain.Member;
import com.example.contentservice.dto.channel.ChannelDetailResponseDto;
import com.example.contentservice.dto.channel.ChannelRequestDto;
import com.example.contentservice.dto.channel.ChannelResponseDto;
import com.example.contentservice.repository.ChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Transactional
public class ChannelService {
    private final ChannelRepository channelRepository;

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
                .switchIfEmpty(Mono.error(new RuntimeException("없는 방송이다! Σ(っ °Д °;)っ")))
                .map(channel -> ResponseEntity.ok(new ChannelDetailResponseDto(channel)));
    }

    public Mono<ResponseEntity<String>> startBroadcast(Member member, Long id, ChannelRequestDto channelRequestDto) {
        return channelRepository
                .findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("없는 방송이다! Σ(っ °Д °;)っ")))
                .flatMap(channel -> {
                    if (!channel.getStreamer().equals(member.getNickname()))
                        return Mono.error(new RuntimeException("권한이 없잖아! (╬▔皿▔)╯"));
                    
                    // 추후에 streamKey 체크!

                    if (channelRequestDto.getTitle().trim().equals(""))
                        channelRequestDto.updateTitle(member.getNickname() + "님의 방송 (^・ω・^ ) <( Commeow! )");

                    return channelRepository.save(channel.channelOn(channelRequestDto));
                })
                .thenReturn(ResponseEntity.ok("방송 시작 (。・・)ノ <(Hi)"));
    }

    public Mono<ResponseEntity<String>> endBroadcast(Member member, Long id) {
        return channelRepository
                .findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("없는 방송이다! Σ(っ °Д °;)っ")))
                .flatMap(channel -> {
                    if (!channel.getStreamer().equals(member.getNickname()))
                        return Mono.error(new RuntimeException("권한이 없잖아! (╬▔皿▔)╯"));

                    return channelRepository.save(channel.channelOff());
                })
                .thenReturn(ResponseEntity.ok("방송 종료 (。・・)ノ <(BYE)"));
    }
}
