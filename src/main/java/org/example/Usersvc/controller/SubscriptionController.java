package org.example.Usersvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.domain.Plan;
import org.example.Usersvc.domain.PlanName;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.example.Usersvc.service.DevRateLimitService;
import org.example.Usersvc.service.ActiveUserTrackingService;
import org.example.Usersvc.service.ProductionRateLimitService;
import org.example.Usersvc.service.UserService;
import org.example.Usersvc.repository.UserSubscriptionRepository;
import org.example.Usersvc.repository.PlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.example.Usersvc.common.logging.UserActionLogger;
import org.example.Usersvc.common.metrics.CustomMetrics;
import org.example.Usersvc.event.model.UserSubscriptionUpdateEvent;
import org.example.Usersvc.event.model.SubscriptionDeactivatedEvent;
import org.example.Usersvc.event.publisher.EventPublisherService;

/**
 * 구독 및 API 사용량 관리 컨트롤러
 */
@Slf4j
@RestController  // @RestController 활성화
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Subscription & Usage", description = "구독 관리 및 API 사용량 조회 API")
public class SubscriptionController {
    
    private final UserService userService;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final PlanRepository planRepository;
    private final UserActionLogger userActionLogger;
    private final CustomMetrics customMetrics;
    private final EventPublisherService eventPublisher;
    
    @Operation(
            summary = "현재 사용자 구독 정보 조회",
            description = "현재 로그인된 사용자의 구독 정보를 조회합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "구독 정보 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @GetMapping("/subscription/current")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUserSubscription() {
        try {
            // 개발 환경에서는 기본 사용자 ID 사용 (data.sql에 있는 사용자)
            String defaultUserId = "user-001";
            
            Optional<User> userOptional = userService.getUserByAuth0Id(defaultUserId);
            if (userOptional.isEmpty()) {
                // 사용자가 없으면 기본 구독 정보 반환
                Map<String, Object> defaultSubscription = new HashMap<>();
                PlanName freePlan = PlanName.FREE;
                defaultSubscription.put("planName", freePlan.getPlanName());
                defaultSubscription.put("status", "ACTIVE");
                defaultSubscription.put("startDate", LocalDateTime.now().toString());
                defaultSubscription.put("maxApiCount", freePlan.getMaxApiCount());
                defaultSubscription.put("rateLimitPerMinute", freePlan.getRateLimitPerMinute());
                defaultSubscription.put("rateLimitPerHour", freePlan.getRateLimitPerHour());
                defaultSubscription.put("rateLimitPerDay", freePlan.getRateLimitPerDay());
                
                return ResponseEntity.ok(ApiResponse.success(defaultSubscription));
            }
            
            User user = userOptional.get();
            Optional<UserSubscription> subscriptionOpt = userSubscriptionRepository.findActiveSubscriptionByUser(user);
            
            Map<String, Object> subscriptionInfo = new HashMap<>();
            if (subscriptionOpt.isPresent()) {
                UserSubscription subscription = subscriptionOpt.get();
                Plan plan = subscription.getPlan();
                
                subscriptionInfo.put("planName", plan.getPlanName());
                subscriptionInfo.put("status", subscription.isActive() ? "ACTIVE" : "INACTIVE");
                subscriptionInfo.put("startDate", subscription.getPlanPaymentDate().toString());
                subscriptionInfo.put("endDate", null); // 새 스키마에서는 endDate가 없음
                subscriptionInfo.put("maxApiCount", plan.getMaxApiCount());
                subscriptionInfo.put("rateLimitPerMinute", plan.getRateLimitPerMinute());
                subscriptionInfo.put("rateLimitPerHour", plan.getRateLimitPerHour());
                subscriptionInfo.put("rateLimitPerDay", plan.getRateLimitPerDay());
                subscriptionInfo.put("paymentProvider", subscription.getPaymentProvider() != null ? subscription.getPaymentProvider().name() : "TOSSPAY");
            } else {
                // 활성 구독이 없으면 기본 FREE 플랜 정보 반환
                PlanName freePlan = PlanName.FREE;
                subscriptionInfo.put("planName", freePlan.getPlanName());
                subscriptionInfo.put("status", "ACTIVE");
                subscriptionInfo.put("startDate", LocalDateTime.now().toString());
                subscriptionInfo.put("maxApiCount", freePlan.getMaxApiCount());
                subscriptionInfo.put("rateLimitPerMinute", freePlan.getRateLimitPerMinute());
                subscriptionInfo.put("rateLimitPerHour", freePlan.getRateLimitPerHour());
                subscriptionInfo.put("rateLimitPerDay", freePlan.getRateLimitPerDay());
            }
            
            return ResponseEntity.ok(ApiResponse.success(subscriptionInfo));
            
        } catch (Exception e) {
            log.error("현재 사용자 구독 정보 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("구독 정보 조회에 실패했습니다.", "SUBSCRIPTION_ERROR"));
        }
    }
    
    /**
     * 구독 취소 엔드포인트
     */
    @Operation(
            summary = "구독 취소",
            description = "사용자의 활성 구독을 취소합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PostMapping("/users/{userId}/subscription/cancel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelSubscription(
            @Parameter(description = "사용자 Auth0 ID") @PathVariable String userId) {
        try {
            log.info("구독 취소 요청 - userId: {}", userId);
            
            // 사용자 존재 여부 확인
            Optional<User> userOptional = userService.getUserByAuth0Id(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
            }
            User user = userOptional.get();
            
            // 활성 구독 조회
            Optional<UserSubscription> activeSubscriptionOpt = userSubscriptionRepository.findActiveSubscriptionByUser(user);
            if (activeSubscriptionOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("취소할 활성 구독이 없습니다.", "NO_ACTIVE_SUBSCRIPTION"));
            }
            
            UserSubscription activeSubscription = activeSubscriptionOpt.get();
            
            // FREE 플랜은 취소할 수 없음
            if (activeSubscription.getPlan().getPlanName() == PlanName.FREE) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("FREE 플랜은 취소할 수 없습니다.", "CANNOT_CANCEL_FREE_PLAN"));
            }
            
            // FREE 플랜으로 자동 전환 (기존 구독의 플랜만 변경)
            Plan freePlan = planRepository.findByPlanName(PlanName.FREE)
                .orElseThrow(() -> new RuntimeException("FREE 플랜을 찾을 수 없습니다"));
            
            String previousPlanName = activeSubscription.getPlan().getPlanName().name();
            
            // 기존 구독의 플랜만 변경 (ID 유지)
            activeSubscription.setPlan(freePlan);
            activeSubscription.setPlanUpdateDate(LocalDateTime.now());
            userSubscriptionRepository.save(activeSubscription);
            
            // UserSubscriptionUpdateEvent 발행
            UserSubscriptionUpdateEvent updateEvent = new UserSubscriptionUpdateEvent(
                user.getUserId(), previousPlanName, "FREE", "USER_CANCELLED"
            );
            eventPublisher.publishEvent("SubscriptionEvents", updateEvent);
            
            // 구독 취소 이벤트 발행
            publishSubscriptionCancelledEvent(user, previousPlanName, "USER_CANCELLED");
            
            // 메트릭 기록
            customMetrics.incrementSubscriptionCancelled(previousPlanName);
            
            // 사용자 액션 로깅
            userActionLogger.logSubscriptionCancellation(userId, previousPlanName, "USER_CANCELLED");
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "CANCELLED");
            response.put("message", "구독이 성공적으로 취소되어 FREE 플랜으로 전환되었습니다");
            response.put("cancelledAt", LocalDateTime.now().toString());
            response.put("previousPlanName", previousPlanName);
            response.put("newPlanName", "FREE");
            response.put("subscriptionId", activeSubscription.getSubscriptionId()); // 동일한 ID 유지
            
            log.info("구독 취소 및 FREE 플랜 전환 완료 - userId: {}, subscriptionId: {}, previousPlan: {}", 
                    userId, activeSubscription.getSubscriptionId(), previousPlanName);
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("구독 취소 중 오류 발생 - userId: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("구독 취소에 실패했습니다: " + e.getMessage(), "CANCEL_ERROR"));
        }
    }
    
