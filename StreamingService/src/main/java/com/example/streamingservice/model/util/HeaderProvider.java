package com.example.streamingservice.model.util;

import com.example.streamingservice.model.messages.RtmpConstants;
import com.example.streamingservice.model.messages.RtmpHeader;

import static com.example.streamingservice.model.messages.RtmpConstants.*;

public class HeaderProvider {

    public static RtmpHeader acknowledgementHeader() {
        RtmpHeader header = new RtmpHeader();
        header.setType((short) RtmpConstants.RTMP_MSG_CONTROL_TYPE_ACKNOWLEDGEMENT);
        header.setCid(2);
        header.setStreamId(0);
        header.setMessageLength(4);

        return header;
    }

    public static RtmpHeader setWindowAcknowledgementHeader() {
        RtmpHeader header = new RtmpHeader();
        header.setType((short) RTMP_MSG_CONTROL_TYPE_WINDOW_ACKNOWLEDGEMENT_SIZE);
        header.setCid(2);
        header.setStreamId(0);
        header.setMessageLength(4);

        return header;
    }

    public static RtmpHeader setPeerBandwidthHeader() {
        RtmpHeader header = new RtmpHeader();
        header.setType((short) RTMP_MSG_CONTROL_TYPE_SET_PEER_BANDWIDTH);
        header.setCid(2);
        header.setStreamId(0);
        header.setMessageLength(5);

        return header;
    }

    public static RtmpHeader setChunkSizeHeader() {
        RtmpHeader header = new RtmpHeader();
        header.setType((short) RTMP_MSG_CONTROL_TYPE_SET_CHUNK_SIZE);
        header.setCid(2);
        header.setStreamId(0);
        header.setMessageLength(4);

        return header;
    }

    public static RtmpHeader commandMessageHeader(int messageLength) {
        RtmpHeader header = new RtmpHeader();
        header.setType((short) RtmpConstants.RTMP_MSG_COMMAND_TYPE_AMF0);
        header.setCid(3);
        header.setStreamId(RTMP_DEFAULT_MESSAGE_STREAM_ID_VALUE);
        header.setMessageLength(messageLength);

        return header;
    }
    public static RtmpHeader userControlMessageEventHeader(int event) {
        RtmpHeader header = new RtmpHeader();
        header.setType((short) RTMP_MSG_USER_CONTROL_TYPE_EVENT);
        header.setCid(2);
        header.setStreamId(RTMP_DEFAULT_MESSAGE_STREAM_ID_VALUE);
        header.setMessageLength(6);

        return header;
    }

    public static RtmpHeader dataMessageHeader(int messageLength) {
        RtmpHeader header = new RtmpHeader();
        header.setType((short) RTMP_MSG_DATA_TYPE_AMF0);
        header.setCid(3);
        header.setStreamId(RTMP_DEFAULT_MESSAGE_STREAM_ID_VALUE);
        header.setMessageLength(messageLength);

        return header;
    }


}
