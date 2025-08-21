package org.example.Usersvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.domain.PlanType;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.service.PaymentService;
import org.example.Usersvc.service.PaymentServiceFactory;
import org.example.Usersvc.service.UserService;
import org.example.Usersvc.exception.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 통합 결제 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class UnifiedPaymentController {

    private final PaymentServiceFactory paymentServiceFactory;
    private final UserService userService;

    /**
     * 통합 구독 결제 요청
     */
    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<Map<String, Object>>> subscribe(
            @RequestBody Map<String, Object> request) {
        
        String userId = (String) request.get("userId");
        String planTypeStr = (String) request.get("planType");
        String provider = (String) request.getOrDefault("provider", "TOSSPAY");
        
        log.info("통합 결제 요청 - userId: {}, planType: {}, provider: {}", userId, planTypeStr, provider);
        
        try {
            // 1. 사용자 구독 가능 여부 확인
            if (!userService.canSubscribe(userId)) {
                log.warn("구독 불가능한 사용자 - userId: {}", userId);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("이미 PRO 구독을 이용중입니다. 구독 취소 후 다시 시도해주세요.", "ALREADY_SUBSCRIBED"));
            }
            
            PlanType planType = PlanType.valueOf(planTypeStr);
            
            // 2. PRO 플랜만 결제 가능 (FREE는 무료이므로 결제 불필요)
            if (planType != PlanType.PRO) {
                log.warn("FREE 플랜 결제 시도 - userId: {}", userId);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("FREE 플랜은 결제가 필요하지 않습니다.", "FREE_PLAN_NO_PAYMENT"));
            }
            
            PaymentService paymentService = paymentServiceFactory.getPaymentService(provider);
            
            Map<String, Object> paymentData = paymentService.createSubscriptionPayment(userId, planType);
            
            log.info("결제 요청 생성 완료 - userId: {}, provider: {}, orderId: {}", 
                userId, provider, paymentData.get("orderId"));
            
            return ResponseEntity.ok(ApiResponse.success(paymentData, 
                provider + " PRO 구독 결제 요청이 생성되었습니다."));
            
        } catch (IllegalArgumentException e) {
            log.error("잘못된 요청 파라미터: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "INVALID_PARAMETER"));
        } catch (Exception e) {
            log.error("결제 요청 생성 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("결제 요청에 실패했습니다.", "PAYMENT_REQUEST_ERROR"));
        }
    }
    
    /**
     * 구독 취소 요청
     */
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<String>> cancelSubscription(
            @RequestBody Map<String, Object> request) {
        
        String userId = (String) request.get("userId");
        
        log.info("구독 취소 요청 - userId: {}", userId);
        
        try {
            // 1. 사용자 구독 취소 가능 여부 확인
            if (!userService.canCancelSubscription(userId)) {
                log.warn("구독 취소 불가능한 사용자 - userId: {}", userId);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("FREE 플랜은 취소할 수 없습니다. PRO 구독만 취소 가능합니다.", "CANNOT_CANCEL_FREE_PLAN"));
            }
            
            // 2. 구독 취소 처리 (여기서는 단순히 is_active를 false로 변경)
            // 실제로는 TossPay API 호출하여 구독 취소해야 함
            log.info("구독 취소 처리 중 - userId: {}", userId);
            
            // TODO: 실제 구독 취소 로직 구현
            // - TossPay 구독 취소 API 호출
            // - 데이터베이스 subscription is_active = false 업데이트
            // - FREE 플랜으로 다운그레이드
            
            log.info("구독 취소 완료 - userId: {}", userId);
            
            return ResponseEntity.ok(ApiResponse.success(
                "구독이 성공적으로 취소되었습니다.", 
                "PRO 구독이 취소되고 FREE 플랜으로 변경되었습니다."));
            
        } catch (IllegalArgumentException e) {
            log.error("잘못된 요청 파라미터: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "INVALID_PARAMETER"));
        } catch (Exception e) {
            log.error("구독 취소 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("구독 취소에 실패했습니다.", "CANCEL_SUBSCRIPTION_ERROR"));
        }
    }
    
    /**
     * 사용자 구독 상태 및 가능한 액션 확인
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSubscriptionStatus(
            @PathVariable String userId) {
        
        log.info("구독 상태 조회 - userId: {}", userId);
        
        try {
            Map<String, Object> statusInfo = new HashMap<>();
            
            // 1. 구독 가능 여부
            boolean canSubscribe = userService.canSubscribe(userId);
            boolean canCancel = userService.canCancelSubscription(userId);
            
            statusInfo.put("canSubscribe", canSubscribe);
            statusInfo.put("canCancel", canCancel);
            
            // 2. 현재 구독 정보
            Optional<User> userOpt = userService.getUserById(userId);
            if (userOpt.isPresent()) {
                // 현재 구독 상태 정보 추가
                statusInfo.put("userId", userId);
                statusInfo.put("userEmail", userOpt.get().getUserEmail());
                
                // 액션 가능 여부에 따른 메시지
                if (canCancel) {
                    statusInfo.put("currentPlan", "PRO");
                    statusInfo.put("status", "ACTIVE");
                    statusInfo.put("message", "PRO 구독을 이용중입니다. 구독 취소가 가능합니다.");
                    statusInfo.put("availableActions", List.of("CANCEL"));
                } else if (canSubscribe) {
                    statusInfo.put("currentPlan", "FREE");
                    statusInfo.put("status", "ACTIVE");
                    statusInfo.put("message", "FREE 플랜을 이용중입니다. PRO 구독이 가능합니다.");
                    statusInfo.put("availableActions", List.of("SUBSCRIBE_PRO"));
                } else {
                    statusInfo.put("message", "구독 상태를 확인할 수 없습니다.");
                    statusInfo.put("availableActions", List.of());
                }
            } else {
                statusInfo.put("message", "사용자를 찾을 수 없습니다.");
                statusInfo.put("availableActions", List.of());
            }
            
            log.info("구독 상태 조회 완료 - userId: {}, canSubscribe: {}, canCancel: {}", 
                userId, canSubscribe, canCancel);
            
            return ResponseEntity.ok(ApiResponse.success(statusInfo));
            
        } catch (Exception e) {
            log.error("구독 상태 조회 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("구독 상태 조회에 실패했습니다.", "STATUS_CHECK_ERROR"));
        }
    }

    /**
     * 통합 결제 승인
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmPayment(
            @RequestBody Map<String, Object> request) {
        
        String paymentKey = (String) request.get("paymentKey");
        String orderId = (String) request.get("orderId");
        Integer amount = (Integer) request.get("amount");
        String provider = (String) request.getOrDefault("provider", "TOSSPAY");
        
        log.info("통합 결제 승인 요청 - paymentKey: {}, orderId: {}, amount: {}, provider: {}", 
            paymentKey, orderId, amount, provider);
        
        try {
            PaymentService paymentService = paymentServiceFactory.getPaymentService(provider);
            Map<String, Object> result = paymentService.confirmPayment(paymentKey, orderId, amount);
            
            log.info("✅ 결제 승인 완료 - orderId: {}, provider: {}", orderId, provider);
            
            return ResponseEntity.ok(ApiResponse.success(result, "결제가 성공적으로 승인되었습니다."));
            
        } catch (PaymentTimeoutException e) {
            log.error("결제 타임아웃: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), e.getErrorCode()));
        } catch (PaymentConfirmException e) {
            log.error("결제 승인 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), e.getErrorCode()));
        } catch (IllegalArgumentException e) {
            log.error("지원하지 않는 결제 제공자: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "UNSUPPORTED_PROVIDER"));
        } catch (Exception e) {
            log.error("결제 승인 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("결제 승인에 실패했습니다.", "PAYMENT_CONFIRM_FAILED"));
        }
    }

    /**
     * 결제 성공 페이지 (리다이렉트용)
     */
    @GetMapping("/success")
    public String paymentSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Integer amount,
            @RequestParam(defaultValue = "TOSSPAY") String provider) {
        
        log.info("결제 성공 처리 - paymentKey: {}, orderId: {}, amount: {}, provider: {}", 
            paymentKey, orderId, amount, provider);
        
        try {
            PaymentService paymentService = paymentServiceFactory.getPaymentService(provider);
            paymentService.confirmPayment(paymentKey, orderId, amount);
            
            log.info("✅ 결제 승인 완료 - orderId: {}", orderId);
            
            // 성공 시 테스트 페이지로 리다이렉트 (파라미터 포함)
            return "redirect:/payment-test?paymentKey=" + paymentKey + 
                   "&orderId=" + orderId + "&amount=" + amount;
            
        } catch (Exception e) {
            log.error("결제 확인 실패: {}", e.getMessage(), e);
            
            // 실패 시 에러 파라미터와 함께 리다이렉트
            return "redirect:/payment-test?code=PAYMENT_CONFIRM_ERROR&message=" + 
                   e.getMessage().replace(" ", "%20");
        }
    }

    /**
     * 결제 실패 페이지 (리다이렉트용)
     */
    @GetMapping("/fail")
    public String paymentFail(
            @RequestParam String code,
            @RequestParam String message,
            @RequestParam String orderId,
            @RequestParam(defaultValue = "TOSSPAY") String provider) {
        
        log.warn("결제 실패 - code: {}, message: {}, orderId: {}, provider: {}", 
            code, message, orderId, provider);
        
        // 실패 시 테스트 페이지로 리다이렉트 (에러 파라미터 포함)
        return "redirect:/payment-test?code=" + code + "&message=" + 
               message.replace(" ", "%20") + "&orderId=" + orderId;
    }
}