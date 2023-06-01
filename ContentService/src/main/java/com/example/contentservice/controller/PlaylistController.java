package com.example.contentservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@Slf4j
public class PlaylistController {
    @Value("${stream.directory}")
    private String path;

    @GetMapping(value = "/streams/**")
    public Mono<Void> downloadM3u8(ServerHttpRequest request,
                                   ServerHttpResponse response) {
        // 경로 지정
        Path file = Paths.get(path);
        String requestUrl = request.getPath().toString();
        String fileName = requestUrl.split("/streams/")[1];

        // "무복사" 파일 전송을 지원
        ZeroCopyHttpOutputMessage zeroCopyResponse =
                (ZeroCopyHttpOutputMessage) response;
        HttpHeaders headers = response.getHeaders();
        // 전송헤더에 파일명을 셋팅
        headers.setContentDispositionFormData(fileName, fileName);
        headers.setAccessControlAllowOrigin("*");
        // 전송헤더에 파일 정보 셋팅
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        // 모든 캐시를 쓰기 전에 서버에 이 캐시 진짜 써도 되냐고 물어보라는 뜻
        headers.setCacheControl(CacheControl.noCache());

        // 경로조합 - 고정된 루트 경로에 부분 경로 추가(공통부분 경로 정의할 때 유용)
        Path ans = file.resolve(fileName);
        log.info(ans.toAbsolutePath().toString());
        if (!Files.exists(ans)) {
            response.setStatusCode(HttpStatus.NOT_FOUND);
            return zeroCopyResponse.setComplete();
        }

        return zeroCopyResponse
                .writeWith(ans, 0, ans.toFile().length());
    }
}
