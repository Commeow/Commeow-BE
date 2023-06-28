package com.example.contentservice.dto.point;

import com.example.contentservice.domain.MessageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class DonationResponseDto {
    private MessageTypeEnum type;
    private String nickname;
    private int points;
    private int remainPoints;
    private String message;
}