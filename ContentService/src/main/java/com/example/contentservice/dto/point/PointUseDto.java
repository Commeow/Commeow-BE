package com.example.contentservice.dto.point;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PointUseDto {
    private String streamer;
    private int points;
    private String message;
}
