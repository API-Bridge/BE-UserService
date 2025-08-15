package org.example.Usersvc.event.publisher;

import org.example.Usersvc.event.model.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 개발 환경용 Mock 이벤트 발행 컴포넌트
 * Kafka 없이 로깅만으로 이벤트 발행을 시뮬레이션
 * 
 * 주요 기능:
 * - 실제 Kafka 발행 없이 로깅만 수행
 * - 개발 환경에서 Kafka 의존성 제거
 * - 이벤트 발행 로직 테스트 지원
 */
@Slf4j
@Component
@Profile("dev")
public class DevEventPublisher implements EventPublisherService {

    /**
     * 개발 환경용 이벤트 발행 (로깅만 수행)
     * 
     * @param topic 이벤트를 발행할 토픽
     * @param event 발행할 이벤트 객체
     */
    public void publishEvent(String topic, BaseEvent event) {
        log.info("[DEV] Mock event published to topic: {}, eventType: {}, eventId: {}",
                topic, event.getEventType(), event.getEventId());
        log.debug("[DEV] Event details: {}", event);
    }
    
    /**
     * 개발 환경용 간단한 이벤트 발행 (로깅만 수행)
     * 
     * @param eventType 이벤트 타입
     * @param data 이벤트 데이터
     */
    public void publishEvent(String eventType, Object data) {
        String eventId = java.util.UUID.randomUUID().toString();
        log.info("[DEV] Mock simple event published: eventType: {}, eventId: {}, data: {}",
                eventType, eventId, data.getClass().getSimpleName());
        log.debug("[DEV] Event data details: {}", data);
    }
}