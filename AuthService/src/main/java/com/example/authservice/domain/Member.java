package com.example.authservice.domain;

import com.example.authservice.dto.SignupRequestDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Table("member")
@NoArgsConstructor
public class Member{
    @Id
    private Long id;
    private String userId;

    @JsonIgnore
    private String password;
    private String nickname;
    private String streamKey;
    private MemberRoleEnum role;

    public Member(SignupRequestDto signupRequestDto, String password, MemberRoleEnum role) {
        this.userId = signupRequestDto.getUserId();
        this.password = password;
        this.nickname = signupRequestDto.getNickname();
        this.streamKey = signupRequestDto.getStreamKey();
        this.role = role;
    }
}
