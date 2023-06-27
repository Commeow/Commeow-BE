package com.example.contentservice.service;

import com.example.contentservice.domain.*;
import com.example.contentservice.dto.member.LoginRequestDto;
import com.example.contentservice.dto.member.LoginResponseDto;
import com.example.contentservice.dto.member.SignupRequestDto;
import com.example.contentservice.dto.member.TokenDto;
import com.example.contentservice.jwt.JwtUtil;
import com.example.contentservice.repository.ChannelRepository;
import com.example.contentservice.repository.MemberRepository;
import com.example.contentservice.repository.PointRepository;
import com.example.contentservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final ChannelRepository channelRepository;
    private final PointRepository pointRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public Mono<ResponseEntity<String>> signup(SignupRequestDto signupRequestDto) {
        return memberRepository.existsByUserId(signupRequestDto.getUserId())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("중복된 아이디입니다."));
                    } else {
                        return memberRepository.existsByNickname(signupRequestDto.getNickname())
                                .flatMap(duplicated -> {
                                    if (duplicated) {
                                        return Mono.error(new IllegalArgumentException("중복된 닉네임입니다."));
                                    } else {
                                        Mono<Member> memberMono = memberRepository.save(new Member(signupRequestDto, passwordEncoder.encode(signupRequestDto.getPassword()), MemberRoleEnum.USER))
                                                .onErrorResume(exception -> Mono.error(new RuntimeException("회원 정보 저장 오류")));

                                        return memberMono
                                                .flatMap(member -> {
                                                    Mono<Channel> channelMono = channelRepository.save(new Channel(member.getNickname()))
                                                            .onErrorResume(exception -> Mono.error(new RuntimeException("회원 정보 저장 오류")));

                                                    Mono<Points> pointMono = pointRepository.save(new Points(member.getUserId()))
                                                            .onErrorResume(exception -> Mono.error(new RuntimeException("회원 정보 저장 오류")));

                                                    return Mono.zip(channelMono, pointMono)
                                                            .thenReturn(member);
                                                }).thenReturn(ResponseEntity.ok(signupRequestDto.getNickname() + "님 회원 가입 완료"));
                                    }
                                });
                    }
                });
    }

    @Transactional
    public Mono<ResponseEntity<LoginResponseDto>> login(LoginRequestDto loginRequestDto) {
        return memberRepository
                .findByUserId(loginRequestDto.getUserId())
                .switchIfEmpty(Mono.error(new NoSuchElementException("존재하지 않는 사용자입니다.")))
                .filter(member -> passwordEncoder.matches(loginRequestDto.getPassword(), member.getPassword()))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("비밀번호가 틀렸습니다.")))
                .flatMap(member -> {
                    TokenDto tokenDto = jwtUtil.createAllToken(member.getUserId(), member.getNickname());
                    return refreshTokenRepository.findByUserId(loginRequestDto.getUserId())
                            .switchIfEmpty(refreshTokenRepository
                                    .save(new RefreshToken(tokenDto.getRefreshToken(), loginRequestDto.getUserId()))
                                    .onErrorResume(exception -> Mono.error(new RuntimeException("회원 정보 저장 오류"))))
                            .flatMap(refreshToken -> {
                                if (refreshToken.getRefreshToken().equals(tokenDto.getRefreshToken())) {
                                    return Mono.just(refreshToken);
                                } else {
                                    return refreshTokenRepository.save(refreshToken.updateToken(tokenDto.getRefreshToken()))
                                            .onErrorResume(exception -> Mono.error(new RuntimeException("회원 정보 저장 오류")));
                                }
                            })
                            .flatMap(refreshToken -> {
                                HttpHeaders header = new HttpHeaders();
                                header.add(JwtUtil.ACCESS_TOKEN, tokenDto.getAccessToken());
                                header.add(JwtUtil.REFRESH_TOKEN, tokenDto.getRefreshToken());

                                return pointRepository.findByUserId(loginRequestDto.getUserId())
                                        .map(points -> new LoginResponseDto(
                                                member.getUserId(),
                                                member.getNickname(),
                                                member.getStreamKey(),
                                                points.getPoints()
                                        ))
                                        .map(loginResponseDto -> ResponseEntity.ok().headers(header).body(loginResponseDto));
                            });
                });
    }


    public Mono<ResponseEntity<String>> logout(String userId) {
        return refreshTokenRepository
                .deleteByUserId(userId)
                .flatMap(result -> {
                    return Mono.just(ResponseEntity.ok("Success"));
                });
    }
}
