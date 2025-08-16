package org.example.Usersvc.repository;

import org.example.Usersvc.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 데이터 액세스 인터페이스
 * 
 * User 엔티티에 대한 데이터베이스 연산을 제공하는 Repository 인터페이스입니다.
 * Spring Data JPA를 활용하여 기본적인 CRUD 연산과 
 * 사용자 도메인에 특화된 쿼리 메서드들을 제공합니다.
 * 
 * 주요 기능:
 * - 기본 CRUD 연산 (상속된 JpaRepository 기능)
 * - Auth0 ID 기반 사용자 조회
 * - 이메일 기반 사용자 조회
 * - 사용자 존재 여부 확인
 * - 생성 기간별 사용자 조회
 * 
 * 성능 최적화:
 * - 자주 사용되는 쿼리에 대한 인덱스 활용
 * - 필요시 커스텀 쿼리를 통한 성능 최적화
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    /**
     * Auth0 ID로 사용자 조회
     * 
     * Auth0에서 제공하는 사용자 식별자를 사용하여 
     * 시스템 내 사용자 정보를 조회합니다.
     * 
     * 사용 사례:
     * - Auth0 JWT 토큰 검증 후 사용자 정보 조회
     * - 외부 인증 시스템과의 사용자 매핑
     * - 로그인 프로세스에서 사용자 식별
     * 
     * @param auth0Id Auth0에서 제공하는 사용자 식별자 (예: "auth0|123456789")
     * @return 해당 Auth0 ID를 가진 사용자 정보, 없으면 Optional.empty()
     */
    Optional<User> findByAuth0Id(String auth0Id);
    
    /**
     * 이메일로 사용자 조회
     * 
     * 사용자의 이메일 주소를 사용하여 사용자 정보를 조회합니다.
     * 이메일은 사용자 식별의 중요한 수단이며, 
     * 다양한 비즈니스 로직에서 활용됩니다.
     * 
     * 사용 사례:
     * - 이메일 기반 사용자 검색
     * - 중복 가입 방지 체크
     * - 비밀번호 재설정 시 사용자 확인
     * - 사용자 초대 기능
     * 
     * @param userEmail 조회할 사용자의 이메일 주소
     * @return 해당 이메일을 가진 사용자 정보, 없으면 Optional.empty()
     */
    Optional<User> findByUserEmail(String userEmail);
    
    /**
     * Auth0 ID 존재 여부 확인
     * 
     * 특정 Auth0 ID를 가진 사용자가 시스템에 존재하는지 확인합니다.
     * 전체 사용자 객체를 조회하지 않고도 존재 여부만 확인할 수 있어
     * 성능상 유리합니다.
     * 
     * 사용 사례:
     * - 중복 가입 방지
     * - 사용자 존재 여부 빠른 확인
     * - 배치 작업에서 사용자 검증
     * 
     * @param auth0Id 확인할 Auth0 사용자 식별자
     * @return 존재하면 true, 존재하지 않으면 false
     */
    boolean existsByAuth0Id(String auth0Id);
    
    /**
     * 이메일 존재 여부 확인
     * 
     * 특정 이메일을 가진 사용자가 시스템에 존재하는지 확인합니다.
     * 이메일 중복 체크나 유효성 검증에 사용됩니다.
     * 
     * 사용 사례:
     * - 이메일 중복 가입 방지
     * - 이메일 유효성 검증
     * - 사용자 초대 전 기존 사용자 확인
     * 
     * @param userEmail 확인할 이메일 주소
     * @return 존재하면 true, 존재하지 않으면 false
     */
    boolean existsByUserEmail(String userEmail);
    
    /**
     * 생성 날짜 범위로 사용자 조회
     * 
     * 특정 기간 동안 생성된 사용자들을 조회합니다.
     * 통계 분석이나 특정 기간의 신규 사용자 관리에 활용됩니다.
     * 
     * 사용 사례:
     * - 특정 기간 신규 가입자 분석
     * - 마케팅 캠페인 효과 측정
     * - 월별/일별 가입자 통계
     * - 신규 사용자 온보딩 대상 선별
     * 
     * @param startDate 조회 시작 날짜 (포함)
     * @param endDate 조회 종료 날짜 (포함)
     * @return 해당 기간에 생성된 사용자 목록, 생성 시간 순으로 정렬
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate ORDER BY u.createdAt DESC")
    List<User> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate);
    
    /**
     * 특정 날짜 이후 생성된 사용자 조회
     * 
     * 지정된 날짜 이후에 생성된 모든 사용자를 조회합니다.
     * 최근 가입자 관리나 신규 사용자 대상 서비스에 활용됩니다.
     * 
     * 사용 사례:
     * - 최근 N일 내 가입한 신규 사용자 조회
     * - 신규 사용자 대상 환영 메시지 발송
     * - 최근 가입자 통계 분석
     * 
     * @param date 기준 날짜 (이 날짜 이후 생성된 사용자 조회)
     * @return 기준 날짜 이후 생성된 사용자 목록, 생성 시간 순으로 정렬
     */
    @Query("SELECT u FROM User u WHERE u.createdAt > :date ORDER BY u.createdAt DESC")
    List<User> findByCreatedAtAfter(@Param("date") LocalDateTime date);
    
    /**
     * 이메일 패턴으로 사용자 검색
     * 
     * 이메일 주소의 일부분이나 패턴을 사용하여 사용자를 검색합니다.
     * 관리자 기능이나 사용자 검색 기능에서 활용됩니다.
     * 
     * 사용 사례:
     * - 관리자의 사용자 검색 기능
     * - 특정 도메인 사용자 조회 (예: @company.com)
     * - 사용자 이름 기반 부분 검색
     * 
     * @param emailPattern 검색할 이메일 패턴 (% 와일드카드 사용 가능)
     * @return 패턴에 매치되는 사용자 목록, 이메일 순으로 정렬
     */
    @Query("SELECT u FROM User u WHERE u.userEmail LIKE :emailPattern ORDER BY u.userEmail")
    List<User> findByUserEmailContaining(@Param("emailPattern") String emailPattern);
    
    /**
     * 전체 사용자 수 조회 (최적화된 카운트 쿼리)
     * 
     * 시스템에 등록된 전체 사용자 수를 조회합니다.
     * count() 메서드보다 더 구체적인 쿼리로 성능을 최적화할 수 있습니다.
     * 
     * 사용 사례:
     * - 대시보드의 사용자 통계
     * - 시스템 현황 모니터링
     * - 페이징 처리를 위한 전체 건수 조회
     * 
     * @return 전체 사용자 수
     */
    @Query("SELECT COUNT(u) FROM User u")
    long countAllUsers();
    
    /**
     * 특정 기간 내 생성된 사용자 수 조회
     * 
     * 지정된 기간 동안 생성된 사용자의 수를 조회합니다.
     * 통계 분석이나 성장률 계산에 활용됩니다.
     * 
     * 사용 사례:
     * - 일별/월별 신규 가입자 통계
     * - 성장률 분석
     * - 마케팅 효과 측정
     * 
     * @param startDate 조회 시작 날짜
     * @param endDate 조회 종료 날짜
     * @return 해당 기간 내 생성된 사용자 수
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                @Param("endDate") LocalDateTime endDate);

    /**
     * Stripe 고객 ID로 사용자 조회
     * 
     * Stripe에서 제공하는 고객 식별자를 사용하여 
     * 시스템 내 사용자 정보를 조회합니다.
     * 
     * 사용 사례:
     * - Stripe 웹훅 처리 시 사용자 식별
     * - 결제 관련 이벤트 처리
     * - 구독 상태 업데이트
     * 
     * @param stripeCustomerId Stripe에서 제공하는 고객 식별자 (예: "cus_123456789")
     * @return 해당 Stripe 고객 ID를 가진 사용자 정보, 없으면 Optional.empty()
     */
    Optional<User> findByStripeCustomerId(String stripeCustomerId);
}