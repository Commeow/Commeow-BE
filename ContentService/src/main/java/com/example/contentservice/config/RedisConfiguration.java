package com.example.contentservice.config;

import com.example.contentservice.domain.RefreshToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfiguration {

    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private int port;

    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }


    @Bean
    public ReactiveRedisTemplate<String, RefreshToken> refreshTokenDefine(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<RefreshToken> serializer = new Jackson2JsonRedisSerializer<>(RefreshToken.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, RefreshToken> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

        RedisSerializationContext<String, RefreshToken> context = builder.value(serializer).build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
