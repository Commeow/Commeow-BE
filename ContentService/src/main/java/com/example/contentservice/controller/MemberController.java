package com.example.contentservice.controller;

import com.example.contentservice.domain.Member;
import com.example.contentservice.dto.member.LoginRequestDto;
import com.example.contentservice.dto.member.LoginResponseDto;
import com.example.contentservice.dto.member.MemberInfoResponseDto;
import com.example.contentservice.dto.member.SignupRequestDto;
import com.example.contentservice.security.UserDetailsImpl;
import com.example.contentservice.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.security.Principal;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("members")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/signup")
    public Mono<ResponseEntity<String>> signup(@RequestBody SignupRequestDto signupRequestDto) {
        return memberService.signup(signupRequestDto);
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponseDto>> login(@RequestBody LoginRequestDto loginRequestDto) {
        return memberService.login(loginRequestDto);
    }

    @GetMapping("/logout")
    public Mono<ResponseEntity<String>> logout(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return memberService.logout(userDetails.getUserId());
    }

    @GetMapping("/info")
    public Mono<ResponseEntity<MemberInfoResponseDto>> getUserInfo(Mono<Principal> userDetails) {
        return userDetails
                .map(principal -> getMemberFromPrincipal(principal))
                .map(member -> member.getUserId())
                .flatMap(userid -> {
                    return memberService.getUserInfo(userid);
                });
    }

    private Member getMemberFromPrincipal(Principal principal) {
        if (principal instanceof Authentication) {
            Authentication authentication = (Authentication) principal;
            Object principalObject = authentication.getPrincipal();
            if (principalObject instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) principalObject;
                return userDetails.getMember();
            }
        }
        throw new RuntimeException("당신 누구야! Σ(っ °Д °;)っ");
    }
}