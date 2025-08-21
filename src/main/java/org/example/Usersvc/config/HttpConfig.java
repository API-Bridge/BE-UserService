package org.example.Usersvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP 클라이언트 설정
 */
@Configuration
public class HttpConfig {

    /**
     * RestTemplate Bean 생성
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}