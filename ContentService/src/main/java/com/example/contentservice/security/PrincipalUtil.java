package com.example.contentservice.security;

import com.example.contentservice.domain.Member;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class PrincipalUtil {
    public Member getMember(Principal principal) {
        if (principal instanceof Authentication) {
            Authentication authentication = (Authentication) principal;
            Object principalObject = authentication.getPrincipal();
            if (principalObject instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) principalObject;
                return userDetails.getMember();
            }
        }
        throw new RuntimeException("존재하지 않는 사용자입니다.");
    }
}
