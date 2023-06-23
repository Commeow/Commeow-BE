package com.example.contentservice.dto;

import com.example.contentservice.domain.MessageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatDto {
    private MessageTypeEnum type;
    private String nickname;
    private String message;
    private String chattingAddress;
}
