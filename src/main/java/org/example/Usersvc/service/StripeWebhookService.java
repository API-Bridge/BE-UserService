package org.example.Usersvc.service;

import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.Plan;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.example.Usersvc.repository.PlanRepository;
import org.example.Usersvc.repository.UserRepository;
import org.example.Usersvc.repository.UserSubscriptionRepository;
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

    /**
     * 체크아웃 세션 완료 처리
     * 결제가 완료되면 구독을 활성화
     */
    public void processCheckoutCompleted(Session session) {
        try {
            String customerId = session.getCustomer();
            String subscriptionId = session.getSubscription();
            
            log.info("체크아웃 완료 처리: customer={}, subscription={}", customerId, subscriptionId);
            
            // 고객 정보로 사용자 찾기
            Optional<User> userOptional = findUserByStripeCustomerId(customerId);
            if (userOptional.isEmpty()) {
                log.warn("Stripe 고객 ID로 사용자를 찾을 수 없음: {}", customerId);
                return;
            }
            
            User user = userOptional.get();
            
            // Stripe에서 구독 정보 조회
            if (subscriptionId != null) {
                try {
                    Subscription stripeSubscription = Subscription.retrieve(subscriptionId);
                    processSubscriptionCreated(stripeSubscription);
                } catch (Exception e) {
                    log.error("Stripe 구독 정보 조회 실패: {}", e.getMessage(), e);
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
                log.warn("Stripe 고객 ID로 사용자를 찾을 수 없음: {}", customerId);
                return;
            }
            
            User user = userOptional.get();
            
            // Price ID로 플랜 결정
            String priceId = stripeSubscription.getItems().getData().get(0).getPrice().getId();
            Optional<Plan> planOptional = findPlanByPriceId(priceId);
            if (planOptional.isEmpty()) {
                log.warn("Price ID로 플랜을 찾을 수 없음: {}", priceId);
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
            
            userSubscriptionRepository.save(userSubscription);
            
            log.info("사용자 구독 업데이트 완료: userId={}, planName={}, active={}", 
                user.getUserId(), plan.getPlanName(), userSubscription.getIsActive());
            
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
        if ("price_1RwJ5zDefwpzeE1sRhAYJn4b".equals(priceId)) {
            return planRepository.findByPlanName("Pro");
        }
        // Free 플랜은 기본값
        return planRepository.findByPlanName("Free");
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