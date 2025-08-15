package org.example.Usersvc.event.publisher;

import org.example.Usersvc.event.model.BaseEvent;

/**
 * 이벤트 발행 서비스 인터페이스
 * 개발 환경과 운영 환경에서 서로 다른 구현체를 사용할 수 있도록 추상화
 */
public interface EventPublisherService {
    
    /**
     * 이벤트를 지정된 토픽에 발행
     * 
     * @param topic 이벤트를 발행할 토픽
     * @param event 발행할 이벤트 객체
     */
    void publishEvent(String topic, BaseEvent event);
    
    /**
     * 간단한 이벤트 발행
     * 
     * @param eventType 이벤트 타입
     * @param data 이벤트 데이터
     */
    void publishEvent(String eventType, Object data);
}