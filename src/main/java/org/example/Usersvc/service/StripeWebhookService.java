package org.example.Usersvc.service;

import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.Plan;
import org.example.Usersvc.domain.PlanType;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.example.Usersvc.repository.PlanRepository;
import org.example.Usersvc.repository.UserRepository;
import org.example.Usersvc.repository.UserSubscriptionRepository;
import org.example.Usersvc.config.StripeProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Stripe 웹훅 이벤트 처리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StripeWebhookService {

    private final UserRepository userRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final PlanRepository planRepository;
    private final StripeCustomerService stripeCustomerService;
    private final StripeProperties stripeProperties;

    /**
     * 체크아웃 세션 완료 처리
     * 결제가 완료되면 구독을 활성화
     */
    public void processCheckoutCompleted(Session session) {
        try {
            String customerId = session.getCustomer();
            String subscriptionId = session.getSubscription();
            
            log.info("체크아웃 완료 처리: customer={}, subscription={}, metadata={}", 
                customerId, subscriptionId, session.getMetadata());
            
            // 먼저 메타데이터에서 userId 확인
            Optional<User> userOptional = Optional.empty();
            String userIdFromMetadata = session.getMetadata().get("userId");
            
            if (userIdFromMetadata != null) {
                log.info("메타데이터에서 userId 찾음: {}", userIdFromMetadata);
                userOptional = userRepository.findById(userIdFromMetadata);
            }
            
            // 메타데이터로 찾지 못하면 고객 정보로 사용자 찾기
            if (userOptional.isEmpty()) {
                log.info("메타데이터에서 사용자를 찾지 못함, Stripe 고객 정보로 검색 시도");
                userOptional = findUserByStripeCustomerId(customerId);
            }
            
            if (userOptional.isEmpty()) {
                log.error("❌ 체크아웃 완료 처리 실패 - 사용자를 찾을 수 없음: customerId={}, sessionId={}, metadata={}", 
                    customerId, session.getId(), session.getMetadata());
                return;
            }
            
            User user = userOptional.get();
            
            // Stripe에서 구독 정보 조회
            if (subscriptionId != null) {
                try {
                    log.info("✅ 체크아웃 완료 - Stripe 구독 정보 조회 시작: subscriptionId={}", subscriptionId);
                    Subscription stripeSubscription = Subscription.retrieve(subscriptionId);
                    processSubscriptionCreated(stripeSubscription);
                    log.info("✅ 체크아웃 완료 - 구독 처리 완료: subscriptionId={}", subscriptionId);
                } catch (Exception e) {
                    log.error("❌ Stripe 구독 정보 조회 실패: {}", e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("체크아웃 완료 처리 중 오류: {}", e.getMessage(), e);
        }
    }

    /**
     * 구독 생성 처리
     */
    public void processSubscriptionCreated(Subscription stripeSubscription) {
        try {
            String customerId = stripeSubscription.getCustomer();
            String subscriptionId = stripeSubscription.getId();
            String status = stripeSubscription.getStatus();
            
            log.info("구독 생성 처리: customer={}, subscription={}, status={}", 
                customerId, subscriptionId, status);
            
            // 사용자 찾기
            Optional<User> userOptional = findUserByStripeCustomerId(customerId);
            if (userOptional.isEmpty()) {
                log.error("구독 생성 처리 실패 - Stripe 고객 ID로 사용자를 찾을 수 없음: customerId={}, subscriptionId={}", 
                    customerId, subscriptionId);
                return;
            }
            
            User user = userOptional.get();
            
            // Price ID로 플랜 결정
            String priceId = stripeSubscription.getItems().getData().get(0).getPrice().getId();
            log.info("구독에서 추출한 Price ID: {}", priceId);
            
            Optional<Plan> planOptional = findPlanByPriceId(priceId);
            if (planOptional.isEmpty()) {
                log.error("구독 생성 처리 실패 - Price ID로 플랜을 찾을 수 없음: priceId={}, subscriptionId={}", 
                    priceId, subscriptionId);
                return;
            }
            
            Plan plan = planOptional.get();
            
            // 기존 구독 비활성화
            deactivateExistingSubscriptions(user);
            
            // 새 구독 생성 또는 업데이트
            UserSubscription userSubscription = findOrCreateUserSubscription(user, plan);
            userSubscription.setStripeSubscriptionId(subscriptionId);
            userSubscription.setIsActive("active".equals(status));
            userSubscription.setPlanUpdateDate(LocalDateTime.now());
            
            UserSubscription savedSubscription = userSubscriptionRepository.save(userSubscription);
            
            log.info("✅ DB 구독 정보 업데이트 완료: userId={}, planName={}, active={}, subscriptionId={}, stripeSubscriptionId={}", 
                user.getUserId(), plan.getPlanType().getPlanName(), savedSubscription.getIsActive(), 
                savedSubscription.getSubscriptionId(), savedSubscription.getStripeSubscriptionId());
            
        } catch (Exception e) {
            log.error("구독 생성 처리 중 오류: {}", e.getMessage(), e);
        }
    }

    /**
     * 구독 업데이트 처리
     */
    public void processSubscriptionUpdated(Subscription stripeSubscription) {
        try {
            String subscriptionId = stripeSubscription.getId();
            String status = stripeSubscription.getStatus();
            
            log.info("구독 업데이트 처리: subscription={}, status={}", subscriptionId, status);
            
            // 구독 ID로 사용자 구독 찾기
            Optional<UserSubscription> userSubscriptionOptional = 
                userSubscriptionRepository.findByStripeSubscriptionId(subscriptionId);
            
            if (userSubscriptionOptional.isEmpty()) {
                log.warn("Stripe 구독 ID로 사용자 구독을 찾을 수 없음: {}", subscriptionId);
                return;
            }
            
            UserSubscription userSubscription = userSubscriptionOptional.get();
            userSubscription.setIsActive("active".equals(status));
            userSubscription.setPlanUpdateDate(LocalDateTime.now());
            
            userSubscriptionRepository.save(userSubscription);
            
            log.info("사용자 구독 상태 업데이트 완료: userId={}, active={}", 
                userSubscription.getUser().getUserId(), userSubscription.getIsActive());
            
        } catch (Exception e) {
            log.error("구독 업데이트 처리 중 오류: {}", e.getMessage(), e);
        }
    }

    /**
     * 구독 삭제 처리
     */
    public void processSubscriptionDeleted(Subscription stripeSubscription) {
        try {
            String subscriptionId = stripeSubscription.getId();
            
            log.info("구독 삭제 처리: subscription={}", subscriptionId);
            
            // 구독 ID로 사용자 구독 찾기
            Optional<UserSubscription> userSubscriptionOptional = 
                userSubscriptionRepository.findByStripeSubscriptionId(subscriptionId);
            
            if (userSubscriptionOptional.isEmpty()) {
                log.warn("Stripe 구독 ID로 사용자 구독을 찾을 수 없음: {}", subscriptionId);
                return;
            }
            
            UserSubscription userSubscription = userSubscriptionOptional.get();
            userSubscription.setIsActive(false);
            userSubscription.setPlanUpdateDate(LocalDateTime.now());
            
            userSubscriptionRepository.save(userSubscription);
            
            log.info("사용자 구독 비활성화 완료: userId={}", 
                userSubscription.getUser().getUserId());
            
        } catch (Exception e) {
            log.error("구독 삭제 처리 중 오류: {}", e.getMessage(), e);
        }
    }

    /**
     * 결제 성공 처리
     */
    public void processPaymentSucceeded(Invoice invoice) {
        try {
            String subscriptionId = invoice.getSubscription();
            if (subscriptionId != null) {
                log.info("결제 성공: subscription={}", subscriptionId);
                
                // 구독 상태를 active로 업데이트
                Optional<UserSubscription> userSubscriptionOptional = 
                    userSubscriptionRepository.findByStripeSubscriptionId(subscriptionId);
                
                if (userSubscriptionOptional.isPresent()) {
                    UserSubscription userSubscription = userSubscriptionOptional.get();
                    userSubscription.setIsActive(true);
                    userSubscription.setPlanPaymentDate(LocalDateTime.now());
                    userSubscription.setPlanUpdateDate(LocalDateTime.now());
                    
                    userSubscriptionRepository.save(userSubscription);
                    
                    log.info("결제 성공으로 구독 활성화: userId={}", 
                        userSubscription.getUser().getUserId());
                }
            }
        } catch (Exception e) {
            log.error("결제 성공 처리 중 오류: {}", e.getMessage(), e);
        }
    }

    /**
     * 결제 실패 처리
     */
    public void processPaymentFailed(Invoice invoice) {
        try {
            String subscriptionId = invoice.getSubscription();
            if (subscriptionId != null) {
                log.info("결제 실패: subscription={}", subscriptionId);
                
                // 필요에 따라 구독 상태 업데이트 또는 알림 처리
                Optional<UserSubscription> userSubscriptionOptional = 
                    userSubscriptionRepository.findByStripeSubscriptionId(subscriptionId);
                
                if (userSubscriptionOptional.isPresent()) {
                    UserSubscription userSubscription = userSubscriptionOptional.get();
                    log.warn("사용자 결제 실패: userId={}, subscription={}", 
                        userSubscription.getUser().getUserId(), subscriptionId);
                    
                    // 실패한 결제에 대한 추가 처리 (예: 알림, 유예 기간 등)
                }
            }
        } catch (Exception e) {
            log.error("결제 실패 처리 중 오류: {}", e.getMessage(), e);
        }
    }

    /**
     * Stripe 고객 ID로 사용자 찾기
     */
    private Optional<User> findUserByStripeCustomerId(String customerId) {
        // 실제 구현에서는 User 엔티티에 stripeCustomerId 필드가 있어야 함
        // 현재는 이메일로 매칭하는 방식으로 구현
        try {
            com.stripe.model.Customer customer = com.stripe.model.Customer.retrieve(customerId);
            String email = customer.getEmail();
            if (email != null) {
                return userRepository.findByUserEmail(email);
            }
        } catch (Exception e) {
            log.error("Stripe 고객 정보 조회 실패: {}", e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Price ID로 플랜 찾기
     */
    private Optional<Plan> findPlanByPriceId(String priceId) {
        // Pro 플랜 Price ID 확인
        if (priceId != null && priceId.equals(stripeProperties.getPrices().getProMonthly())) {
            return planRepository.findByPlanType(PlanType.PRO);
        }
        // Free 플랜은 기본값
        return planRepository.findByPlanType(PlanType.FREE);
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
            
            log.info("기존 구독 비활성화: userId={}", user.getUserId());
        }
    }

    /**
     * 사용자 구독 찾기 또는 생성
     */
    private UserSubscription findOrCreateUserSubscription(User user, Plan plan) {
        Optional<UserSubscription> existingSubscription = 
            userSubscriptionRepository.findActiveSubscriptionByUser(user);
        
        if (existingSubscription.isPresent()) {
            UserSubscription subscription = existingSubscription.get();
            subscription.setPlan(plan);
            return subscription;
        } else {
            UserSubscription newSubscription = UserSubscription.builder()
                .subscriptionId(java.util.UUID.randomUUID().toString())
                .user(user)
                .plan(plan)
                .planPaymentDate(LocalDateTime.now())
                .build();
            newSubscription.setIsActive(true);
            return newSubscription;
        }
    }
}