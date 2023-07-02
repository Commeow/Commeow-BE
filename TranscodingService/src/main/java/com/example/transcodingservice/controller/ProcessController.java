package com.example.transcodingservice.controller;

import com.example.transcodingservice.service.ProcessManagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ffmpeg")
@RequiredArgsConstructor
public class ProcessController {
    private final ProcessManagingService service;

    @GetMapping("/{owner}")
    Mono<Long> initiateFfmpeg(@PathVariable("owner") String owner) {
        return service.startProcess(owner);
    }
}
