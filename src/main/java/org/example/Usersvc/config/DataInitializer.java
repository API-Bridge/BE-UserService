package org.example.Usersvc.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.Plan;
import org.example.Usersvc.domain.PlanName;
import org.example.Usersvc.repository.PlanRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 애플리케이션 시작 시 필요한 기본 데이터를 초기화합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final PlanRepository planRepository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("데이터 초기화 시작...");
        
        // Plan 데이터가 없으면 기본 플랜 생성
        if (planRepository.count() == 0) {
            log.info("Plan 테이블이 비어있습니다. 기본 플랜을 생성합니다.");
            initializePlans();
        } else {
            log.info("Plan 데이터가 이미 존재합니다. 개수: {}", planRepository.count());
        }
        
        log.info("데이터 초기화 완료");
    }

    private void initializePlans() {
        // FREE 플랜 생성
        Plan freePlan = Plan.builder()
                .planName(PlanName.FREE)
                .price(BigDecimal.ZERO)
                .description("무료 플랜 - 시작하기에 완벽")
                .features("[\"월 100회 API 호출\", \"분당 10회 제한\", \"시간당 100회 제한\", \"일일 1,000회 제한\", \"최대 5개 커스텀 API\", \"기본 지원\", \"커뮤니티 액세스\"]")
                .build();
        
        planRepository.save(freePlan);
        log.info("FREE 플랜 생성 완료: {}", freePlan.getPlanName());

        // PRO 플랜 생성
        Plan proPlan = Plan.builder()
                .planName(PlanName.PRO)
                .price(new BigDecimal("22.00"))
                .description("프로 플랜 - 비즈니스용")
                .features("[\"월 10,000회 API 호출\", \"분당 60회 제한\", \"시간당 3,600회 제한\", \"일일 86,400회 제한\", \"최대 50개 커스텀 API\", \"우선 지원\", \"고급 분석\", \"API 키 관리\", \"TossPay 결제\"]")
                .build();
        
        planRepository.save(proPlan);
        log.info("PRO 플랜 생성 완료: {}", proPlan.getPlanName());
    }
}