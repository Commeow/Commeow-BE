package com.example.contentservice.service;

import com.example.contentservice.dto.mypage.ChannelInfoDto;
import com.example.contentservice.repository.ChannelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MyPageService {
    private final ChannelRepository channelRepository;

    public Mono<ResponseEntity<String>> changeInfo(String id, ChannelInfoDto channelInfoDto) {
        return channelRepository.findById(id)
                .switchIfEmpty(Mono.error(new NoSuchElementException("존재하지 않는 사용자입니다.")))
                .flatMap(channel -> {
                    return channelRepository.save(channel.changeInfo(channelInfoDto))
                            .thenReturn(ResponseEntity.ok("Success"));
                });
    }
}