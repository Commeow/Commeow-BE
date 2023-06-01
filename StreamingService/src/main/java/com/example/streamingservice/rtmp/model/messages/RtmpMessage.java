package com.example.streamingservice.rtmp.model.messages;

import io.netty.buffer.ByteBuf;

public record RtmpMessage(RtmpHeader header, ByteBuf payload) {
}
