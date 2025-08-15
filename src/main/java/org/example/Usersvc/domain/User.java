package org.example.Usersvc.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

// 사용자 엔티티 클래스
// 시스템의 핵심 사용자 정보를 저장하는 엔티티입니다.
// Auth0와 연동하여 외부 인증 시스템의 사용자 정보를 내부 시스템에서 관리할 수 있도록 설계되었습니다.
// 
// 주요 특징:
// - UUID 기반의 사용자 식별자 사용
// - Auth0 ID와의 매핑을 통한 외부 인증 연동
// - 이메일 기반의 사용자 식별 지원
// - 생성 시간 자동 관리
// 
// 데이터베이스 스키마:
// - user_id: VARCHAR(36) - UUID 기반 기본키
// - auth0_id: VARCHAR(255) - Auth0 사용자 식별자 (유니크)
// - user_email: VARCHAR(255) - 사용자 이메일 (유니크)
// - created_at: DATETIME - 생성 시간 (자동 설정)
@Entity
@Table(name = "users", 
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_auth0_id", columnNames = "auth0_id"),
           @UniqueConstraint(name = "uk_user_email", columnNames = "user_email")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = {})
public class User {
    
    /**
     * 사용자 고유 식별자
     * 
     * UUID 형태의 문자열로 저장되며, 시스템 내부에서 
     * 사용자를 식별하는 기본키입니다.
     * 
     * 특징:
     * - 36자 길이의 UUID 문자열
     * - 데이터베이스 기본키
     * - 외부 시스템에 노출되어도 안전한 식별자
     */
    @Id
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;
    
    /**
     * Auth0 사용자 식별자
     * 
     * Auth0에서 제공하는 외부 사용자 식별자입니다.
     * 일반적으로 "auth0|{숫자}" 형태로 구성됩니다.
     * 
     * 특징:
     * - Auth0와의 연동을 위한 핵심 필드
     * - 유니크 제약조건 적용
     * - Auth0 JWT 토큰의 subject와 매칭
     * - 최대 255자 길이
     */
    @Column(name = "auth0_id", length = 255, nullable = false, unique = true)
    private String auth0Id;
    
    /**
     * 사용자 이메일 주소
     * 
     * 사용자의 이메일 주소로, 사용자 식별 및 
     * 커뮤니케이션에 사용됩니다.
     * 
     * 특징:
     * - 사용자 식별의 중요한 수단
     * - 유니크 제약조건 적용
     * - Auth0 프로필 정보와 동기화
     * - 최대 255자 길이
     */
    @Column(name = "user_email", length = 255, nullable = false, unique = true)
    private String userEmail;
    
    /**
     * 사용자 생성 시간
     * 
     * 사용자 계정이 시스템에 처음 생성된 시간을 기록합니다.
     * 데이터베이스에서 자동으로 현재 시간으로 설정됩니다.
     * 
     * 특징:
     * - 자동 생성 시간 설정
     * - 사용자 가입 시점 추적
     * - 감사(Audit) 정보로 활용
     */
    @Column(name = "created_at", nullable = false, updatable = false, 
            columnDefinition = "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
    
    /**
     * 엔티티 저장 전 자동 실행되는 메서드
     * 
     * 생성 시간이 설정되지 않은 경우 현재 시간으로 자동 설정합니다.
     * JPA의 @PrePersist 어노테이션을 사용하여 저장 전에 실행됩니다.
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
    
    /**
     * 사용자 정보 업데이트 메서드
     * 
     * 사용자의 이메일 정보를 업데이트합니다.
     * 도메인 로직을 캡슐화하여 데이터 무결성을 보장합니다.
     * 
     * @param userEmail 새로운 사용자 이메일
     * @throws IllegalArgumentException 이메일이 null이거나 빈 문자열인 경우
     */
    public void updateEmail(String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 이메일은 필수입니다.");
        }
        this.userEmail = userEmail.trim();
    }
    
    /**
     * Auth0 ID 업데이트 메서드
     * 
     * Auth0 시스템에서 사용자 ID가 변경된 경우 업데이트합니다.
     * 일반적으로는 변경되지 않지만, 마이그레이션 등의 상황에서 사용될 수 있습니다.
     * 
     * @param auth0Id 새로운 Auth0 사용자 ID
     * @throws IllegalArgumentException Auth0 ID가 null이거나 빈 문자열인 경우
     */
    public void updateAuth0Id(String auth0Id) {
        if (auth0Id == null || auth0Id.trim().isEmpty()) {
            throw new IllegalArgumentException("Auth0 ID는 필수입니다.");
        }
        this.auth0Id = auth0Id.trim();
    }
    
    /**
     * 사용자 정보 유효성 검증 메서드
     * 
     * 사용자 엔티티의 필수 정보가 모두 올바르게 설정되어 있는지 검증합니다.
     * 비즈니스 로직에서 사용자 정보의 유효성을 확인할 때 사용됩니다.
     * 
     * @return 유효한 사용자 정보인 경우 true, 그렇지 않으면 false
     */
    public boolean isValidUser() {
        return userId != null && !userId.trim().isEmpty() &&
               auth0Id != null && !auth0Id.trim().isEmpty() &&
               userEmail != null && !userEmail.trim().isEmpty() &&
               createdAt != null;
    }
    
    /**
     * 사용자가 최근에 생성된 계정인지 확인하는 메서드
     * 
     * 생성 시간을 기준으로 최근 생성된 계정인지 판단합니다.
     * 신규 사용자 대상 특별 서비스나 온보딩 프로세스에서 활용할 수 있습니다.
     * 
     * @param days 최근으로 간주할 일수
     * @return 지정된 일수 내에 생성된 계정인 경우 true
     */
    public boolean isRecentlyCreated(int days) {
        if (createdAt == null) {
            return false;
        }
        return createdAt.isAfter(LocalDateTime.now().minusDays(days));
    }
    
    // equals와 hashCode 메서드 구현
    // JPA에서 엔티티의 동일성을 올바르게 비교하기 위해 기본키를 기준으로 구현합니다.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}