package com.example.contentservice.service;

import com.example.contentservice.domain.Channel;
import com.example.contentservice.domain.Member;
import com.example.contentservice.domain.MemberRoleEnum;
import com.example.contentservice.dto.channel.ChannelRequestDto;
import com.example.contentservice.repository.ChannelRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChannelServiceTest {

    private final Long EXISTING_CHANNEL_ID = 1L;
    private final Long NON_EXISTING_CHANNEL_ID = 3L;
    private final Member VALID_MEMBER = new Member(EXISTING_CHANNEL_ID, "user1", "1234", "유저1", "streamkey", MemberRoleEnum.USER);
    private final Member INVALID_MEMBER = new Member(NON_EXISTING_CHANNEL_ID, "user3", "1234", "유저3", "streamkey", MemberRoleEnum.USER);
    private Channel EXISTING_CHANNEL;

    @InjectMocks
    private ChannelService channelService;

    @Mock
    private ChannelRepository channelRepository;

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
        StepVerifier.create(
                        channelService.getChannelDetail(NON_EXISTING_CHANNEL_ID)
                )
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException
                ).verify();
    }

    @Test
    @DisplayName("존재하는 채널 방송 시작 성공 테스트")
    public void testExistChannelOnAir() {
        ChannelRequestDto channelRequestDto = new ChannelRequestDto("");
        Channel updatedChannel = new Channel(EXISTING_CHANNEL_ID, "유저1의 방송", "유저1", "#1234", true);

        when(channelRepository.save(Mockito.any(Channel.class))).thenReturn(Mono.just(updatedChannel));

        StepVerifier.create(
                channelService.startBroadcast(VALID_MEMBER, EXISTING_CHANNEL_ID, channelRequestDto)
        ).assertNext(responseEntity -> {
            Assertions.assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        }).verifyComplete();

        channelRepository.findById(EXISTING_CHANNEL_ID)
                .map(Channel::getOnAir)
                .doOnNext(isOnAir -> Assertions.assertThat(isOnAir).isTrue())
                .block();
    }

    @Test
    @DisplayName("방송 시작 시 제목 변경 테스트")
    public void testChannelTitleChange() {
        ChannelRequestDto channelRequestDto = new ChannelRequestDto("new title");
        Channel updatedChannel = new Channel(EXISTING_CHANNEL_ID, "유저1의 방송", "유저1", "#1234", true);

        when(channelRepository.save(Mockito.any(Channel.class))).thenReturn(Mono.just(updatedChannel));

        channelService.startBroadcast(VALID_MEMBER, EXISTING_CHANNEL_ID, channelRequestDto).block();

        channelRepository.findById(EXISTING_CHANNEL_ID)
                .map(Channel::getTitle)
                .doOnNext(title -> Assertions.assertThat(title).isEqualTo("new title"))
                .block();
    }

    @Test
    @DisplayName("존재하지 않는 채널 방송 시작 실패 테스트")
    public void testNotExistChannelOnAir() {
        ChannelRequestDto channelRequestDto = new ChannelRequestDto("");

        StepVerifier.create(
                channelService.startBroadcast(INVALID_MEMBER, NON_EXISTING_CHANNEL_ID, channelRequestDto)
        ).expectErrorMatches(throwable ->
                throwable instanceof RuntimeException
        ).verify();
    }

    @Test
    @DisplayName("존재하는 채널 방송 시작 권한 실패 테스트")
    public void testExistChannelOnAirNoAuthority() {
        ChannelRequestDto channelRequestDto = new ChannelRequestDto("");

        StepVerifier.create(
                        channelService.startBroadcast(INVALID_MEMBER, EXISTING_CHANNEL_ID, channelRequestDto)
                )
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException
                ).verify();
    }

    @Test
    @DisplayName("존재하는 채널 방송 종료 성공 테스트")
    public void testExistChannelOffAir() {
        Channel updatedChannel = new Channel(EXISTING_CHANNEL_ID, "유저1의 방송", "유저1", "#1234", false);

        when(channelRepository.save(Mockito.any(Channel.class))).thenReturn(Mono.just(updatedChannel));

        StepVerifier.create(
                channelService.endBroadcast(VALID_MEMBER, EXISTING_CHANNEL_ID)
        ).assertNext(responseEntity -> {
            Assertions.assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        }).verifyComplete();

        channelRepository.findById(EXISTING_CHANNEL_ID)
                .map(Channel::getOnAir)
                .doOnNext(isOnAir -> Assertions.assertThat(isOnAir).isFalse())
                .block();
    }

    @Test
    @DisplayName("존재하지 않는 채널 방송 종료 실패 테스트")
    public void testNotExistChannelOffAir() {
        StepVerifier.create(
                channelService.endBroadcast(INVALID_MEMBER, NON_EXISTING_CHANNEL_ID)
        ).expectErrorMatches(throwable ->
                throwable instanceof RuntimeException
        ).verify();
    }

    @Test
    @DisplayName("존재하는 채널 방송 종료 권한 실패 테스트")
    public void testExistChannelOffAirNoAuthority() {
        StepVerifier.create(
                channelService.endBroadcast(INVALID_MEMBER, EXISTING_CHANNEL_ID)
        ).expectErrorMatches(throwable ->
                throwable instanceof RuntimeException
        ).verify();
    }
}