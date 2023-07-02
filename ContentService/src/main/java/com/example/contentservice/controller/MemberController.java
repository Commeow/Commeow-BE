package com.example.contentservice.controller;

import com.example.contentservice.dto.member.LoginRequestDto;
import com.example.contentservice.dto.member.LoginResponseDto;
import com.example.contentservice.dto.member.SignupRequestDto;
import com.example.contentservice.security.PrincipalUtil;
import com.example.contentservice.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("members")
public class MemberController {

    private final MemberService memberService;
    private final PrincipalUtil principalUtil;

    @PostMapping("/signup")
    public Mono<ResponseEntity<String>> signup(@RequestBody SignupRequestDto signupRequestDto) {
        return memberService.signup(signupRequestDto);
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponseDto>> login(@RequestBody LoginRequestDto loginRequestDto) {
        return memberService.login(loginRequestDto);
    }

    @GetMapping("/logout")
    public Mono<ResponseEntity<String>> logout(Mono<Principal> userDetails) {
        return userDetails.flatMap(principal -> {
            return memberService.logout(principalUtil.getMember(principal).getUserId());
        });
    }
}