package com.example.contentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatDto {
    private String nickname;
    private String message;
    private String chattingAddress;
}
