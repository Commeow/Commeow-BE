package com.example.streamingservice.rtmp;

import com.example.streamingservice.rtmp.entity.Member;
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
//                .host("0.0.0.0")
                .port(1935)
                .doOnBound(disposableServer ->
                        log.info("RTMP server started on port {}", disposableServer.port()))
                .doOnConnection(connection -> connection
                        .addHandlerLast(getInboundConnectionLogger())
                        .addHandlerLast(getHandshakeHandler())
                        .addHandlerLast(getChunkDecoder())
                        .addHandlerLast(getChunkEncoder())
                        .addHandlerLast(getRtmpMessageHandler()))
                .option(ChannelOption.SO_BACKLOG, 128) // 클라이언트가 새로운 연결을 요청할 때 대기하는 큐 크기를 128로 설정
                .childOption(ChannelOption.SO_KEEPALIVE, true) // 특정 시간동안 클라이언트의 요청이 없어도 서버-클라이언트 연결이 끊기지 않도록 설정
                .handle((in, out) -> in
                        .receiveObject()
                        .cast(Stream.class)
                        .flatMap(stream ->
                                webClient
                                .post()
                                .uri(authAddress + "/auth/check")
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .body(Mono.just(new Member(stream.getStreamName(), stream.getStreamKey())), Member.class)
                                .retrieve()
                                .bodyToMono(Boolean.class)
                                .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(500)))
                                .doOnError(error -> log.info(error.getMessage()))
                                .onErrorReturn(Boolean.FALSE)
                                .flatMap(ans -> {
                                    log.info("Member {} stream key validation", stream.getStreamName());
//                                    if (ans) {
//                                        stream.sendPublishMessage();
//                                        stream.getReadyToBroadcast().thenRun(() -> webClient
//                                                .get()
//                                                .uri(transcodingAddress + "/ffmpeg/" + stream.getStreamName())
//                                                .retrieve()
//                                                .bodyToMono(Long.class)
////                                                .delaySubscription(Duration.ofSeconds(10L))
//                                                .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(1000)))
//                                                .doOnError(error -> {
//                                                    log.info("Error occured on transcoding server " + error.getMessage());
//                                                    stream.closeStream();
//                                                    stream.getPublisher().disconnect();
//                                                })
//                                                .onErrorComplete()
//                                                .subscribe((s) -> log.info("Transcoding server started ffmpeg with pid " + s.toString())));
//                                    } else {
//                                        stream.getPublisher().disconnect();
//                                    }
                                    return Mono.empty();
                                }))
                        .then())
                .bindNow();
        server.onDispose().block();
    }
}
