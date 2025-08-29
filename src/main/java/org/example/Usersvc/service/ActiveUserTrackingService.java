package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.User;
import org.springframework.stereotype.Service;

/**
 * 활성 사용자 추적 서비스
 * 
 * ActiveUserTracker를 래핑하여 기존 API 호출 인터페이스 호환성을 제공합니다.
 * 기존 코드에서 ApiUsageTrackingService를 사용하는 부분들이 원활하게 작동하도록 지원합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActiveUserTrackingService {

    private final ActiveUserTracker activeUserTracker;

    /**
     * API 호출을 기록합니다 (활성 사용자로 추적)
     * 
     * @param user 사용자
     * @param apiEndpoint 호출된 API 엔드포인트 (현재는 사용하지 않음)
     * @param requestCount 요청 횟수 (현재는 사용하지 않음)
     */
    public void recordApiCall(User user, String apiEndpoint, int requestCount) {
        // API 호출을 활성 사용자 추적으로 변환
        activeUserTracker.track(user, "API_CALL");
        
        log.debug("API 호출을 활성 사용자로 추적 - userId: {}, endpoint: {}", 
                user.getUserId(), apiEndpoint);
    }

    /**
     * API 호출을 기록합니다 (기본 1회)
     * 
     * @param user 사용자
     * @param apiEndpoint 호출된 API 엔드포인트
     */
    public void recordApiCall(User user, String apiEndpoint) {
        recordApiCall(user, apiEndpoint, 1);
    }

    /**
     * 현재 분당 활성 사용자 수 (DAU와 동일)
     * 
     * @param user 사용자
     * @return 오늘의 활성 사용자 수
     */
    public long getCurrentMinuteUsage(User user) {
        return activeUserTracker.getTodayActiveUsers();
    }

    /**
     * 현재 시간당 활성 사용자 수 (DAU와 동일)
     * 
     * @param user 사용자
     * @return 오늘의 활성 사용자 수
     */
    public long getCurrentHourUsage(User user) {
        return activeUserTracker.getTodayActiveUsers();
    }

    /**
     * 현재 일일 활성 사용자 수 (DAU)
     * 
     * @param user 사용자
     * @return 오늘의 활성 사용자 수
     */
    public long getCurrentDayUsage(User user) {
        return activeUserTracker.getTodayActiveUsers();
    }

    /**
     * 월간 활성 사용자 수 (MAU)
     * 
     * @param user 사용자
     * @return 이번 달 활성 사용자 수
     */
    public long getMonthlyUsage(User user) {
        return activeUserTracker.getMonthlyActiveUsers();
    }

    /**
     * 개발/테스트용 카운터 초기화
     */
    public void resetCounters(String userId) {
        activeUserTracker.resetAllCounters();
        log.info("활성 사용자 카운터 초기화됨 - userId: {}", userId);
    }
}