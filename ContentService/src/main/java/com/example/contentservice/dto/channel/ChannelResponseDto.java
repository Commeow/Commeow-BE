package com.example.contentservice.dto.channel;

import com.example.contentservice.domain.Channel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChannelResponseDto {
    private Long channelId;
    private String streamer;
    private String title;

    public ChannelResponseDto(Channel channel) {
        this.channelId = channel.getId();
        this.streamer = channel.getStreamer();
        this.title = channel.getTitle();
    }
}
