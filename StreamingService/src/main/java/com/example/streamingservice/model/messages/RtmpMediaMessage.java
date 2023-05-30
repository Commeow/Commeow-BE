package com.example.streamingservice.model.messages;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

public record RtmpMediaMessage(RtmpHeader header, byte[] payload) {

    public static RtmpMediaMessage  fromRtmpMessage(RtmpMessage message) {
        return new RtmpMediaMessage(message.header(), ByteBufUtil.getBytes(message.payload()));
    }
    public static RtmpMessage toRtmpMessage(RtmpMediaMessage message) {
        return new RtmpMessage(message.header(), Unpooled.wrappedBuffer(message.payload()));
    }

    public boolean isAudioConfig() {
        return this.payload.length > 1 && this.payload[1] == 0x00;
    }

    public boolean isKeyframe() {
        return this.payload.length > 1 && this.payload[0] == 0x17;
    }

    public boolean isVideoConfig() {
        return this.payload.length > 1 && this.payload[1] == 0x00;
    }

}
