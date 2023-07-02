package com.example.streamingservice.rtmp;

import com.example.streamingservice.rtmp.entity.StreamKey;
import com.example.streamingservice.rtmp.handlers.*;
import com.example.streamingservice.rtmp.model.context.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;
import reactor.netty.tcp.TcpServer;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RtmpServerTest {

    @InjectMocks
    private TestRtmpServer rtmpServer;

    @Mock
    private RtmpMessageHandler getRtmpMessageHandler;

    @Mock
    private InboundConnectionLogger getInboundConnectionLogger;

    @Mock
    private HandshakeHandler getHandshakeHandler;

    @Mock
    private ChunkDecoder getChunkDecoder;

    @Mock
    private ChunkEncoder getChunkEncoder;

    @Mock
    private WebClient webClient;

    private final String authAddress = "testAuthAddress";
    private final String transcodingAddress = "testTranscodingAddress";

    @BeforeEach
    void setup() {
        rtmpServer.setWebClient(webClient);
        rtmpServer.setAuthAddress(authAddress);
        rtmpServer.setTranscodingAddress(transcodingAddress);
    }

    @Test
    @DisplayName("RTMP 서버 성공 테스트")
    void testRtmpServer() {

        WebClient.RequestBodyUriSpec mockPost = Mockito.mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestHeadersUriSpec mockGet = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.ResponseSpec mockResponse = Mockito.mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(mockPost);
        when(webClient.get()).thenReturn(mockGet);

        when(mockPost.uri(Mockito.anyString())).thenReturn(mockPost);
        when(mockGet.uri(Mockito.anyString())).thenReturn(mockGet);

        when(mockPost.retrieve()).thenReturn(mockResponse);
        when(mockGet.retrieve()).thenReturn(mockResponse);

        when(mockResponse.bodyToMono(Boolean.class)).thenReturn(Mono.just(true));
        when(mockResponse.bodyToMono(Long.class)).thenReturn(Mono.just(1L));

        StepVerifier.create(Mono.fromRunnable(()-> rtmpServer.run())).verifyComplete();
    }

    private static class TestRtmpServer extends RtmpServer {
        @Override
        protected RtmpMessageHandler getRtmpMessageHandler() {
            return Mockito.mock(RtmpMessageHandler.class);
        }

        @Override
        protected InboundConnectionLogger getInboundConnectionLogger() {
            return Mockito.mock(InboundConnectionLogger.class);
        }

        @Override
        protected HandshakeHandler getHandshakeHandler() {
            return Mockito.mock(HandshakeHandler.class);
        }

        @Override
        protected ChunkDecoder getChunkDecoder() {
            return Mockito.mock(ChunkDecoder.class);
        }

        @Override
        protected ChunkEncoder getChunkEncoder() {
            return Mockito.mock(ChunkEncoder.class);
        }
    }
}
