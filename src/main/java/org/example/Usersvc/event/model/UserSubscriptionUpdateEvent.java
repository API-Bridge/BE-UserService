package org.example.Usersvc.event.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자 구독 플랜 업데이트 이벤트
 * SubscriptionEvents 토픽에서 수신하는 UserSubscriptionUpdate 이벤트를 처리
 */
@Getter
@Setter
@NoArgsConstructor
public class UserSubscriptionUpdateEvent extends BaseEvent {
    
    /**
     * 이벤트 페이로드 - 사용자 구독 정보
     */
    private Payload payload;

    public UserSubscriptionUpdateEvent(String userId, String previousPlan, String newPlan, String updateReason) {
        super("USER_SUBSCRIPTION_UPDATE");
        this.payload = new Payload(userId, previousPlan, newPlan, updateReason);
    }

    @Override
    public Object getPayload() {
        return this.payload;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Payload {
        /**
         * 사용자 ID
         */
        private String userId;
        
        /**
         * 이전 구독 플랜 (FREE, PRO)
         */
        private String previousPlan;
        
        /**
         * 새로운 구독 플랜 (FREE, PRO)
         */
        private String newPlan;
        
        /**
         * 플랜 변경 사유
         */
        private String updateReason;

        public Payload(String userId, String previousPlan, String newPlan, String updateReason) {
            this.userId = userId;
            this.previousPlan = previousPlan;
            this.newPlan = newPlan;
            this.updateReason = updateReason;
        }
    }
}