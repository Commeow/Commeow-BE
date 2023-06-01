package com.example.streamingservice.rtmp.handlers;

import com.example.streamingservice.rtmp.model.messages.RtmpMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import static com.example.streamingservice.rtmp.model.messages.RtmpConstants.*;

@Slf4j
public class ChunkEncoder extends MessageToByteEncoder<RtmpMessage> {

    private final long start = System.currentTimeMillis();
    private int chunkSize = RTMP_DEFAULT_CHUNK_SIZE;

    private boolean videoFirstMessage = true;
    private boolean audioFirstMessage = true;

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RtmpMessage message, ByteBuf byteBuf) {
        switch (message.header().getType()) {
            case RTMP_MSG_CONTROL_TYPE_SET_CHUNK_SIZE   -> handleSetChunkSize(message, byteBuf);
            case RTMP_MSG_USER_CONTROL_TYPE_AUDIO       -> handleAudioMessage(message, byteBuf);
            case RTMP_MSG_USER_CONTROL_TYPE_VIDEO       -> handleVideoMessage(message, byteBuf);
            default                                     -> handleDefault(message, byteBuf);
        }
    }

    private void handleDefault(RtmpMessage message, ByteBuf buf) {
        encodeFmt0(message, buf);
        encodeFmt3(message, buf);
    }

    private void handleAudioMessage(RtmpMessage message, ByteBuf buf) {
        if (audioFirstMessage) {
            log.info("Audio config is sent");
            handleDefault(message, buf);
            audioFirstMessage = false;
        } else {
            encodeFmt1(message, buf);
            encodeFmt3(message, buf);
        }
    }

    private void handleVideoMessage(RtmpMessage message, ByteBuf buf) {
        if (videoFirstMessage) {
            log.info("Video config is sent");
            handleDefault(message, buf);
            videoFirstMessage = false;
        } else {
            encodeFmt1(message, buf);
            encodeFmt3(message, buf);
        }
    }

    private void handleSetChunkSize(RtmpMessage message, ByteBuf buf) {
        chunkSize = message.payload().copy().readInt();
        handleDefault(message, buf);
    }

    private void encodeFmt0(RtmpMessage message, ByteBuf buf) {
        boolean extendedTimestamp = false;

        int cid = message.header().getCid();
        byte[] basicHeader = encodeFmtAndChunkId(RTMP_CHUNK_TYPE_0, cid);
        buf.writeBytes(basicHeader);

        long timestamp = System.currentTimeMillis() - start;

        if (timestamp >= RTMP_MAX_TIMESTAMP) {
            extendedTimestamp = true;
            buf.writeMedium(RTMP_MAX_TIMESTAMP);
        } else {
            buf.writeMedium((int) timestamp);
        }

        buf.writeMedium(message.header().getMessageLength());
        buf.writeByte(message.header().getType());
        buf.writeIntLE(message.header().getStreamId());

        if (extendedTimestamp) {
            buf.writeInt((int) timestamp);
        }

        int min = Math.min(chunkSize, message.payload().readableBytes());
        buf.writeBytes(message.payload(), min);
    }

    private void encodeFmt1(RtmpMessage message, ByteBuf buf) {
        int cid = message.header().getCid();
        byte[] basicHeader = encodeFmtAndChunkId(RTMP_CHUNK_TYPE_1, cid);
        buf.writeBytes(basicHeader);

        buf.writeMedium(message.header().getTimestampDelta());
        buf.writeMedium(message.header().getMessageLength());
        buf.writeByte(message.header().getType());

        int min = Math.min(chunkSize, message.payload().readableBytes());
        buf.writeBytes(message.payload(), min);
    }

    private void encodeFmt3(RtmpMessage message, ByteBuf buf) {
        int cid = message.header().getCid();
        byte[] basicHeader = encodeFmtAndChunkId(RTMP_CHUNK_TYPE_3, cid);
        while (message.payload().isReadable()) {
            buf.writeBytes(basicHeader);

            int min = Math.min(chunkSize, message.payload().readableBytes());
            buf.writeBytes(message.payload(), min);
        }
    }

    private byte[] encodeFmtAndChunkId(int fmt, int cid) {
        if (cid >= 64 + 255) {
            return new byte[] {
                    (byte) ((fmt << 6) | 1),
                    (byte) ((cid - 64) & 0xff),
                    (byte) (((cid - 64) >> 8) & 0xff)};
        } else if (cid >= 64) {
            return new byte[] {
                    (byte) ((fmt << 6)),
                    (byte) ((cid - 64) & 0xff)
            };
        } else {
            return new byte[] { (byte) ((fmt << 6) | cid) };
        }
    }
}
