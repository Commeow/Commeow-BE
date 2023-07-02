package com.example.streamingservice.rtmp;

import com.example.streamingservice.rtmp.entity.StreamKey;
import com.example.streamingservice.rtmp.handlers.*;
import com.example.streamingservice.rtmp.model.context.Stream;
import io.netty.channel.ChannelOption;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpServer;
import reactor.util.retry.Retry;

import java.time.Duration;

@NoArgsConstructor
@Getter
@Setter
@Slf4j
public abstract class RtmpServer implements CommandLineRunner {

    protected abstract RtmpMessageHandler getRtmpMessageHandler();

    protected abstract InboundConnectionLogger getInboundConnectionLogger();

    protected abstract HandshakeHandler getHandshakeHandler();

    protected abstract ChunkDecoder getChunkDecoder();

    protected abstract ChunkEncoder getChunkEncoder();

    @Autowired
    private WebClient webClient;

    @Value("${transcoding.server}")
    private String transcodingAddress;

    @Value("${auth.server}")
    private String authAddress;

    @Override
    public void run(String... args) {
        DisposableServer server = TcpServer.create()
                        .port(1935)
                        .doOnBound(disposableServer ->
                                log.info("RTMP 서버가 포트 {} 에서 시작됩니다.", disposableServer.port()))
                        .doOnConnection(connection -> connection
                                .addHandlerLast(getInboundConnectionLogger())
                                .addHandlerLast(getHandshakeHandler())
                                .addHandlerLast(getChunkDecoder())
                                .addHandlerLast(getChunkEncoder())
                                .addHandlerLast(getRtmpMessageHandler()))
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .handle((in, out) -> in
                                .receiveObject()
                                .cast(Stream.class)
                        .flatMap(stream -> {
                            return webClient
                                    .post()
                                    .uri(authAddress + "/broadcasts/" + stream.getStreamName() + "/check")
                                    .body(Mono.just(new StreamKey(stream.getStreamKey())), StreamKey.class)
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .retrieve()
                                    .bodyToMono(Boolean.class).log()
                                    .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(500)))
                                    .doOnError(error -> log.info(error.getMessage()))
                                    .onErrorReturn(Boolean.FALSE)
                                    .flatMap(ans -> {
                                        if (ans) {
                                            log.info("스트리머 {} 의 stream key가 유효합니다.", stream.getStreamName());
                                            stream.sendPublishMessage();
                                            stream.getReadyToBroadcast().thenRun(() -> webClient
                                                    .get()
                                                    .uri(transcodingAddress + "/ffmpeg/" + stream.getStreamName())
                                                    .retrieve()
                                                    .bodyToMono(Long.class)
                                                    .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(1000)))
                                                    .doOnError(error -> {
                                                        log.info("Transcoding 서버에서 다음의 에러가 발생했습니다 : " + error.getMessage());
                                                        webClient
                                                                .post()
                                                                .uri(authAddress + "/broadcasts/" + stream.getStreamName() + "/offair")
                                                                .retrieve()
                                                                .bodyToMono(Boolean.class)
                                                                .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(500)))
                                                                .doOnError(e -> log.info(e.getMessage()))
                                                                .onErrorReturn(Boolean.FALSE)
                                                                .subscribeOn(Schedulers.parallel())
                                                                .subscribe((s) -> {
                                                                    log.info("방송 송출이 끊어집니다.");
                                                                    if (s) {
                                                                        log.info("방송이 종료됩니다.");
                                                                    } else {
                                                                        log.info("ContentService 서버와 통신 에러 발생");
                                                                    }
                                                                });
                                                        stream.closeStream();
                                                        stream.getPublisher().disconnect();
                                                    })
                                                    .onErrorComplete()
                                                    .subscribe((s) -> {
                                                        log.info("Transcoding server started ffmpeg with pid " + s.toString());
                                                        webClient
                                                                .post()
                                                                .uri(authAddress + "/broadcasts/" + stream.getStreamName() + "/onair")
                                                                .retrieve()
                                                                .bodyToMono(Boolean.class)
                                                                .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(500)))
                                                                .doOnError(e -> log.info(e.getMessage()))
                                                                .onErrorReturn(Boolean.FALSE)
                                                                .subscribeOn(Schedulers.parallel())
                                                                .subscribe((t) -> {
                                                                    if (t) {
                                                                        log.info("방송이 시작됩니다.");
                                                                    } else {
                                                                        log.info("ContentService 서버와 통신 에러 발생");
                                                                    }
                                                                });
                                                    }));
                                        } else {
                                            stream.getPublisher().disconnect();
                                        }
                                        return Mono.empty();
                                    });
                        })
                        .then())
                .bindNow();
        server.onDispose().block();
    }
}