    /**
     * 구독 취소 이벤트 발행
     */
    private void publishSubscriptionCancelledEvent(User user, String previousPlanName, String reason) {
        try {
            // 필요한 정보들 수집
            Optional<UserSubscription> subscriptionOpt = userSubscriptionRepository.findActiveSubscriptionByUser(user);
            String subscriptionId = subscriptionOpt.map(UserSubscription::getSubscriptionId).orElse("unknown");
            Long planId = subscriptionOpt.map(s -> s.getPlan().getPlanId().longValue()).orElse(0L);
            
            SubscriptionDeactivatedEvent event = SubscriptionDeactivatedEvent.createUserCancelledEvent(
                    user.getUserId(), 
                    subscriptionId,
                    planId,
                    previousPlanName, 
                    null, // stripeSubscriptionId (TossPay로 전환되어 null)
                    0L,   // activeDays (계산 복잡성으로 인해 기본값 사용)
                    subscriptionId); // newSubscriptionId (기존 구독을 FREE로 전환하므로 동일)
            
            eventPublisher.publishEvent("user-events", event);
            log.info("구독 취소 이벤트 발행 완료 - userId: {}", user.getUserId());
        } catch (Exception e) {
            log.warn("구독 취소 이벤트 발행 실패 - userId: {}", user.getUserId(), e);
            // 이벤트 발행 실패가 전체 취소 프로세스를 방해하지 않도록 함
        }
    }
    
    /**
     * 테스트 모드인지 확인하는 메소드
     * TossPay를 사용하므로 항상 실제 모드로 동작
     */
    private boolean isTestMode() {
        // TossPay 사용으로 변경됨 - 실제 결제 모드로 동작
        return false; // 실제 TossPay API 사용
    }
    
    /**
     * 결제 성공 페이지 핸들러
     */
    @GetMapping("/subscription/success")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handlePaymentSuccess(
            @RequestParam(required = false) String session_id) {
        
        log.info("결제 성공 페이지 접근 - session_id: {}", session_id);
        
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "결제가 성공적으로 완료되었습니다.");
            response.put("sessionId", session_id);
            response.put("status", "success");
            
            if (session_id != null) {
                // 세션 정보 조회 시도 (실제 환경에서는 Stripe API 호출)
                log.info("Stripe 세션 정보 조회: {}", session_id);
                response.put("redirectUrl", "/dashboard");
            }
            
            return ResponseEntity.ok(ApiResponse.success(response, "결제 완료"));
            
        } catch (Exception e) {
            log.error("결제 성공 페이지 처리 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("결제 확인 중 오류가 발생했습니다.", "PAYMENT_SUCCESS_ERROR"));
        }
    }

    /**
     * 결제 취소 페이지 핸들러
     */
    @GetMapping("/subscription/cancel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handlePaymentCancel() {
        
        log.info("결제 취소 페이지 접근");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "결제가 취소되었습니다.");
        response.put("status", "cancelled");
        response.put("redirectUrl", "/pricing");
        
        return ResponseEntity.ok(ApiResponse.success(response, "결제 취소"));
    }
}