package com.example.streamingservice.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketException;
import java.time.Duration;
import java.time.LocalDateTime;
@Slf4j
public class InboundConnectionLogger extends ChannelInboundHandlerAdapter {

    LocalDateTime connectionTime = LocalDateTime.now();

    /*
    channel 활성화
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive()) {
            log.info("Channel is active. Address: " + ctx.channel().remoteAddress() + " .Channel id is: " + ctx.channel().id());
        }
    }

    /*
    channel 비활성화
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Channel id {} with address {} is inactive", ctx.channel().id(), ctx.channel().remoteAddress());
        Duration duration = Duration.between(connectionTime, LocalDateTime.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        String time = hours + " hours, " + minutes + " minutes, " + seconds + " seconds";
        log.info("Channel has been active for {}", time);
        ctx.fireChannelInactive();
    }

    /*
    예외 발생
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof SocketException) {
            log.info("Socket closed");
        } else {
            log.error("Error occured. Address: " + ctx.channel().remoteAddress(), cause);
        }
    }
}