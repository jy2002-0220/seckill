package com.xxxx.seckill.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    //实现对象，向redis发送时的序列化，因为key是String类型，value是对象类型，要在网络中传输必须经过序列化
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        //序列化key
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        //序列化value,将对象变为json存入redis
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        //hash类型key的序列化
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        //hash类型Value的序列化
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        //注入连接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }


}
