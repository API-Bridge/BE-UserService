package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.repository.UserRepository;
import org.example.Usersvc.util.HeaderUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 관리자 권한 검증 서비스
 * 
 * User 테이블의 admin 컬럼을 기반으로 관리자 권한을 검증합니다.
 * API Gateway에서 전달받은 헤더와 상관없이 데이터베이스의 실제 사용자 정보로 검증합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuthorizationService {
    
    private final UserRepository userRepository;
    
    /**
     * 사용자 ID를 통한 관리자 권한 검증
     * 
     * @param userId 검증할 사용자 ID
     * @return 관리자 권한이 있으면 true, 없으면 false
     */
    public boolean isAdmin(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.debug("빈 사용자 ID로 관리자 권한 검증 시도");
            return false;
        }
        
        // 헤더에서 실제 사용자 ID 추출
        String actualUserId = HeaderUtils.extractUserId(userId);
        
        try {
            Optional<User> userOpt = userRepository.findById(actualUserId);
            
            if (userOpt.isEmpty()) {
                log.debug("관리자 권한 검증 - 사용자를 찾을 수 없음: {}", actualUserId);
                return false;
            }
            
            User user = userOpt.get();
            boolean isAdmin = user.isAdmin();
            
            log.debug("관리자 권한 검증 - userId: {}, isAdmin: {}", actualUserId, isAdmin);
            
            return isAdmin;
            
        } catch (Exception e) {
            log.error("관리자 권한 검증 중 오류 발생 - userId: {}, error: {}", actualUserId, e.getMessage());
            return false;
        }
    }
    
    /**
     * 관리자 권한 검증 및 예외 발생
     * 관리자가 아닌 경우 IllegalAccessException을 발생시킵니다.
     * 
     * @param userId 검증할 사용자 ID
     * @throws IllegalAccessException 관리자 권한이 없는 경우
     */
    public void requireAdminRole(String userId) throws IllegalAccessException {
        if (!isAdmin(userId)) {
            String actualUserId = HeaderUtils.extractUserId(userId);
            log.warn("관리자 권한 없음 - 접근 거부: {}", actualUserId);
            throw new IllegalAccessException("관리자 권한이 필요합니다.");
        }
    }
    
    /**
     * Auth0 ID로 활성 상태인 관리자 권한 확인
     * 
     * @param auth0Id 검증할 Auth0 ID
     * @return 활성 상태인 관리자인 경우 true, 그렇지 않으면 false
     */
    public boolean isActiveAdminByAuth0Id(String auth0Id) {
        if (auth0Id == null || auth0Id.trim().isEmpty()) {
            return false;
        }
        
        try {
            Optional<User> userOpt = userRepository.findByAuth0Id(auth0Id);
            
            if (userOpt.isEmpty()) {
                log.debug("Auth0 ID로 사용자를 찾을 수 없음: {}", auth0Id);
                return false;
            }
            
            User user = userOpt.get();
            boolean isActiveAdmin = user.isAdmin();
            
            log.debug("Auth0 ID 기반 활성 관리자 권한 검증 - auth0Id: {}, userId: {}, isActiveAdmin: {}", 
                    auth0Id, user.getUserId(), isActiveAdmin);
            
            return isActiveAdmin;
            
        } catch (Exception e) {
            log.error("Auth0 ID 기반 관리자 권한 확인 중 오류 발생 - auth0Id: {}", auth0Id, e);
            return false;
        }
    }
    
    /**
     * 활성 상태인 관리자인지 검증
     * 관리자 권한이 있으면서 사용자 상태가 활성인 경우에만 true를 반환합니다.
     * 
     * @param userId 검증할 사용자 ID
     * @return 활성 상태인 관리자인 경우 true, 그렇지 않으면 false
     */
    public boolean isActiveAdmin(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        
        String actualUserId = HeaderUtils.extractUserId(userId);
        
        try {
            Optional<User> userOpt = userRepository.findById(actualUserId);
            
            if (userOpt.isEmpty()) {
                return false;
            }
            
            User user = userOpt.get();
            // status 필드 제거로 인해 모든 사용자는 활성 상태로 간주
            boolean isActiveAdmin = user.isAdmin();
            
            log.debug("활성 관리자 권한 검증 - userId: {}, isActiveAdmin: {}", actualUserId, isActiveAdmin);
            
            return isActiveAdmin;
            
        } catch (Exception e) {
            log.error("활성 관리자 권한 검증 중 오류 발생 - userId: {}, error: {}", actualUserId, e.getMessage());
            return false;
        }
    }
}