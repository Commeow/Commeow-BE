package com.example.contentservice.service;

import com.example.contentservice.domain.Channel;
import com.example.contentservice.domain.Member;
import com.example.contentservice.domain.MemberRoleEnum;
import com.example.contentservice.dto.channel.StreamerCheckRequestDto;
import com.example.contentservice.repository.ChannelRepository;
import com.example.contentservice.repository.MemberRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.naming.AuthenticationException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChannelServiceTest {

    private final Long EXISTING_CHANNEL_ID = 1L;
    private final Long NON_EXISTING_CHANNEL_ID = 3L;
    private final Member VALID_MEMBER = new Member(EXISTING_CHANNEL_ID, "user1", "1234", "유저1", "streamkey", MemberRoleEnum.USER);
    private final String NOT_EXIST_MEMBER_NAME = "notUser";
    private final String INVALID_STREAM_KEY = "invalidStreamKey";

    private Channel EXISTING_CHANNEL;

    @InjectMocks
    private ChannelService channelService;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private MemberRepository memberRepository;

    @BeforeEach
    void setup() {
        EXISTING_CHANNEL = new Channel(1L, "유저1의 방송", "유저1", "#1234", false);
        Channel channel2 = new Channel(2L, "유저2의 방송", "유저2", "#5678", true);
        List<Channel> channels = Arrays.asList(EXISTING_CHANNEL, channel2);

        // 반환값 설정
        when(channelRepository.findById(1L)).thenReturn(Mono.just(EXISTING_CHANNEL));
        when(channelRepository.findById(2L)).thenReturn(Mono.just(channel2));
        when(channelRepository.findById(3L)).thenReturn(Mono.empty());
        when(channelRepository.findAllByOnAirTrue())
                .thenReturn(Flux.fromIterable(channels).filter(Channel::getOnAir));
    }

    @Test
    @DisplayName("방송 중인 채널 전체 조회 테스트")
    public void testAllOnAirChannels() {
        StepVerifier.create(
                channelService.getAllOnAirChannels()
        ).assertNext(responseEntity -> {
            Assertions.assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            Assertions.assertThat(responseEntity.getBody().toIterable()).hasSize(1);
        }).verifyComplete();
    }

    @Test
    @DisplayName("존재하는 채널 상세 조회 성공 테스트")
    public void testGetChannelDetail() {
        StepVerifier.create(
                channelService.getChannelDetail(EXISTING_CHANNEL_ID)
        ).assertNext(responseEntity -> {
            Assertions.assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            Assertions.assertThat(responseEntity.getBody().getChannelId()).isEqualTo(EXISTING_CHANNEL_ID);
            Assertions.assertThat(responseEntity.getBody().getStreamer()).isEqualTo(VALID_MEMBER.getNickname());
        }).verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 채널 상세 조회 실패 테스트")
    public void testNotExistChannelDetail() {
        StepVerifier.create(channelService.getChannelDetail(NON_EXISTING_CHANNEL_ID))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException
                ).verify();
    }

    @Test
    @DisplayName("스트림 키 일치 여부 테스트")
    public void testStreamKey() {
        when(memberRepository.findByNickname(VALID_MEMBER.getNickname())).thenReturn(Mono.just(VALID_MEMBER));
        StreamerCheckRequestDto requestDto = new StreamerCheckRequestDto(VALID_MEMBER.getStreamKey());
        StepVerifier.create(
                channelService.checkBroadcast(VALID_MEMBER.getNickname(), requestDto)
        ).expectNext(true).verifyComplete();
    }

    @Test
    @DisplayName("스트림 키 확인 시 존재하지 않는 회원 실패 테스트")
    public void testNotExistMemberStreamKey() {
        when(memberRepository.findByNickname(anyString())).thenReturn(Mono.empty());
        StreamerCheckRequestDto requestDto = new StreamerCheckRequestDto(INVALID_STREAM_KEY);
        StepVerifier.create(channelService.checkBroadcast(NOT_EXIST_MEMBER_NAME, requestDto)
                ).expectErrorMatches(throwable ->
                        throwable instanceof NoSuchElementException
                ).verify();
    }

    @Test
    @DisplayName("스트림 키 확인 시 스트림 키 불일치 실패 테스트")
    public void testNotMatchInfoStreamKey() {
        when(memberRepository.findByNickname(VALID_MEMBER.getNickname())).thenReturn(Mono.just(VALID_MEMBER));
        StreamerCheckRequestDto requestDto = new StreamerCheckRequestDto(INVALID_STREAM_KEY);
        StepVerifier.create(channelService.checkBroadcast(VALID_MEMBER.getNickname(), requestDto)
                ).expectErrorMatches(throwable ->
                    throwable instanceof AuthenticationException
                ).verify();
    }

    @Test
    @DisplayName("존재하는 채널 방송 시작 테스트")
    public void testExistChannelOnAir() {
        when(channelRepository.findByStreamer(VALID_MEMBER.getNickname())).thenReturn(Mono.just(EXISTING_CHANNEL));
        when(channelRepository.save(EXISTING_CHANNEL.channelOn())).thenReturn(Mono.just(EXISTING_CHANNEL.channelOn()));
        StepVerifier.create(
                channelService.startBroadcast(VALID_MEMBER.getNickname())
                ).expectNext(true).verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 채널 방송 시작 실패 테스트")
    public void testNotExistChannelOnAir() {
        when(channelRepository.findByStreamer(NOT_EXIST_MEMBER_NAME)).thenReturn(Mono.empty());
        StepVerifier.create(
                channelService.startBroadcast(NOT_EXIST_MEMBER_NAME)
                ).expectErrorMatches(throwable ->
                    throwable instanceof NoSuchElementException
                ).verify();
    }

    @Test
    @DisplayName("존재하는 채널 방송 종료 테스트")
    public void testExistChannelOffAir() {
        when(channelRepository.findByStreamer(VALID_MEMBER.getNickname())).thenReturn(Mono.just(EXISTING_CHANNEL.channelOn()));
        when(channelRepository.save(EXISTING_CHANNEL.channelOff())).thenReturn(Mono.just(EXISTING_CHANNEL.channelOff()));
        StepVerifier.create(
                channelService.endBroadcast(VALID_MEMBER.getNickname())
        ).expectNext(true).verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 채널 방송 종료 실패 테스트")
    public void testNotExistChannelOffAir() {
        when(channelRepository.findByStreamer(NOT_EXIST_MEMBER_NAME)).thenReturn(Mono.empty());
        StepVerifier.create(
                channelService.startBroadcast(NOT_EXIST_MEMBER_NAME)
        ).expectErrorMatches(throwable ->
                throwable instanceof NoSuchElementException
        ).verify();
    }
}