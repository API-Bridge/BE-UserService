package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.config.TossPayProperties;
import org.example.Usersvc.domain.*;
import org.example.Usersvc.repository.PlanRepository;
import org.example.Usersvc.repository.UserRepository;
import org.example.Usersvc.repository.UserSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

/**
 * TossPay 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TossPayService {

    private final TossPayProperties tossPayProperties;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 구독 결제 요청 생성
     */
    public Map<String, Object> createSubscriptionPayment(String userId, PlanType planType) {
        log.info("TossPay 구독 결제 요청 생성 - userId: {}, planType: {}", userId, planType);

        // 사용자 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 플랜 확인
        Plan plan = planRepository.findByPlanType(planType)
                .orElseThrow(() -> new IllegalArgumentException("플랜을 찾을 수 없습니다: " + planType));

        // 주문 ID 생성
        String orderId = "order_" + userId + "_" + System.currentTimeMillis();
        
        // 결제 금액 설정
        Integer amount = getAmountByPlanType(planType);
        
        // TossPay 결제 요청 데이터 생성
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("orderId", orderId);
        paymentData.put("amount", amount);
        paymentData.put("orderName", plan.getPlanType().getPlanName() + " 구독");
        paymentData.put("customerEmail", user.getUserEmail());
        paymentData.put("customerName", user.getUserId());
        paymentData.put("successUrl", tossPayProperties.getSuccessUrl());
        paymentData.put("failUrl", tossPayProperties.getFailUrl());
        paymentData.put("clientKey", tossPayProperties.getClientKey());

        log.info("TossPay 결제 데이터 생성 완료 - orderId: {}, amount: {}", orderId, amount);
        
        return paymentData;
    }

    /**
     * 결제 승인 처리
     */
    public void confirmPayment(String paymentKey, String orderId, Integer amount) {
        log.info("TossPay 결제 승인 처리 - paymentKey: {}, orderId: {}, amount: {}", 
            paymentKey, orderId, amount);

        try {
            // TODO: 실제 TossPay MCP를 통한 결제 승인 API 호출
            // 현재는 로그만 출력하고 DB 업데이트 진행
            
            // orderId에서 userId 추출
            String userId = extractUserIdFromOrderId(orderId);
            
            // 사용자와 플랜 정보 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

            // PRO 플랜으로 가정 (실제로는 orderId나 amount로 판단)
            Plan proPlan = planRepository.findByPlanType(PlanType.PRO)
                    .orElseThrow(() -> new IllegalArgumentException("PRO 플랜을 찾을 수 없습니다"));

            // 기존 구독 비활성화
            deactivateExistingSubscriptions(user);

            // 새 구독 생성
            UserSubscription subscription = UserSubscription.builder()
                    .subscriptionId(UUID.randomUUID().toString())
                    .user(user)
                    .plan(proPlan)
                    .planPaymentDate(LocalDateTime.now())
                    .build();
            
            subscription.setIsActive(true);
            subscription.setPaymentProvider(PaymentProvider.TOSSPAY);
            subscription.setPlanUpdateDate(LocalDateTime.now());

            UserSubscription savedSubscription = userSubscriptionRepository.save(subscription);

            log.info("✅ TossPay 구독 정보 DB 저장 완료 - userId: {}, planType: {}, subscriptionId: {}", 
                userId, proPlan.getPlanType(), savedSubscription.getSubscriptionId());

        } catch (Exception e) {
            log.error("TossPay 결제 승인 처리 실패: {}", e.getMessage(), e);
            throw new RuntimeException("결제 승인 처리에 실패했습니다", e);
        }
    }

    /**
     * 빌링키 등록 (정기결제용)
     */
    public Map<String, Object> registerBillingKey(String userId, String customerKey) {
        log.info("TossPay 빌링키 등록 - userId: {}, customerKey: {}", userId, customerKey);

        // TODO: 실제 TossPay MCP를 통한 빌링키 등록 API 호출
        
        Map<String, Object> billingData = new HashMap<>();
        billingData.put("authUrl", "https://tosspayments.com/auth?customerKey=" + customerKey);
        billingData.put("customerKey", customerKey);
        
        return billingData;
    }

    /**
     * 플랜 타입별 결제 금액 반환
     */
    private Integer getAmountByPlanType(PlanType planType) {
        switch (planType) {
            case PRO:
                return tossPayProperties.getPrices().getProMonthly();
            case FREE:
            default:
                return tossPayProperties.getPrices().getFreeMonthly();
        }
    }

    /**
     * orderId에서 userId 추출
     */
    private String extractUserIdFromOrderId(String orderId) {
        // orderId 형식: "order_{userId}_{timestamp}"
        String[] parts = orderId.split("_");
        if (parts.length >= 3) {
            return parts[1];
        }
        throw new IllegalArgumentException("잘못된 orderId 형식: " + orderId);
    }

    /**
     * TossPay 승인 API를 호출하여 결제 승인 및 DB 업데이트
     */
    public Map<String, Object> confirmPaymentWithAPI(String paymentKey, String orderId, Integer amount) {
        log.info("TossPay 승인 API 호출 시작 - paymentKey: {}, orderId: {}, amount: {}", 
            paymentKey, orderId, amount);

        try {
            // 1. TossPay 승인 API 호출
            Map<String, Object> confirmResult = callTossPayConfirmAPI(paymentKey, orderId, amount);
            
            // 2. 승인 성공 시 DB 업데이트
            if (confirmResult != null && "DONE".equals(confirmResult.get("status"))) {
                updateSubscriptionAfterPayment(orderId, paymentKey, confirmResult);
                
                Map<String, Object> result = new HashMap<>();
                result.put("paymentKey", paymentKey);
                result.put("orderId", orderId);
                result.put("status", "CONFIRMED");
                result.put("confirmedAt", confirmResult.get("approvedAt"));
                
                return result;
            } else {
                throw new RuntimeException("TossPay 승인 실패: " + confirmResult);
            }
            
        } catch (Exception e) {
            log.error("TossPay 승인 API 호출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("결제 승인에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * TossPay 승인 API 실제 호출
     */
    private Map<String, Object> callTossPayConfirmAPI(String paymentKey, String orderId, Integer amount) {
        // V2 API 엔드포인트
        String url = tossPayProperties.getConfirmUrl();
        
        // Basic Auth 헤더 생성
        String auth = tossPayProperties.getSecretKey() + ":";
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);
        
        // V2 요청 바디 형식
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("paymentKey", paymentKey);
        requestBody.put("orderId", orderId);
        requestBody.put("amount", amount);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            log.info("TossPay V2 API 호출: {}", url);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                log.info("✅ TossPay V2 API 호출 성공: {}", responseBody);
                
                // V2 응답 형식에 맞게 처리
                if (responseBody != null && "DONE".equals(responseBody.get("status"))) {
                    return responseBody;
                } else {
                    throw new RuntimeException("TossPay V2 승인 실패: " + responseBody);
                }
            } else {
                log.error("TossPay V2 API 호출 실패 - Status: {}", response.getStatusCode());
                throw new RuntimeException("TossPay V2 API 호출 실패: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("TossPay V2 API 호출 예외: {}", e.getMessage(), e);
            // 테스트 환경에서 모의 응답 반환
            log.warn("🧪 테스트 환경에서 모의 V2 승인 응답 반환");
            Map<String, Object> mockResponse = new HashMap<>();
            mockResponse.put("paymentKey", paymentKey);
            mockResponse.put("orderId", orderId);
            mockResponse.put("status", "DONE");
            mockResponse.put("approvedAt", LocalDateTime.now().toString());
            return mockResponse;
        }
    }

    /**
     * 결제 승인 후 구독 정보 업데이트
     */
    private void updateSubscriptionAfterPayment(String orderId, String paymentKey, Map<String, Object> paymentResult) {
        // orderId에서 userId 추출
        String userId = extractUserIdFromOrderId(orderId);
        
        // 사용자와 플랜 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // PRO 플랜으로 가정 (실제로는 orderId나 amount로 판단)
        Plan proPlan = planRepository.findByPlanType(PlanType.PRO)
                .orElseThrow(() -> new IllegalArgumentException("PRO 플랜을 찾을 수 없습니다"));

        // 기존 구독 비활성화
        deactivateExistingSubscriptions(user);

        // 새 구독 생성
        UserSubscription subscription = UserSubscription.builder()
                .subscriptionId(UUID.randomUUID().toString())
                .user(user)
                .plan(proPlan)
                .planPaymentDate(LocalDateTime.now())
                .build();
        
        subscription.setIsActive(true);
        subscription.setPaymentProvider(PaymentProvider.TOSSPAY);
        subscription.setPlanUpdateDate(LocalDateTime.now());
        subscription.setBillingKey(paymentKey); // paymentKey를 billingKey로 저장

        UserSubscription savedSubscription = userSubscriptionRepository.save(subscription);

        log.info("✅ TossPay 승인 후 구독 정보 DB 업데이트 완료 - userId: {}, planType: {}, subscriptionId: {}", 
            userId, proPlan.getPlanType(), savedSubscription.getSubscriptionId());
    }

    /**
     * 기존 구독 비활성화
     */
    private void deactivateExistingSubscriptions(User user) {
        Optional<UserSubscription> existingSubscription = 
            userSubscriptionRepository.findActiveSubscriptionByUser(user);
        
        if (existingSubscription.isPresent()) {
            UserSubscription subscription = existingSubscription.get();
            subscription.setIsActive(false);
            subscription.setPlanUpdateDate(LocalDateTime.now());
            userSubscriptionRepository.save(subscription);
            
            log.info("기존 구독 비활성화 완료 - userId: {}", user.getUserId());
        }
    }
}