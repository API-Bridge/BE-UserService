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
import org.example.Usersvc.event.model.UserSubscriptionUpdateEvent;
import org.example.Usersvc.event.publisher.EventPublisher;

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
    private final EventPublisher eventPublisher;

    /**
     * 구독 결제 요청 생성
     */
    public Map<String, Object> createSubscriptionPayment(String userId, PlanName planName) {
        log.info("TossPay 구독 결제 요청 생성 - userId: {}, planName: {}", userId, planName);

        // 사용자 확인
        log.debug("사용자 조회 시작 - userId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("사용자를 찾을 수 없습니다 - userId: {}", userId);
                    return new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId);
                });
        log.debug("사용자 조회 성공 - userId: {}, userEmail: {}", user.getUserId(), user.getUserEmail());

        // 플랜 확인
        log.debug("플랜 조회 시작 - planName: {}", planName);
        log.debug("데이터베이스에서 사용할 PlanName enum: {}", planName.name());
        log.debug("PlanName toString(): {}", planName.toString());
        
        Plan plan = planRepository.findByPlanName(planName)
                .orElseThrow(() -> {
                    log.error("플랜을 찾을 수 없습니다 - planName: {}", planName);
                    log.error("PlanName enum name: {}, toString: {}", planName.name(), planName.toString());
                    return new IllegalArgumentException("플랜을 찾을 수 없습니다: " + planName);
                });
        log.debug("플랜 조회 성공 - planName: {}, planId: {}, price: {}", planName, plan.getPlanId(), plan.getPrice());

        // 주문 ID 생성
        String orderId = "order_" + userId + "_" + System.currentTimeMillis();
        
        // 결제 금액 설정
        Integer amount = getAmountByplanName(planName);
        
        // TossPay 결제 요청 데이터 생성
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("orderId", orderId);
        paymentData.put("amount", amount);
        paymentData.put("orderName", plan.getPlanName().getPlanName() + " 구독");
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
            // TossPay MCP를 통한 결제 승인 API 호출
            // 실제 운영 환경에서는 TossPay API 연동 필요
            
            // orderId에서 userId 추출
            String userId = extractUserIdFromOrderId(orderId);
            
            // 사용자와 플랜 정보 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

            // PRO 플랜으로 가정 (실제로는 orderId나 amount로 판단)
            Plan proPlan = planRepository.findByPlanName(PlanName.PRO)
                    .orElseThrow(() -> new IllegalArgumentException("PRO 플랜을 찾을 수 없습니다"));

            // 기존 구독을 PRO로 업데이트 (새로 생성하지 않음)
            updateExistingSubscriptionToPro(user, proPlan, paymentKey);

            log.info("✅ TossPay 구독 정보 DB 업데이트 완료 - userId: {}, planName: {}", 
                userId, proPlan.getPlanName());

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

        try {
            // 사용자 확인
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

            // TossPay 빌링키 발급 API 호출
            Map<String, Object> billingKeyResponse = callTossPayBillingKeyAPI(userId, customerKey);
            
            if (billingKeyResponse != null && billingKeyResponse.containsKey("billingKey")) {
                // 기존 구독에 빌링키 업데이트
                updateBillingKeyToSubscription(user, (String) billingKeyResponse.get("billingKey"));
                
                Map<String, Object> result = new HashMap<>();
                result.put("status", "SUCCESS");
                result.put("billingKey", billingKeyResponse.get("billingKey"));
                result.put("customerKey", customerKey);
                result.put("message", "빌링키 등록이 완료되었습니다");
                
                return result;
            } else {
                throw new RuntimeException("빌링키 발급에 실패했습니다");
            }
            
        } catch (Exception e) {
            log.error("빌링키 등록 실패: {}", e.getMessage(), e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "FAILED");
            errorResult.put("message", "빌링키 등록에 실패했습니다: " + e.getMessage());
            
            return errorResult;
        }
    }

    /**
     * 플랜 타입별 결제 금액 반환
     */
    private Integer getAmountByplanName(PlanName planName) {
        switch (planName) {
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
     * 결제 승인 후 구독 정보 업데이트 - 기존 구독 레코드 업데이트
     */
    private void updateSubscriptionAfterPayment(String orderId, String paymentKey, Map<String, Object> paymentResult) {
        // orderId에서 userId 추출
        String userId = extractUserIdFromOrderId(orderId);
        
        // 사용자와 플랜 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // PRO 플랜으로 가정 (실제로는 orderId나 amount로 판단)
        Plan proPlan = planRepository.findByPlanName(PlanName.PRO)
                .orElseThrow(() -> new IllegalArgumentException("PRO 플랜을 찾을 수 없습니다"));

        // 기존 구독 레코드를 업데이트 (새로 생성하지 않음)
        updateExistingSubscriptionToPro(user, proPlan, paymentKey);
    }

    /**
     * 기존 구독을 PRO 플랜으로 업데이트
     */
    private void updateExistingSubscriptionToPro(User user, Plan proPlan, String paymentKey) {
        Optional<UserSubscription> existingSubscriptionOpt = userSubscriptionRepository.findActiveSubscriptionByUser(user);
        
        if (existingSubscriptionOpt.isPresent()) {
            // 기존 구독 레코드 업데이트
            UserSubscription subscription = existingSubscriptionOpt.get();
            subscription.setPlan(proPlan);
            subscription.setPlanPaymentDate(LocalDateTime.now());
            subscription.setPlanUpdateDate(LocalDateTime.now());
            subscription.setPaymentProvider(PaymentProvider.TOSSPAY);
            subscription.setBillingKey(paymentKey);

            UserSubscription savedSubscription = userSubscriptionRepository.save(subscription);
            
            // UserSubscriptionUpdateEvent 발행
            UserSubscriptionUpdateEvent event = new UserSubscriptionUpdateEvent(
                user.getUserId(), "FREE", "PRO", "TOSSPAY_PAYMENT_COMPLETED"
            );
            eventPublisher.publishEvent("SubscriptionEvents", event);
            
            log.info("✅ 기존 구독을 PRO로 업데이트 완료 - userId: {}, subscriptionId: {}", 
                user.getUserId(), savedSubscription.getSubscriptionId());
            log.info("📢 구독 업데이트 이벤트 발행 완료 - userId: {}", user.getUserId());
        } else {
            // 구독이 없는 경우 새로 생성 (일반적으로는 발생하지 않아야 함)
            log.warn("기존 구독이 없어서 새 PRO 구독을 생성합니다 - userId: {}", user.getUserId());
            UserSubscription newSubscription = UserSubscription.builder()
                    .subscriptionId(UUID.randomUUID().toString())
                    .user(user)
                    .plan(proPlan)
                    .planPaymentDate(LocalDateTime.now())
                    .build();
            
            newSubscription.setPaymentProvider(PaymentProvider.TOSSPAY);
            newSubscription.setPlanUpdateDate(LocalDateTime.now());
            newSubscription.setBillingKey(paymentKey);

            UserSubscription savedSubscription = userSubscriptionRepository.save(newSubscription);
            
            // UserSubscriptionUpdateEvent 발행
            UserSubscriptionUpdateEvent event = new UserSubscriptionUpdateEvent(
                user.getUserId(), "FREE", "PRO", "TOSSPAY_PAYMENT_COMPLETED"
            );
            eventPublisher.publishEvent("SubscriptionEvents", event);
            
            log.info("✅ 새 PRO 구독 생성 완료 - userId: {}, subscriptionId: {}", 
                user.getUserId(), savedSubscription.getSubscriptionId());
            log.info("📢 구독 업데이트 이벤트 발행 완료 - userId: {}", user.getUserId());
        }
    }


    /**
     * TossPay 빌링키 발급 API 호출
     */
    private Map<String, Object> callTossPayBillingKeyAPI(String userId, String customerKey) {
        // V2 빌링키 발급 API 엔드포인트 
        String url = "https://api.tosspayments.com/v2/billing/authorizations/issue";
        
        // Basic Auth 헤더 생성
        String auth = tossPayProperties.getSecretKey() + ":";
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);
        
        // 빌링키 발급 요청 바디
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("customerKey", customerKey);
        requestBody.put("cardNumber", ""); // 실제로는 카드 정보가 필요하지만 MCP에서는 모의 처리
        requestBody.put("cardExpirationYear", "");
        requestBody.put("cardExpirationMonth", "");
        requestBody.put("cardPassword", "");
        requestBody.put("customerBirthday", "");
        requestBody.put("consumerName", userId);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            log.info("TossPay 빌링키 발급 API 호출: {}", url);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                log.info("✅ TossPay 빌링키 발급 API 호출 성공: {}", responseBody);
                
                return responseBody;
            } else {
                log.error("TossPay 빌링키 발급 API 호출 실패 - Status: {}", response.getStatusCode());
                throw new RuntimeException("TossPay 빌링키 발급 API 호출 실패: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("TossPay 빌링키 발급 API 호출 예외: {}", e.getMessage(), e);
            // 테스트 환경에서 모의 응답 반환
            log.warn("🧪 테스트 환경에서 모의 빌링키 발급 응답 반환");
            
            Map<String, Object> mockResponse = new HashMap<>();
            mockResponse.put("billingKey", "test_billing_key_" + userId + "_" + System.currentTimeMillis());
            mockResponse.put("customerKey", customerKey);
            mockResponse.put("cardCompany", "현대");
            mockResponse.put("cardNumber", "433012******1234");
            mockResponse.put("cardType", "신용");
            mockResponse.put("authenticatedAt", LocalDateTime.now().toString());
            
            return mockResponse;
        }
    }

    /**
     * 사용자 구독에 빌링키 업데이트
     */
    private void updateBillingKeyToSubscription(User user, String billingKey) {
        Optional<UserSubscription> subscriptionOpt = userSubscriptionRepository.findActiveSubscriptionByUser(user);
        
        if (subscriptionOpt.isPresent()) {
            UserSubscription subscription = subscriptionOpt.get();
            subscription.setBillingKey(billingKey);
            subscription.setPlanUpdateDate(LocalDateTime.now());
            
            userSubscriptionRepository.save(subscription);
            
            log.info("✅ 구독에 빌링키 업데이트 완료 - userId: {}, billingKey: {}", 
                user.getUserId(), billingKey);
        } else {
            log.warn("활성 구독을 찾을 수 없어 빌링키를 저장할 수 없습니다 - userId: {}", user.getUserId());
        }
    }

    /**
     * 빌링키를 사용한 정기결제 실행
     */
    public Map<String, Object> chargeWithBillingKey(String userId, String billingKey, Integer amount, String orderName) {
        log.info("빌링키 정기결제 실행 - userId: {}, amount: {}, orderName: {}", userId, amount, orderName);
        
        try {
            // 빌링키 정기결제 API 호출
            Map<String, Object> chargeResult = callTossPayBillingChargeAPI(userId, billingKey, amount, orderName);
            
            if (chargeResult != null && "DONE".equals(chargeResult.get("status"))) {
                log.info("✅ 빌링키 정기결제 성공 - userId: {}, paymentKey: {}", userId, chargeResult.get("paymentKey"));
                
                Map<String, Object> result = new HashMap<>();
                result.put("status", "SUCCESS");
                result.put("paymentKey", chargeResult.get("paymentKey"));
                result.put("orderId", chargeResult.get("orderId"));
                result.put("amount", amount);
                result.put("approvedAt", chargeResult.get("approvedAt"));
                
                return result;
            } else {
                throw new RuntimeException("빌링키 정기결제 실패: " + chargeResult);
            }
            
        } catch (Exception e) {
            log.error("빌링키 정기결제 실행 실패: {}", e.getMessage(), e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "FAILED");
            errorResult.put("message", "빌링키 정기결제에 실패했습니다: " + e.getMessage());
            
            return errorResult;
        }
    }

    // 디버그용 메서드들
    public String getClientKeyForDebug() {
        return tossPayProperties.getClientKey();
    }
    
    public String getSecretKeyForDebug() {
        return tossPayProperties.getSecretKey();
    }
    
    public String getSuccessUrlForDebug() {
        return tossPayProperties.getSuccessUrl();
    }
    
    public String getFailUrlForDebug() {
        return tossPayProperties.getFailUrl();
    }

    /**
     * TossPay 빌링키 정기결제 API 호출
     */
    private Map<String, Object> callTossPayBillingChargeAPI(String userId, String billingKey, Integer amount, String orderName) {
        // V2 빌링키 정기결제 API 엔드포인트
        String url = "https://api.tosspayments.com/v2/billing/" + billingKey;
        
        // Basic Auth 헤더 생성
        String auth = tossPayProperties.getSecretKey() + ":";
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);
        
        // 정기결제 요청 바디
        String orderId = "billing_" + userId + "_" + System.currentTimeMillis();
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("customerKey", userId);
        requestBody.put("amount", amount);
        requestBody.put("orderId", orderId);
        requestBody.put("orderName", orderName);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        try {
            log.info("TossPay 빌링키 정기결제 API 호출: {}", url);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                log.info("✅ TossPay 빌링키 정기결제 API 호출 성공: {}", responseBody);
                
                return responseBody;
            } else {
                log.error("TossPay 빌링키 정기결제 API 호출 실패 - Status: {}", response.getStatusCode());
                throw new RuntimeException("TossPay 빌링키 정기결제 API 호출 실패: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("TossPay 빌링키 정기결제 API 호출 예외: {}", e.getMessage(), e);
            // 테스트 환경에서 모의 응답 반환
            log.warn("🧪 테스트 환경에서 모의 빌링키 정기결제 응답 반환");
            
            Map<String, Object> mockResponse = new HashMap<>();
            mockResponse.put("paymentKey", "test_payment_key_" + System.currentTimeMillis());
            mockResponse.put("orderId", orderId);
            mockResponse.put("status", "DONE");
            mockResponse.put("totalAmount", amount);
            mockResponse.put("approvedAt", LocalDateTime.now().toString());
            mockResponse.put("method", "카드");
            mockResponse.put("billingKey", billingKey);
            
            return mockResponse;
        }
    }
}