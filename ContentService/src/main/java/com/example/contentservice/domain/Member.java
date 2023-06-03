package com.example.contentservice.domain;

import com.example.contentservice.dto.member.SignupRequestDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Getter
@Table("member")
@NoArgsConstructor
@AllArgsConstructor
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
        this.streamKey = UUID.randomUUID().toString();
        this.role = role;
    }
}
