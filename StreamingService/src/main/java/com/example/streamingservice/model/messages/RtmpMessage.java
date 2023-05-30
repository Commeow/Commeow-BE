package com.example.streamingservice.model.messages;

import io.netty.buffer.ByteBuf;

public record RtmpMessage(RtmpHeader header, ByteBuf payload) {
}
