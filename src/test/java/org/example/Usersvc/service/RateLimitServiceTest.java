package org.example.Usersvc.service;

import org.example.Usersvc.domain.SubscriptionPlan;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RateLimitService 테스트
 */
@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private UserSubscriptionService userSubscriptionService;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    @InjectMocks
    private RateLimitService rateLimitService;
    
    private User testUser;
    private SubscriptionPlan testPlan;
    private UserSubscription testSubscription;
    
    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = User.builder()
                .auth0Id("test-auth0-id")
                .userEmail("test@example.com")
                .build();
        testUser.setUserId("1");
        
        // 테스트 플랜 생성
        testPlan = SubscriptionPlan.builder()
                .planName("FREE")
                .maxApiCount(10)
                .rateLimitPerMinute(60)
                .rateLimitPerHour(1000)
                .rateLimitPerDay(10000)
                .monthlyPrice(0)
                .build();
        
        // 테스트 구독 생성
        testSubscription = UserSubscription.builder()
                .user(testUser)
                .plan(testPlan)
                .startedAt(LocalDateTime.now())
                .expiresAt(null)
                .build();
        
        // RedisTemplate Mock 설정
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userSubscriptionService.getActiveSubscription(testUser)).thenReturn(testSubscription);
    }
    
    @Test
    @DisplayName("분당 첫 번째 요청은 허용되어야 한다")
    void shouldAllowFirstRequestPerMinute() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null);
        
        // When
        boolean result = rateLimitService.isAllowedPerMinute(testUser);
        
        // Then
        assertThat(result).isTrue();
        verify(valueOperations).set(anyString(), eq("1"), any());
    }
    
    @Test
    @DisplayName("분당 제한 내의 요청은 허용되어야 한다")
    void shouldAllowRequestWithinMinuteLimit() {
        // Given
        when(valueOperations.get(anyString())).thenReturn("30");
        
        // When
        boolean result = rateLimitService.isAllowedPerMinute(testUser);
        
        // Then
        assertThat(result).isTrue();
        verify(valueOperations).increment(anyString());
    }
    
    @Test
    @DisplayName("분당 제한을 초과한 요청은 거부되어야 한다")
    void shouldRejectRequestExceedingMinuteLimit() {
        // Given
        when(valueOperations.get(anyString())).thenReturn("60");
        
        // When
        boolean result = rateLimitService.isAllowedPerMinute(testUser);
        
        // Then
        assertThat(result).isFalse();
        verify(valueOperations, never()).increment(anyString());
    }
    
    @Test
    @DisplayName("시간당 제한 내의 요청은 허용되어야 한다")
    void shouldAllowRequestWithinHourLimit() {
        // Given
        when(valueOperations.get(anyString())).thenReturn("500");
        
        // When
        boolean result = rateLimitService.isAllowedPerHour(testUser);
        
        // Then
        assertThat(result).isTrue();
        verify(valueOperations).increment(anyString());
    }
    
    @Test
    @DisplayName("시간당 제한을 초과한 요청은 거부되어야 한다")
    void shouldRejectRequestExceedingHourLimit() {
        // Given
        when(valueOperations.get(anyString())).thenReturn("1000");
        
        // When
        boolean result = rateLimitService.isAllowedPerHour(testUser);
        
        // Then
        assertThat(result).isFalse();
        verify(valueOperations, never()).increment(anyString());
    }
    
    @Test
    @DisplayName("일일 제한 내의 요청은 허용되어야 한다")
    void shouldAllowRequestWithinDayLimit() {
        // Given
        when(valueOperations.get(anyString())).thenReturn("5000");
        
        // When
        boolean result = rateLimitService.isAllowedPerDay(testUser);
        
        // Then
        assertThat(result).isTrue();
        verify(valueOperations).increment(anyString());
    }
    
    @Test
    @DisplayName("일일 제한을 초과한 요청은 거부되어야 한다")
    void shouldRejectRequestExceedingDayLimit() {
        // Given
        when(valueOperations.get(anyString())).thenReturn("10000");
        
        // When
        boolean result = rateLimitService.isAllowedPerDay(testUser);
        
        // Then
        assertThat(result).isFalse();
        verify(valueOperations, never()).increment(anyString());
    }
    
    @Test
    @DisplayName("현재 분당 사용량을 정확히 조회해야 한다")
    void shouldReturnCorrectCurrentMinuteUsage() {
        // Given
        when(valueOperations.get(anyString())).thenReturn("25");
        
        // When
        long usage = rateLimitService.getCurrentMinuteUsage(testUser);
        
        // Then
        assertThat(usage).isEqualTo(25);
    }
    
    @Test
    @DisplayName("Redis 에러 시 graceful degradation으로 허용해야 한다")
    void shouldAllowRequestOnRedisError() {
        // Given
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis connection error"));
        
        // When
        boolean result = rateLimitService.isAllowedPerMinute(testUser);
        
        // Then
        assertThat(result).isTrue();
    }
}