package com.example.contentservice.jwt;

import com.example.contentservice.dto.member.TokenDto;
import com.example.contentservice.repository.RefreshTokenRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {
    public static final String ACCESS_TOKEN = "Access_Token";
    public static final String REFRESH_TOKEN = "Refresh_Token";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.secret.key}")
    private String SECURITY_KEY;
    private Key key;

    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(SECURITY_KEY);
        key = Keys.hmacShaKeyFor(bytes);
    }

    public TokenDto createAllToken(String userId) {
        return new TokenDto(createToken(userId, ACCESS_TOKEN),
                createToken(userId, REFRESH_TOKEN));
    }

    public String createToken(String userId, String type) {
        Date date = new Date();
        Date exprTime = type.equals(JwtUtil.ACCESS_TOKEN) ?
                Date.from(Instant.now().plus(1, ChronoUnit.HOURS)) :
                Date.from(Instant.now().plus(14, ChronoUnit.DAYS));

        return BEARER_PREFIX +
                Jwts.builder()
                        .signWith(key, signatureAlgorithm)
                        .setSubject(userId)
                        .setExpiration(exprTime)
                        .setIssuedAt(date)
                        .compact();
    }

    public Mono<Boolean> validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return Mono.just(true);
        } catch (SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT signature, 유효하지 않는 JWT 서명 입니다.");
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT token, 만료된 JWT token 입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT claims is empty, 잘못된 JWT 토큰 입니다.");
        }
        return Mono.just(false);
    }

    public Mono<Boolean> refreshTokenValidation(String token) {
        return validateToken(token)
                .flatMap((valid) -> {
                    if (!valid)
                        return Mono.just(false);
                    else {
                        return refreshTokenRepository
                                .existsByUserId(getUserInfoFromToken(token))
                                .flatMap((exists) -> {
                                    if (exists) {
                                        return refreshTokenRepository
                                                .findByUserId(getUserInfoFromToken(token))
                                                .map(refreshToken -> {
                                                    return token.equals(refreshToken.getRefreshToken().substring(7));
                                                });
                                    } else return Mono.just(false);
                                });
                    }
                });
    }

    public String getUserInfoFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();
    }
}
