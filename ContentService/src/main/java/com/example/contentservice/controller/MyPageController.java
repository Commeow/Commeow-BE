package com.example.contentservice.controller;

import com.example.contentservice.dto.mypage.ChannelInfoDto;
import com.example.contentservice.service.MyPageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@RequestMapping("mypage")
@RequiredArgsConstructor
public class MyPageController {
    private final MyPageService myPageService;

    @PostMapping("/{id}")
    public Mono<ResponseEntity<String>> changeInfo(@PathVariable("id") String id, @RequestBody ChannelInfoDto channelInfoDto) {
        return myPageService.changeInfo(id, channelInfoDto);
    }
}
