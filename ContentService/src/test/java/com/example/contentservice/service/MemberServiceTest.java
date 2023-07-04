package com.example.contentservice.service;

import com.example.contentservice.domain.Channel;
import com.example.contentservice.domain.Member;
import com.example.contentservice.domain.MemberRoleEnum;
import com.example.contentservice.domain.RefreshToken;
import com.example.contentservice.dto.member.LoginRequestDto;
import com.example.contentservice.dto.member.LoginResponseDto;
import com.example.contentservice.dto.member.SignupRequestDto;
import com.example.contentservice.dto.member.TokenDto;
import com.example.contentservice.jwt.JwtUtil;
import com.example.contentservice.repository.ChannelRepository;
import com.example.contentservice.repository.MemberRepository;
import com.example.contentservice.repository.RefreshTokenRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MemberServiceTest {

    private Member USER1;
    private TokenDto USER1_TOKEN;
    private RefreshToken USER1_REFRESHTOKEN;
    private RefreshToken USER1_UPDATEDTOKEN;

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @BeforeEach
    void setup() {
        USER1 = new Member(1L, "user1", "password1", "nickname1", "streamkey1", MemberRoleEnum.USER);
        USER1_TOKEN = new TokenDto("mockAccessToken", "mockRefreshToken");
        USER1_REFRESHTOKEN = new RefreshToken(1L, "user1_RefreshToken", "user1");
        USER1_UPDATEDTOKEN = new RefreshToken(2L, "user1_UpdatedToken", "user1");

        //반환값 설정
        when(memberRepository.existsByUserId("user1")).thenReturn(Mono.just(true));
        when(memberRepository.existsByNickname("nickname1")).thenReturn(Mono.just(true));
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void testSignUp() {
        SignupRequestDto signupRequestDto = new SignupRequestDto("user2", "password2", "nickname2");
        Member USER2 = new Member(2L, "user2", "password2", "nickname2", "streamKey2", MemberRoleEnum.USER);
        Channel USER2_CHANNEL = new Channel(2L, "title2", "streamer2", "chattingAddress2", false);

        when(memberRepository.existsByUserId("user2")).thenReturn(Mono.just(false));
        when(memberRepository.existsByNickname("nickname2")).thenReturn(Mono.just(false));
        when(passwordEncoder.encode("password1")).thenReturn("encodedPassword1");

        when(memberRepository.save(Mockito.any(Member.class))).thenReturn(Mono.just(USER2));
        when(channelRepository.save(Mockito.any(Channel.class))).thenReturn(Mono.just(USER2_CHANNEL));
        when(channelRepository.findById(2L)).thenReturn(Mono.just(USER2_CHANNEL));
        when(memberRepository.findById(2L)).thenReturn(Mono.just(USER2));

        StepVerifier.create(memberService.signup(signupRequestDto))
                .assertNext(responseEntity -> {
                    Assertions.assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                    Assertions.assertThat(responseEntity.getBody().equals(signupRequestDto.getNickname() + "님 회원 가입 완료 o(〃＾▽＾〃)o"));
                }).verifyComplete();

        memberRepository
                .findById(2L)
                .doOnNext(member -> Assertions.assertThat(member).isEqualTo(USER2))
                .block();

        channelRepository
                .findById(2L)
                .doOnNext(channel -> Assertions.assertThat(channel).isEqualTo(USER2_CHANNEL))
                .block();
    }

    @Test
    @DisplayName("회원가입 실패 테스트 : 중복된 아이디")
    void testDuplicatedIDSignUp() {
        SignupRequestDto signupRequestDto = new SignupRequestDto("user1", "password2", "nickname2");

        StepVerifier.create(memberService.signup(signupRequestDto))
                .expectErrorSatisfies(throwable -> {
                    Assertions.assertThat(throwable).isInstanceOf(RuntimeException.class);
                    Assertions.assertThat(throwable.getMessage()).isEqualTo("중복된 아이디입니다.");
                }).verify();
    }

    @Test
    @DisplayName("회원가입 실패 테스트 : 중복된 닉네임")
    void testDuplicatedNicknameSignUp() {
        SignupRequestDto signupRequestDto = new SignupRequestDto("user2", "password2", "nickname1");

        when(memberRepository.existsByUserId("user2")).thenReturn(Mono.just(false));

        StepVerifier.create(memberService.signup(signupRequestDto))
                .expectErrorSatisfies(throwable -> {
                    Assertions.assertThat(throwable).isInstanceOf(RuntimeException.class);
                    Assertions.assertThat(throwable.getMessage()).isEqualTo("중복된 닉네임입니다.");
                }).verify();
    }

    @Test
    @DisplayName("회원가입 실패 테스트 : 회원 정보 저장 실패")
    void testSaveMemberFailure() {
        SignupRequestDto signupRequestDto = new SignupRequestDto("user2", "password2", "nickname2");

        when(memberRepository.existsByUserId("user2")).thenReturn(Mono.just(false));
        when(memberRepository.existsByNickname("nickname2")).thenReturn(Mono.just(false));
        when(passwordEncoder.encode("password2")).thenReturn("encodedPassword2");
        when(memberRepository.save(Mockito.any(Member.class))).thenReturn(Mono.error(new RuntimeException("회원 정보 저장 오류")));

        StepVerifier.create(memberService.signup(signupRequestDto))
                .expectErrorSatisfies(throwable -> {
                    Assertions.assertThat(throwable).isInstanceOf(RuntimeException.class);
                    Assertions.assertThat(throwable.getMessage()).isEqualTo("회원 정보 저장 오류");
                }).verify();
    }

    @Test
    @DisplayName("회원가입 실패 테스트 : 채널 정보 저장 실패")
    void testSaveChannelFailure() {
        SignupRequestDto signupRequestDto = new SignupRequestDto("user2", "password2", "nickname2");
        Member USER2 = new Member(2L, "user2", "password2", "nickname2", "streamKey2", MemberRoleEnum.USER);

        when(memberRepository.existsByUserId("user2")).thenReturn(Mono.just(false));
        when(memberRepository.existsByNickname("nickname2")).thenReturn(Mono.just(false));
        when(passwordEncoder.encode("password1")).thenReturn("encodedPassword1");
        when(memberRepository.save(Mockito.any(Member.class))).thenReturn(Mono.just(USER2));
        when(channelRepository.save(Mockito.any(Channel.class))).thenReturn(Mono.error(new RuntimeException("회원 정보 저장 오류")));

        StepVerifier.create(memberService.signup(signupRequestDto))
                .expectErrorSatisfies(throwable -> {
                    Assertions.assertThat(throwable).isInstanceOf(RuntimeException.class);
                    Assertions.assertThat(throwable.getMessage()).isEqualTo("회원 정보 저장 오류");
                }).verify();
    }

    @Test
    @DisplayName("로그인 성공 테스트 : refresh 토큰이 존재할 때")
    void testLogin() {
        LoginRequestDto loginRequestDto = new LoginRequestDto("user1", "password1");

        when(memberRepository.findByUserId("user1")).thenReturn(Mono.just(USER1));
        when(passwordEncoder.matches("password1", "password1")).thenReturn(true);
        when(jwtUtil.createAllToken("user1", "nickname1")).thenReturn(USER1_TOKEN);

        when(refreshTokenRepository.findByUserId("user1")).thenReturn(Mono.just(USER1_REFRESHTOKEN));
        when(refreshTokenRepository.save(Mockito.any(RefreshToken.class))).thenReturn(Mono.just(USER1_UPDATEDTOKEN));
        when(refreshTokenRepository.findByUserId("user1")).thenReturn(Mono.just(USER1_UPDATEDTOKEN));

        HttpHeaders expectedHeaders = new HttpHeaders();
        expectedHeaders.add(JwtUtil.ACCESS_TOKEN, "mockAccessToken");
        expectedHeaders.add(JwtUtil.REFRESH_TOKEN, USER1_UPDATEDTOKEN.getRefreshToken());

        LoginResponseDto expectedResponse = new LoginResponseDto("user1", "nickname1", "streamKey1", 100);

        StepVerifier.create(memberService.login(loginRequestDto))
                .assertNext(responseEntity -> {
                    Assertions.assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                    Assertions.assertThat(responseEntity.getHeaders().equals(expectedHeaders));
                    Assertions.assertThat(responseEntity.getBody().equals(expectedResponse));
                }).verifyComplete();

        refreshTokenRepository
                .findByUserId("user1")
                .doOnNext(refreshToken -> Assertions.assertThat(refreshToken).isEqualTo(USER1_UPDATEDTOKEN))
                .block();
    }

    @Test
    @DisplayName("로그인 성공 테스트 : refresh 토큰이 없을 때")
    void testNotExistedRefreshTokenLogin() {
        LoginRequestDto loginRequestDto = new LoginRequestDto("user1", "password1");

        when(memberRepository.findByUserId("user1")).thenReturn(Mono.just(USER1));
        when(passwordEncoder.matches("password1", "password1")).thenReturn(true);
        when(jwtUtil.createAllToken("user1", "nickname1")).thenReturn(USER1_TOKEN);

        when(refreshTokenRepository.findByUserId("user1")).thenReturn(Mono.empty());
        when(refreshTokenRepository.save(Mockito.any(RefreshToken.class))).thenReturn(Mono.just(USER1_REFRESHTOKEN));
        when(refreshTokenRepository.findByUserId("user1")).thenReturn(Mono.just(USER1_REFRESHTOKEN));

        HttpHeaders expectedHeaders = new HttpHeaders();
        expectedHeaders.add(JwtUtil.ACCESS_TOKEN, "mockAccessToken");
        expectedHeaders.add(JwtUtil.REFRESH_TOKEN, USER1_REFRESHTOKEN.getRefreshToken());

        LoginResponseDto expectedResponse = new LoginResponseDto("user1", "nickname1", "streamKey1", 100);

        StepVerifier.create(memberService.login(loginRequestDto))
                .assertNext(responseEntity -> {
                    Assertions.assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                    Assertions.assertThat(responseEntity.getHeaders().equals(expectedHeaders));
                    Assertions.assertThat(responseEntity.getBody().equals(expectedResponse));
                }).verifyComplete();

        refreshTokenRepository
                .findByUserId("user1")
                .doOnNext(refreshToken -> Assertions.assertThat(refreshToken).isEqualTo(USER1_REFRESHTOKEN))
                .block();
    }

    @Test
    @DisplayName("로그인 실패 테스트 : 존재하지 않는 사용자")
    void testNotExistedUserLogin() {
        LoginRequestDto loginRequestDto = new LoginRequestDto("user2", "password1");

        when(memberRepository.findByUserId("user2")).thenReturn(Mono.error(new IllegalArgumentException("존재하지 않는 사용자입니다.")));

        StepVerifier.create(memberService.login(loginRequestDto))
                .expectErrorSatisfies(throwable -> {
                    Assertions.assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
                    Assertions.assertThat(throwable.getMessage()).isEqualTo("존재하지 않는 사용자입니다.");
                }).verify();
    }

    @Test
    @DisplayName("로그인 실패 테스트 : 일치하지 않는 비밀번호")
    void testIncorrectPasswordLogin() {
        LoginRequestDto loginRequestDto = new LoginRequestDto("user1", "password2");

        when(memberRepository.findByUserId("user1")).thenReturn(Mono.just(USER1));
        when(passwordEncoder.matches("password1", "password2")).thenReturn(false);

        StepVerifier.create(memberService.login(loginRequestDto))
                .expectErrorSatisfies(throwable -> {
                    Assertions.assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
                    Assertions.assertThat(throwable.getMessage()).isEqualTo("비밀번호가 틀렸습니다.");
                }).verify();
    }

    @Test
    @DisplayName("로그인 실패 테스트 : refresh 토큰이 존재할 때 저장 실패")
    void testExistedRefreshtokenSaveFailureLogin() {
        LoginRequestDto loginRequestDto = new LoginRequestDto("user1", "password1");

        when(memberRepository.findByUserId("user1")).thenReturn(Mono.just(USER1));
        when(passwordEncoder.matches("password1", "password1")).thenReturn(true);
        when(jwtUtil.createAllToken("user1", "nickname1")).thenReturn(USER1_TOKEN);

        when(refreshTokenRepository.findByUserId("user1")).thenReturn(Mono.just(USER1_REFRESHTOKEN));
        when(refreshTokenRepository.save(Mockito.any(RefreshToken.class))).thenReturn(Mono.error(new RuntimeException("Refresh Token 저장 중 오류 발생!")));

        StepVerifier.create(memberService.login(loginRequestDto))
                .expectErrorSatisfies(throwable -> {
                    Assertions.assertThat(throwable).isInstanceOf(RuntimeException.class);
                    Assertions.assertThat(throwable.getMessage()).isEqualTo("Refresh Token 저장 중 오류 발생!");
                }).verify();
    }

    @Test
    @DisplayName("로그인 실패 테스트 : refresh 토큰이 존재하지 않을때 저장 실패")
    void testNotExistedRefreshtokenSaveFailureLogin() {
        LoginRequestDto loginRequestDto = new LoginRequestDto("user1", "password1");

        when(memberRepository.findByUserId("user1")).thenReturn(Mono.just(USER1));
        when(passwordEncoder.matches("password1", "password1")).thenReturn(true);
        when(jwtUtil.createAllToken("user1", "nickname1")).thenReturn(USER1_TOKEN);

        when(refreshTokenRepository.findByUserId("user1")).thenReturn(Mono.empty());
        when(refreshTokenRepository.save(Mockito.any(RefreshToken.class))).thenReturn(Mono.error(new RuntimeException("Refresh Token 저장 중 오류 발생!")));

        StepVerifier.create(memberService.login(loginRequestDto))
                .expectErrorSatisfies(throwable -> {
                    Assertions.assertThat(throwable).isInstanceOf(RuntimeException.class);
                    Assertions.assertThat(throwable.getMessage()).isEqualTo("Refresh Token 저장 중 오류 발생!");
                }).verify();
    }

    @Test
    @DisplayName("로그아웃 성공 테스트")
    void testLogout() {
        when(refreshTokenRepository.deleteByUserId("user1")).thenReturn(Mono.just(1L));

        StepVerifier.create(memberService.logout("user1"))
                .assertNext(responseEntity -> {
                    Assertions.assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                    Assertions.assertThat(responseEntity.getBody().equals("삭제 성공 (∩^o^)⊃━☆"));
                }).verifyComplete();
    }

    @Test
    @DisplayName("로그아웃 실패 테스트 : 존재하지 않는 refresh 토큰")
    void testNotExistedRefreshTokenLogout() {
        when(refreshTokenRepository.deleteByUserId("user1")).thenReturn(Mono.just(0L));

        StepVerifier.create(memberService.logout("user1"))
                .assertNext(responseEntity -> {
                    Assertions.assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    Assertions.assertThat(responseEntity.getBody().equals("삭제 실패 o(TヘT∩)"));
                }).verifyComplete();
    }
}