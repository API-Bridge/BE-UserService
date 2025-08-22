package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSubscription;
import org.example.Usersvc.repository.UserSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 구독 데이터 무결성 검증 및 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionIntegrityService {
    
    private final UserSubscriptionRepository userSubscriptionRepository;
    
    /**
     * 전체 시스템의 구독 데이터 무결성 검증
     * 
     * @return 검증 결과 리포트
     */
    @Transactional(readOnly = true)
    public IntegrityReport performFullIntegrityCheck() {
        log.info("구독 데이터 무결성 검증 시작");
        
        IntegrityReport report = new IntegrityReport();
        
        // 1. 다중 활성 구독을 가진 사용자 찾기
        List<User> usersWithMultipleActive = userSubscriptionRepository.findUsersWithMultipleActiveSubscriptions();
        report.setUsersWithMultipleActiveSubscriptions(usersWithMultipleActive);
        
        // 2. 전체 구독 통계 수집
        Object[] stats = userSubscriptionRepository.getSubscriptionStatistics();
        if (stats != null && stats.length >= 4) {
            report.setTotalUsers(((Number) stats[0]).longValue());
            report.setTotalSubscriptions(((Number) stats[1]).longValue());
            report.setActiveSubscriptions(((Number) stats[2]).longValue());
            report.setInactiveSubscriptions(((Number) stats[3]).longValue());
        }
        
        // 3. 각 문제 사용자의 구독 상세 정보 수집
        for (User user : usersWithMultipleActive) {
            List<UserSubscription> userSubs = userSubscriptionRepository.findAllSubscriptionsByUserOrderByLatest(user);
            long activeCount = userSubscriptionRepository.countActiveSubscriptionsByUser(user);
            long totalCount = userSubscriptionRepository.countSubscriptionsByUser(user);
            
            report.addUserIssueDetail(user.getUserId(), user.getUserEmail(), 
                                    activeCount, totalCount, userSubs);
        }
        
        // 4. 검증 결과 로깅
        log.info("구독 데이터 무결성 검증 완료 - 문제 사용자: {}, 전체 사용자: {}, 전체 구독: {}", 
                usersWithMultipleActive.size(), report.getTotalUsers(), report.getTotalSubscriptions());
        
        return report;
    }
    
    /**
     * 특정 사용자의 구독 무결성 검증
     */
    @Transactional(readOnly = true)
    public UserIntegrityStatus checkUserIntegrity(User user) {
        long activeCount = userSubscriptionRepository.countActiveSubscriptionsByUser(user);
        long totalCount = userSubscriptionRepository.countSubscriptionsByUser(user);
        List<UserSubscription> allSubs = userSubscriptionRepository.findAllSubscriptionsByUserOrderByLatest(user);
        
        UserIntegrityStatus status = new UserIntegrityStatus();
        status.setUserId(user.getUserId());
        status.setUserEmail(user.getUserEmail());
        status.setActiveSubscriptionCount(activeCount);
        status.setTotalSubscriptionCount(totalCount);
        status.setAllSubscriptions(allSubs);
        status.setHasIntegrityIssue(activeCount != 1); // 정상적으로는 활성 구독이 정확히 1개여야 함
        
        return status;
    }
    
    /**
     * 구독 무결성 자동 복구 (위험한 작업 - 신중하게 사용)
     * 다중 활성 구독을 가진 사용자의 최신 구독만 활성화하고 나머지는 비활성화
     */
    @Transactional
    public int repairMultipleActiveSubscriptions(boolean dryRun) {
        List<User> problemUsers = userSubscriptionRepository.findUsersWithMultipleActiveSubscriptions();
        int repairedCount = 0;
        
        log.warn("구독 무결성 자동 복구 시작 - 문제 사용자 수: {}, DRY RUN: {}", 
                problemUsers.size(), dryRun);
        
        for (User user : problemUsers) {
            List<UserSubscription> userSubs = userSubscriptionRepository.findAllSubscriptionsByUserOrderByLatest(user);
            
            // 첫 번째(최신) 구독만 활성화하고 나머지는 비활성화
            for (int i = 0; i < userSubs.size(); i++) {
                UserSubscription sub = userSubs.get(i);
                boolean shouldBeActive = (i == 0); // 첫 번째(최신)만 활성화
                
                if (sub.getIsActive() != shouldBeActive) {
                    log.info("구독 상태 변경: userId={}, subscriptionId={}, {} -> {}", 
                            user.getUserId(), sub.getSubscriptionId(), 
                            sub.getIsActive(), shouldBeActive);
                    
                    if (!dryRun) {
                        sub.setIsActive(shouldBeActive);
                        userSubscriptionRepository.save(sub);
                    }
                    repairedCount++;
                }
            }
        }
        
        log.info("구독 무결성 자동 복구 완료 - 수정된 구독 수: {}, DRY RUN: {}", repairedCount, dryRun);
        return repairedCount;
    }
    
    /**
     * 무결성 검증 리포트
     */
    public static class IntegrityReport {
        private List<User> usersWithMultipleActiveSubscriptions;
        private long totalUsers;
        private long totalSubscriptions;
        private long activeSubscriptions;
        private long inactiveSubscriptions;
        private Map<String, UserIntegrityStatus> userIssueDetails = new java.util.HashMap<>();
        
        // Getters and Setters
        public List<User> getUsersWithMultipleActiveSubscriptions() { return usersWithMultipleActiveSubscriptions; }
        public void setUsersWithMultipleActiveSubscriptions(List<User> users) { this.usersWithMultipleActiveSubscriptions = users; }
        
        public long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
        
        public long getTotalSubscriptions() { return totalSubscriptions; }
        public void setTotalSubscriptions(long totalSubscriptions) { this.totalSubscriptions = totalSubscriptions; }
        
        public long getActiveSubscriptions() { return activeSubscriptions; }
        public void setActiveSubscriptions(long activeSubscriptions) { this.activeSubscriptions = activeSubscriptions; }
        
        public long getInactiveSubscriptions() { return inactiveSubscriptions; }
        public void setInactiveSubscriptions(long inactiveSubscriptions) { this.inactiveSubscriptions = inactiveSubscriptions; }
        
        public Map<String, UserIntegrityStatus> getUserIssueDetails() { return userIssueDetails; }
        
        public void addUserIssueDetail(String userId, String userEmail, long activeCount, long totalCount, List<UserSubscription> subs) {
            UserIntegrityStatus status = new UserIntegrityStatus();
            status.setUserId(userId);
            status.setUserEmail(userEmail);
            status.setActiveSubscriptionCount(activeCount);
            status.setTotalSubscriptionCount(totalCount);
            status.setAllSubscriptions(subs);
            status.setHasIntegrityIssue(true);
            userIssueDetails.put(userId, status);
        }
        
        public boolean hasIntegrityIssues() {
            return usersWithMultipleActiveSubscriptions != null && !usersWithMultipleActiveSubscriptions.isEmpty();
        }
    }
    
    /**
     * 사용자별 무결성 상태
     */
    public static class UserIntegrityStatus {
        private String userId;
        private String userEmail;
        private long activeSubscriptionCount;
        private long totalSubscriptionCount;
        private boolean hasIntegrityIssue;
        private List<UserSubscription> allSubscriptions;
        
        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getUserEmail() { return userEmail; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
        
        public long getActiveSubscriptionCount() { return activeSubscriptionCount; }
        public void setActiveSubscriptionCount(long activeSubscriptionCount) { this.activeSubscriptionCount = activeSubscriptionCount; }
        
        public long getTotalSubscriptionCount() { return totalSubscriptionCount; }
        public void setTotalSubscriptionCount(long totalSubscriptionCount) { this.totalSubscriptionCount = totalSubscriptionCount; }
        
        public boolean isHasIntegrityIssue() { return hasIntegrityIssue; }
        public void setHasIntegrityIssue(boolean hasIntegrityIssue) { this.hasIntegrityIssue = hasIntegrityIssue; }
        
        public List<UserSubscription> getAllSubscriptions() { return allSubscriptions; }
        public void setAllSubscriptions(List<UserSubscription> allSubscriptions) { this.allSubscriptions = allSubscriptions; }
    }
}