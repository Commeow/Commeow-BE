package com.example.authservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class TestController {
    @GetMapping("/any")
    @PreAuthorize("hasAnyRole()")
    public Mono<String> forAllUsers() {
        return Mono.just("아무나 볼 수 있는...어쩌구");
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public Mono<String> forAllMembers() {
        return Mono.just("회원만 볼 수 있는...어쩌구");
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<String> forAllAdmins() {
        return Mono.just("관리자만 볼 수 있는...어쩌구");
    }
}
