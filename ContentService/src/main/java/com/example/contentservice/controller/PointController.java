package com.example.contentservice.controller;

import com.example.contentservice.dto.point.PointChargeDto;
import com.example.contentservice.dto.point.PointUseDto;
import com.example.contentservice.security.PrincipalUtil;
import com.example.contentservice.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.security.Principal;

@RestController
@RequestMapping("points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;
    private final PrincipalUtil principalUtil;

    @PostMapping("/charge")
    public Mono<ResponseEntity<Integer>> addPoint(Mono<Principal> userDetails, @RequestBody PointChargeDto pointChargeDto) {
        return userDetails.flatMap(principal -> {
            return pointService.addPoint(principalUtil.getMember(principal), pointChargeDto);
        });
    }

    @PostMapping("/spend")
    public Mono<ResponseEntity<Integer>> spendPoint(Mono<Principal> userDetails, @RequestBody PointUseDto pointUseDto) {
        return userDetails.flatMap(principal -> {
            return pointService.usePoint(principalUtil.getMember(principal), pointUseDto);
        });
    }
}
