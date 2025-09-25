package org.example.Usersvc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 캐시 및 세션 저장소 설정 클래스
 * MSA 환경에서 성능 향상을 위한 Redis 기반 캐시 시스템 구성
 * 
 * 주요 기능:
 * - Redis 연결 팩토리 및 커넥션 풀 설정
 * - RedisTemplate 구성 (JSON 직렬화/역직렬화)
 * - Spring Cache 추상화를 위한 캐시 매니저 설정
 * - TTL 및 캐시 정책 관리
 */
@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.data.redis.port:6380}")
    private int redisPort;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        System.out.println("=== RedisConfig Debug ===");
        System.out.println("Redis Host: " + redisHost);
        System.out.println("Redis Port: " + redisPort);
        System.out.println("========================");
        return new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(redisHost, redisPort)
        );
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    // Spring Boot가 자동으로 stringRedisTemplate을 생성하므로 수동 정의 제거
    // 필요시 @Autowired로 주입받아 사용

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}