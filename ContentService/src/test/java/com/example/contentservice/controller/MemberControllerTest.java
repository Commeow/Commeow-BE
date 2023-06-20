package com.example.contentservice.controller;

import com.example.contentservice.domain.Member;
import com.example.contentservice.domain.MemberRoleEnum;
import com.example.contentservice.dto.member.LoginRequestDto;
import com.example.contentservice.dto.member.LoginResponseDto;
import com.example.contentservice.dto.member.SignupRequestDto;
import com.example.contentservice.security.PrincipalUtil;
import com.example.contentservice.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.security.Principal;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@Slf4j
@WebFluxTest(controllers = MemberController.class)
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MemberControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private MemberService memberService;

    @MockBean
    private PrincipalUtil principalUtil;

    @Test
    @WithMockUser
    @DisplayName("회원가입 POST 성공 테스트")
    public void testSignup() {
        SignupRequestDto signupRequestDto =
                new SignupRequestDto("user1", "password1", "nickname1");

        when(memberService.signup(Mockito.any(SignupRequestDto.class)))
                .thenReturn(Mono.just(ResponseEntity.ok(signupRequestDto.getNickname() + "님 회원 가입 완료 o(〃＾▽＾〃)o")));

        webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/members/signup")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(signupRequestDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(value -> {
                    Assertions.assertThat(value).isNotNull();
                    Assertions.assertThat(value).isEqualTo(signupRequestDto.getNickname() + "님 회원 가입 완료 o(〃＾▽＾〃)o");
                });
    }

    @Test
    @WithMockUser
    @DisplayName("로그인 POST 성공 테스트")
    public void testLogin() {
        LoginRequestDto loginRequestDto = new LoginRequestDto("user1", "password1");
        LoginResponseDto loginResponseDto = new LoginResponseDto("user1", "nickname1", "streamKey1");

        HttpHeaders headers = new HttpHeaders();
        headers.add("Access_Token", "mockAccessToken");
        headers.add("Refresh_Token", "mockRefreshToken");

        when(memberService.login(Mockito.any(LoginRequestDto.class)))
                .thenReturn(Mono.just(ResponseEntity.ok().headers(headers).body(loginResponseDto)));

        this.webTestClient
                .mutateWith(csrf())
                .post()
                .uri("/members/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequestDto)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access_Token", "mockAccessToken")
                .expectHeader().valueEquals("Refresh_Token", "mockRefreshToken")
                .expectBody(LoginResponseDto.class)
                .value(value -> {
                    Assertions.assertThat(value).isNotNull();
                    Assertions.assertThat(value.getUserId()).isEqualTo(loginRequestDto.getUserId());
                    Assertions.assertThat(value.getNickname()).isEqualTo(loginResponseDto.getNickname());
                    Assertions.assertThat(value.getStreamKey()).isNotNull();
                });
    }

    @Test
    @WithMockUser
    @DisplayName("로그아웃 GET 성공 테스트")
    public void testLogout() {
        Member USER1 = new Member(1L, "user1", "password1", "nickname1", "streamkey1", MemberRoleEnum.USER);

        when(principalUtil.getMember(Mockito.any(Principal.class))).thenReturn(USER1);
        when(memberService.logout("user1")).thenReturn(Mono.just(ResponseEntity.ok("삭제 성공 (∩^o^)⊃━☆")));

        this.webTestClient
                .mutateWith(csrf())
                .get()
                .uri("/members/logout")
                .header("Access_Token", "Bearer mockAccessToken")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(value -> {
                    Assertions.assertThat(value).isNotNull();
                    Assertions.assertThat(value).isEqualTo("삭제 성공 (∩^o^)⊃━☆");
                });
    }
}
