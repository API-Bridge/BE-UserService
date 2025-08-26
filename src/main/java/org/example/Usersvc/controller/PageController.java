package org.example.Usersvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.service.AdminAuthorizationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 테스트 페이지 접근용 컨트롤러
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PageController {

    private final AdminAuthorizationService adminAuthorizationService;

    /**
     * 토스페이 결제 테스트 페이지 (누구나 접근 가능)
     */
    @GetMapping("/payment-test")
    public String paymentTest() {
        return "payment-test";
    }
    
    /**
     * 통합 테스트 대시보드 (관리자 전용)
     */
    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String adminAuth0Id,
            HttpServletResponse response) throws IOException {
        
        log.info("관리자 대시보드 접근 시도 - userId: {}, adminAuth0Id: {}", userId, adminAuth0Id);
        
        // 관리자 권한 확인
        boolean isAdmin = false;
        
        if (userId != null && !userId.trim().isEmpty()) {
            // userId로 관리자 확인
            isAdmin = adminAuthorizationService.isActiveAdmin(userId);
            log.info("UserId 기반 관리자 확인 - userId: {}, isAdmin: {}", userId, isAdmin);
        } else if (adminAuth0Id != null && !adminAuth0Id.trim().isEmpty()) {
            // Auth0 ID로 관리자 확인
            isAdmin = adminAuthorizationService.isActiveAdminByAuth0Id(adminAuth0Id);
            log.info("Auth0 ID 기반 관리자 확인 - adminAuth0Id: {}, isAdmin: {}", adminAuth0Id, isAdmin);
        }
        
        if (!isAdmin) {
            log.warn("관리자 권한 없음 - 대시보드 접근 거부");
            response.sendError(403, "관리자 권한이 필요합니다. 액세스가 거부되었습니다.");
            return null;
        }
        
        log.info("관리자 권한 확인됨 - 대시보드 접근 허용");
        return "unified-test-dashboard";
    }
    
    /**
     * 루트 페이지 -> 결제 테스트로 리다이렉트 (관리자가 아닌 사용자용)
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/payment-test";
    }
}