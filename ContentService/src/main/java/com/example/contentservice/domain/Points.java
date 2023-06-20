package com.example.contentservice.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Table("points")
@NoArgsConstructor
@AllArgsConstructor
public class Points {
    @Id
    private Long id;
    private String userId;
    private int points;

    public Points(String userId) {
        this.userId = userId;
        this.points = 0;
    }

    public Points addPoints(int points) {
        this.points += points;
        return this;
    }
}
