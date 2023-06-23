package com.example.contentservice.dto.point;

import com.example.contentservice.domain.MessageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DonationDto {
    private MessageTypeEnum type;
    private String streamer;
    private String nickname;
    private int points;
    private String chattingAddress;
}