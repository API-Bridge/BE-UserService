package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.Plan;
import org.example.Usersvc.repository.UserRepository;
import org.example.Usersvc.repository.PlanRepository;
import org.example.Usersvc.repository.UserSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * 관리자 전용 서비스
 * 
 * 관리자 권한이 필요한 시스템 관리 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final UserService userService;

    /**
     * 사용자 완전한 정보 조회 (구독 정보 포함)
     */
    public Map<String, Object> getUserCompleteInfo(String userId) {
        User user = userRepository.findByAuth0Id(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getUserId());
        userInfo.put("auth0Id", user.getAuth0Id());
        userInfo.put("userEmail", user.getUserEmail());
        userInfo.put("createdAt", user.getCreatedAt());

        // 구독 정보 추가
        subscriptionRepository.findActiveSubscriptionByUser(user)
                .ifPresentOrElse(
                        subscription -> {
                            Map<String, Object> subscriptionInfo = new HashMap<>();
                            subscriptionInfo.put("subscriptionId", subscription.getSubscriptionId());
                            subscriptionInfo.put("planId", subscription.getPlan().getPlanId());
                            subscriptionInfo.put("planName", subscription.getPlan().getPlanName());
                            subscriptionInfo.put("planName", subscription.getPlan().getPlanName().name());
                            subscriptionInfo.put("price", subscription.getPlan().getPrice());
                            subscriptionInfo.put("paymentDate", subscription.getPlanPaymentDate());
                            subscriptionInfo.put("updateDate", subscription.getPlanUpdateDate());
                            subscriptionInfo.put("billingKey", subscription.getBillingKey());
                            subscriptionInfo.put("paymentProvider", subscription.getPaymentProvider());
                            userInfo.put("subscription", subscriptionInfo);
                        },
                        () -> userInfo.put("subscription", null)
                );

        return userInfo;
    }

    /**
     * 시스템 통계 조회
     */
    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 사용자 통계
        long totalUsers = userRepository.count();
        stats.put("totalUsers", totalUsers);
        
        // 구독 통계 (모든 구독이 활성 상태)
        long totalSubscriptions = subscriptionRepository.count();
        stats.put("totalSubscriptions", totalSubscriptions);
        stats.put("activeSubscriptions", totalSubscriptions);
        
        // 플랜별 통계 (모든 구독이 활성 상태)
        Map<String, Long> planStats = subscriptionRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        sub -> sub.getPlan().getPlanName().name(),
                        Collectors.counting()
                ));
        stats.put("planDistribution", planStats);
        
        return stats;
    }

    /**
     * 플랜별 사용자 분포 통계
     */
    public Map<String, Object> getPlanDistributionStatistics() {
        Map<String, Object> result = new HashMap<>();
        
        List<Plan> allPlans = planRepository.findAll();
        Map<String, Long> distribution = new HashMap<>();
        
        for (Plan plan : allPlans) {
            long count = subscriptionRepository.countByPlan(plan);
            distribution.put(plan.getPlanName().name(), count);
        }
        
        result.put("planDistribution", distribution);
        result.put("totalActivePlans", distribution.values().stream().mapToLong(Long::longValue).sum());
        
        return result;
    }

}