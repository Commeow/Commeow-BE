package com.example.contentservice.domain;

import com.example.contentservice.dto.channel.ChannelRequestDto;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

class ChannelTest {

    private final Channel ON_CHANNEL = new Channel(1L, "유저1님의 방송", "유저1", "#1234", true);
    private final Channel OFF_CHANNEL = new Channel(1L, "유저1님의 방송", "유저1", "#1234", false);
    private final String UPDATE_TITLE = "update title";


    @BeforeEach
    public void setup() {

    }

    @Test
    @DisplayName("방송 시작 테스트")
    public void testChannelOn() {
        ChannelRequestDto requestDto = new ChannelRequestDto("");
        StepVerifier.create(Mono.just(OFF_CHANNEL.channelOn(requestDto)))
                .assertNext(updatedChannel -> {
                    Assertions.assertThat(updatedChannel.getOnAir()).isTrue();
                    Assertions.assertThat(updatedChannel.getTitle()).isEqualTo("");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("제목 변경 테스트")
    public void testUpdateTitleChannelOn() {
        ChannelRequestDto requestDto = new ChannelRequestDto(UPDATE_TITLE);
        StepVerifier.create(Mono.just(OFF_CHANNEL.channelOn(requestDto)))
                .assertNext(updatedChannel -> {
                    Assertions.assertThat(updatedChannel.getTitle()).isEqualTo(UPDATE_TITLE);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("방송 종료 테스트")
    public void testChannelOff() {
        StepVerifier.create(Mono.just(ON_CHANNEL.channelOff()))
                .assertNext(updatedChannel -> {
                    Assertions.assertThat(updatedChannel.getOnAir()).isFalse();
                })
                .verifyComplete();
    }
}