package com.example.contentservice.dto.channel;

import com.example.contentservice.domain.Channel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChannelDetailResponseDto {
    private Long channelId;
    private String title;
    private String streamer;
    private String chattingAddress;

    public ChannelDetailResponseDto(Channel channel) {
        this.channelId = channel.getId();
        this.title = channel.getTitle();
        this.streamer = channel.getStreamer();
        this.chattingAddress = channel.getChattingAddress();
    }
}
