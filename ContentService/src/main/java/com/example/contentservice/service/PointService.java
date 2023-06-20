package com.example.contentservice.service;

import com.example.contentservice.domain.Member;
import com.example.contentservice.domain.Points;
import com.example.contentservice.dto.point.PointRequestDto;
import com.example.contentservice.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointService {

    private final PointRepository pointRepository;

    public Mono<ResponseEntity<String>> addPoint(Member member, PointRequestDto pointRequestDto) {
        return pointRepository.findByUserId(member.getUserId())
                .switchIfEmpty(Mono.error(() -> new NoSuchElementException("존재하지 않는 사용자입니다.")))
                .flatMap(points -> {
                    return pointRepository.save(points.addPoints(pointRequestDto.getPoints()));
                }).thenReturn(ResponseEntity.ok("Success"));
    }
}
