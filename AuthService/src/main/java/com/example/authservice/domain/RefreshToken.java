package com.example.authservice.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Table(name = "refreshtoken")
@NoArgsConstructor
public class RefreshToken {
    @Id
    private Long id;
    private String refreshToken;
    private String userId;

    public RefreshToken(String refreshToken, String userId){
        this.refreshToken = refreshToken;
        this.userId = userId;
    }

    public RefreshToken updateToken(String token){
        this.refreshToken = token;
        return this;
    }
}
