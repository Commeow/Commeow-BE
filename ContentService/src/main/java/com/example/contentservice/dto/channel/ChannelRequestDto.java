package com.example.contentservice.dto.channel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChannelRequestDto {
    private String title;

    public void updateTitle(String title) {
        this.title = title;
    }
}
