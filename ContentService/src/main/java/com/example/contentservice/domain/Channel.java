package com.example.contentservice.domain;

import com.example.contentservice.dto.channel.ChannelRequestDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Getter
@Table("channel")
@NoArgsConstructor
@AllArgsConstructor
public class Channel {
    @Id
    private Long id;
    private String title;
    private String streamer;
    private String chattingAddress;
    private Boolean onAir;

    public Channel(String nickname) {
        this.title = nickname + "님의 방송 (^・ω・^ ) <( Commeow!)";
        this.streamer = nickname;
        this.chattingAddress = UUID.randomUUID().toString();
        this.onAir = false;
    }

    public Channel channelOn(){
        this.onAir = true;
        return this;
    }

    public Channel channelOff(){
        this.onAir = false;
        return this;
    }
}
