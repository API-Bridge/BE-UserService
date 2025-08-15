package org.example.Usersvc.event.publisher;

import org.example.Usersvc.event.model.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 마이크로서비스 이벤트 발행 컴포넌트
 * MSA 환경에서 Kafka를 통한 비동기 이벤트 발행 기능 제공
 * 
 * 주요 기능:
 * - Kafka를 통한 이벤트 메시지 발행
 * - 이벤트 발행 상태 로깅 및 모니터링
 * - 비동기 방식으로 이벤트 발행 및 성공/실패 대응
 * - Domain Event Pattern 구현을 위한 공통 인터페이스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    /** Kafka 메시지 전송을 위한 KafkaTemplate */
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 이벤트를 Kafka 토픽에 비동기로 발행
     * 이벤트 ID를 파티션 키로 사용하여 메시지 순서 보장
     * 
     * @param topic 이벤트를 발행할 Kafka 토픽
     * @param event 발행할 이벤트 객체 (BaseEvent 상속)
     */
    public void publishEvent(String topic, BaseEvent event) {
        log.info("Publishing event to topic: {}, eventType: {}, eventId: {}",
                topic, event.getEventType(), event.getEventId());

        kafkaTemplate.send(topic, event.getEventId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event: {}", event, ex);
                    } else {
                        log.debug("Event published successfully: {}", event.getEventId());
                    }
                });
    }
    
    /**
     * 간단한 이벤트 발행 메서드 (이벤트 타입과 데이터 직접 전달)
     * 
     * @param eventType 이벤트 타입
     * @param data 이벤트 데이터
     */
    public void publishEvent(String eventType, Object data) {
        log.info("Publishing simple event: eventType: {}, data: {}",
                eventType, data.getClass().getSimpleName());

        String eventId = java.util.UUID.randomUUID().toString();
        
        kafkaTemplate.send("user-service-events", eventId, data)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event: eventType: {}, eventId: {}", eventType, eventId, ex);
                    } else {
                        log.debug("Event published successfully: eventType: {}, eventId: {}", eventType, eventId);
                    }
                });
    }
}