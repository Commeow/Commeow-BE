package com.example.contentservice.service;

import com.example.contentservice.domain.Member;
import com.example.contentservice.domain.Points;
import com.example.contentservice.dto.point.PointChargeDto;
import com.example.contentservice.dto.point.PointUseDto;
import com.example.contentservice.repository.MemberRepository;
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
    private final MemberRepository memberRepository;

    public Mono<ResponseEntity<Integer>> addPoint(Member member, PointChargeDto pointChargeDto) {
        return pointRepository.findByUserId(member.getUserId())
                .switchIfEmpty(Mono.error(() -> new NoSuchElementException("존재하지 않는 사용자입니다.")))
                .flatMap(points -> {
                    return pointRepository.save(points.addPoints(pointChargeDto.getPoints()))
                            .map(savedPoints -> ResponseEntity.ok(savedPoints.getPoints()));
                });
    }

    public Mono<ResponseEntity<Integer>> usePoint(Member member, PointUseDto pointUseDto) {
        return memberRepository.findByNickname(pointUseDto.getStreamer())
                .switchIfEmpty(Mono.error(() -> new NoSuchElementException("존재하지 않는 사용자입니다.")))
                .flatMap(streamer -> pointRepository.findByUserId(member.getUserId())
                        .switchIfEmpty(Mono.error(() -> new NoSuchElementException("존재하지 않는 사용자입니다.")))
                        .flatMap(memberPoints -> {
                            if (memberPoints.getPoints() - pointUseDto.getPoints() < 0) {
                                return Mono.error(() -> new NoSuchElementException("포인트가 부족합니다."));
                            }

                            return pointRepository.save(memberPoints.usePoints(pointUseDto.getPoints()))
                                    .flatMap(savedMemberPoints -> pointRepository.findByUserId(streamer.getUserId())
                                            .flatMap(streamerPoints -> {
                                                Points updatedStreamerPoints = streamerPoints.addPoints(pointUseDto.getPoints());
                                                return pointRepository.save(updatedStreamerPoints)
                                                        .map(savedStreamerPoints -> ResponseEntity.ok(savedMemberPoints.getPoints()));
                                            }));
                        }));
    }
}
