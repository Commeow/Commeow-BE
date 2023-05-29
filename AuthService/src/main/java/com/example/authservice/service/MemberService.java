package com.example.authservice.service;

import com.example.authservice.domain.Member;
import com.example.authservice.domain.MemberRoleEnum;
import com.example.authservice.domain.RefreshToken;
import com.example.authservice.dto.LoginRequestDto;
import com.example.authservice.dto.SignupRequestDto;
import com.example.authservice.jwt.JwtUtil;
import com.example.authservice.repository.MemberRepository;
import com.example.authservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public Mono<ResponseEntity<String>> signup(SignupRequestDto signupRequestDto) {
        return memberRepository.existsByUserId(signupRequestDto.getUserId())
                .flatMap(exists -> {
                    if (exists)
                        return Mono.error(new IllegalArgumentException("중복된 아이디입니다."));
                    else {
                        return memberRepository.save(new Member(signupRequestDto,
                                        passwordEncoder.encode(signupRequestDto.getPassword()),
                                        signupRequestDto.getMemberRole().equals("ADMIN") ? MemberRoleEnum.ADMIN : MemberRoleEnum.USER))
                                .onErrorResume(exception -> {
                                    return Mono.error(new RuntimeException("회원 정보 저장 중 오류 발생!"));
                                })
                                .thenReturn(ResponseEntity.ok(signupRequestDto.getNickname() + "님 회원 가입 완료 o(〃＾▽＾〃)o"));
                    }
                });
    }

    @Transactional
    public Mono<ResponseEntity<String>> login(LoginRequestDto loginRequestDto) {
        return memberRepository
                .findByUserId(loginRequestDto.getUserId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("존재하지 않는 사용자입니다.")))
                .filter(member -> passwordEncoder.matches(loginRequestDto.getPassword(), member.getPassword()))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("비밀번호가 틀렸습니다.")))
                .map(member -> jwtUtil.createAllToken(member.getUserId()))
                .flatMap(tokenDto -> {
                    return refreshTokenRepository.findByUserId(loginRequestDto.getUserId())
                            .switchIfEmpty(refreshTokenRepository.save(new RefreshToken(tokenDto.getRefreshToken(), loginRequestDto.getUserId()))
                                    .onErrorResume(exception -> {
                                        return Mono.error(new RuntimeException("Refresh Token 저장 중 오류 발생!"));
                                    }))
                            .flatMap(refreshToken -> {
                                if (refreshToken.getRefreshToken().equals(tokenDto.getRefreshToken()))
                                    return Mono.just(refreshToken);
                                else
                                    return refreshTokenRepository.save(refreshToken.updateToken(tokenDto.getRefreshToken()))
                                            .onErrorResume(exception -> {
                                                return Mono.error(new RuntimeException("Refresh Token 저장 중 오류 발생!"));
                                            });
                            })
                            .map(refreshToken -> {
                                HttpHeaders header = new HttpHeaders();

                                header.add(JwtUtil.ACCESS_TOKEN, tokenDto.getAccessToken());
                                header.add(JwtUtil.REFRESH_TOKEN, tokenDto.getRefreshToken());
                                return ResponseEntity.ok().headers(header).body("로그인 성공 o(〃＾▽＾〃)o");
                            });
                });
    }

    public Mono<ResponseEntity<String>> logout(String userId) {
        return refreshTokenRepository.deleteByUserId(userId)
                .flatMap(result -> {
                    if (result == 0)
                        return Mono.just(ResponseEntity.badRequest().body("삭제 실패 o(TヘT∩)"));
                    else return Mono.just(ResponseEntity.ok("삭제 성공 (∩^o^)⊃━☆"));
                });
    }
}
