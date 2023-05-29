package com.example.authservice.controller;

import com.example.authservice.dto.LoginRequestDto;
import com.example.authservice.dto.SignupRequestDto;
import com.example.authservice.security.UserDetailsImpl;
import com.example.authservice.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
    public Mono<ResponseEntity<String>> login(@RequestBody LoginRequestDto loginRequestDto) {
        return memberService.login(loginRequestDto);
    }

    @GetMapping("/logout")
    public Mono<ResponseEntity<String>> logout(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return memberService.logout(userDetails.getUserId());
    }
}
