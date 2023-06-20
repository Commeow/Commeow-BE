package com.example.contentservice.service;

import com.example.contentservice.domain.Member;
import com.example.contentservice.dto.point.PointRequestDto;
import com.example.contentservice.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class PointService {

    private final PointRepository pointRepository;

    public Mono<ResponseEntity<Integer>> addPoint(Member member, PointRequestDto pointRequestDto) {
        return pointRepository.findByUserId(member.getUserId())
                .switchIfEmpty(Mono.error(() -> new NoSuchElementException("존재하지 않는 사용자입니다.")))
                .flatMap(points -> {
                    return pointRepository.save(points.addPoints(pointRequestDto.getPoints()))
                            .map(savedPoints -> ResponseEntity.ok(savedPoints.getPoints()));
                });
    }

    public Mono<ResponseEntity<Integer>> usePoint(Member member, PointRequestDto pointRequestDto) {
        return pointRepository.findByUserId(member.getUserId())
                .switchIfEmpty(Mono.error(() -> new NoSuchElementException("존재하지 않는 사용자입니다.")))
                .flatMap(points -> {
                    if (points.getPoints() - pointRequestDto.getPoints() < 0) {
                        return Mono.error(new IllegalArgumentException("포인트가 부족합니다."));
                    }
                    return pointRepository.save(points.usePoints(pointRequestDto.getPoints()))
                            .map(savedPoints -> ResponseEntity.ok(savedPoints.getPoints()));
                });
    }
}
