package org.example.Usersvc.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 사용자 암호화 키 ARN 정보 엔티티 클래스
 * 
 * BYOK(Bring Your Own Key) 기능을 지원하기 위한 엔티티입니다.
 * 사용자가 자체 암호화 키를 AWS Secrets Manager에 저장하고,
 * 반환받은 ARN(Amazon Resource Name)을 시스템에서 관리합니다.
 * 
 * 주요 특징:
 * - AWS Secrets Manager ARN 정보 저장
 * - 사용자별 다중 키 관리 지원
 * - ARN 설명 정보 관리
 * - 생성 시간 자동 관리
 * - 암호화 키 추적 및 감사 지원
 * 
 * 데이터베이스 스키마:
 * - arn_id: VARCHAR(36) - UUID 기반 기본키
 * - user_id: VARCHAR(36) - 사용자 식별자 (User 테이블 참조)
 * - arn: VARCHAR(255) - AWS Secrets Manager ARN
 * - arn_description: VARCHAR(255) - ARN 설명 (선택적)
 * - created_at: DATETIME - 생성 시간 (자동 설정)
 * 
 * BYOK 워크플로우:
 * 1. 사용자가 자체 암호화 키를 제공
 * 2. 시스템이 AWS Secrets Manager에 키 저장
 * 3. AWS에서 반환된 ARN을 이 엔티티로 관리
 * 4. 암호화/복호화 시 ARN을 통해 키 참조
 */
@Entity
@Table(name = "user_secrets_arn",
       indexes = {
           @Index(name = "idx_user_id", columnList = "user_id"),
           @Index(name = "idx_arn", columnList = "arn")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = {})
public class UserSecretsArn {
    
    /**
     * ARN 정보 고유 식별자
     * 
     * UUID 형태의 문자열로 저장되며, 시스템 내부에서
     * ARN 정보를 식별하는 기본키입니다.
     * 
     * 특징:
     * - 36자 길이의 UUID 문자열
     * - 데이터베이스 기본키
     * - 시스템 내부 식별자로 사용
     */
    @Id
    @Column(name = "arn_id", length = 36, nullable = false)
    private String arnId;
    
    /**
     * 사용자 식별자
     * 
     * 이 ARN 정보를 소유한 사용자의 식별자입니다.
     * User 엔티티의 userId와 연결되지만, MSA 환경에서는
     * 외래키 제약조건을 사용하지 않습니다.
     * 
     * 특징:
     * - User 테이블의 user_id와 논리적 연결
     * - 사용자별 ARN 조회를 위한 인덱스 적용
     * - 36자 길이의 UUID 문자열
     */
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;
    
    /**
     * AWS Secrets Manager ARN
     * 
     * AWS Secrets Manager에 저장된 사용자 암호화 키의
     * ARN(Amazon Resource Name)입니다.
     * 
     * ARN 형식 예시:
     * arn:aws:secretsmanager:region:account-id:secret:name-suffix
     * 
     * 특징:
     * - AWS Secrets Manager에서 반환된 실제 ARN
     * - 암호화/복호화 작업 시 키 참조에 사용
     * - 최대 255자 길이
     * - 인덱스 적용으로 빠른 조회 지원
     */
    @Column(name = "arn", length = 255, nullable = false)
    private String arn;
    
    /**
     * ARN 설명 정보
     * 
     * 사용자가 ARN에 대한 설명이나 용도를 기록할 수 있는 필드입니다.
     * 선택적 정보로, null 값을 허용합니다.
     * 
     * 사용 예시:
     * - "개인 문서 암호화용 키"
     * - "API 토큰 암호화 키"
     * - "백업용 암호화 키"
     * 
     * 특징:
     * - 사용자 정의 설명 정보
     * - 선택적 정보 (null 허용)
     * - 최대 255자 길이
     * - 키 관리 및 구분 목적
     */
    @Column(name = "arn_description", length = 255)
    private String arnDescription;
    
    /**
     * ARN 정보 생성 시간
     * 
     * ARN 정보가 시스템에 처음 저장된 시간을 기록합니다.
     * 데이터베이스에서 자동으로 현재 시간으로 설정됩니다.
     * 
     * 특징:
     * - 자동 생성 시간 설정
     * - ARN 등록 시점 추적
     * - 감사(Audit) 정보로 활용
     * - 키 생성 시간 기반 정책 적용 가능
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
     * ARN 설명 업데이트 메서드
     * 
     * ARN에 대한 설명 정보를 업데이트합니다.
     * 사용자가 키의 용도나 설명을 변경할 때 사용됩니다.
     * 
     * @param description 새로운 ARN 설명 (null 또는 빈 문자열 허용)
     */
    public void updateDescription(String description) {
        if (description != null && !description.trim().isEmpty()) {
            this.arnDescription = description.trim();
        } else {
            this.arnDescription = null;
        }
    }
    
    /**
     * ARN 정보 유효성 검증 메서드
     * 
     * ARN 엔티티의 필수 정보가 모두 올바르게 설정되어 있는지 검증합니다.
     * 비즈니스 로직에서 ARN 정보의 유효성을 확인할 때 사용됩니다.
     * 
     * @return 유효한 ARN 정보인 경우 true, 그렇지 않으면 false
     */
    public boolean isValidArn() {
        return arnId != null && !arnId.trim().isEmpty() &&
               userId != null && !userId.trim().isEmpty() &&
               arn != null && !arn.trim().isEmpty() &&
               isValidArnFormat(arn) &&
               createdAt != null;
    }
    
    /**
     * AWS ARN 형식 유효성 검증 메서드
     * 
     * 제공된 ARN 문자열이 AWS ARN의 기본 형식을 따르는지 검증합니다.
     * 
     * AWS ARN 형식: arn:partition:service:region:account-id:resource
     * 
     * @param arnString 검증할 ARN 문자열
     * @return 유효한 ARN 형식인 경우 true, 그렇지 않으면 false
     */
    private boolean isValidArnFormat(String arnString) {
        if (arnString == null || arnString.trim().isEmpty()) {
            return false;
        }
        
        // 기본 ARN 형식 검증: arn:으로 시작하고 최소 6개의 콜론으로 구분된 부분을 가져야 함
        String[] arnParts = arnString.split(":");
        if (arnParts.length < 6 || !arnString.startsWith("arn:")) {
            return false;
        }
        
        // Secrets Manager ARN인지 확인
        return arnString.contains("secretsmanager");
    }
    
    /**
     * ARN이 최근에 생성된 것인지 확인하는 메서드
     * 
     * 생성 시간을 기준으로 최근 생성된 ARN인지 판단합니다.
     * 새로운 키에 대한 특별 처리나 모니터링에서 활용할 수 있습니다.
     * 
     * @param days 최근으로 간주할 일수
     * @return 지정된 일수 내에 생성된 ARN인 경우 true
     */
    public boolean isRecentlyCreated(int days) {
        if (createdAt == null) {
            return false;
        }
        return createdAt.isAfter(LocalDateTime.now().minusDays(days));
    }
    
    /**
     * ARN에서 시크릿 이름 추출 메서드
     * 
     * AWS Secrets Manager ARN에서 시크릿의 이름 부분을 추출합니다.
     * 로깅이나 사용자 인터페이스에서 표시 목적으로 사용됩니다.
     * 
     * @return 시크릿 이름, 추출할 수 없는 경우 전체 ARN 반환
     */
    public String getSecretName() {
        if (arn == null || !isValidArnFormat(arn)) {
            return arn;
        }
        
        try {
            // ARN 형식: arn:aws:secretsmanager:region:account:secret:name-suffix
            String[] parts = arn.split(":");
            if (parts.length >= 7) {
                String secretPart = parts[6];
                // suffix 제거 (마지막 하이픈과 6자리 문자)
                int lastHyphenIndex = secretPart.lastIndexOf("-");
                if (lastHyphenIndex > 0 && secretPart.length() - lastHyphenIndex == 7) {
                    return secretPart.substring(0, lastHyphenIndex);
                }
                return secretPart;
            }
        } catch (Exception e) {
            // 파싱 실패 시 전체 ARN 반환
        }
        
        return arn;
    }
    
    /**
     * ARN의 AWS 리전 정보 추출 메서드
     * 
     * ARN에서 AWS 리전 정보를 추출합니다.
     * 리전별 키 관리나 통계에서 활용할 수 있습니다.
     * 
     * @return AWS 리전 이름, 추출할 수 없는 경우 "unknown" 반환
     */
    public String getRegion() {
        if (arn == null || !isValidArnFormat(arn)) {
            return "unknown";
        }
        
        try {
            // ARN 형식: arn:aws:secretsmanager:region:account:secret:name
            String[] parts = arn.split(":");
            if (parts.length >= 4) {
                return parts[3];
            }
        } catch (Exception e) {
            // 파싱 실패 시 unknown 반환
        }
        
        return "unknown";
    }
    
    // equals와 hashCode 메서드 구현
    // JPA에서 엔티티의 동일성을 올바르게 비교하기 위해 기본키를 기준으로 구현합니다.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSecretsArn that = (UserSecretsArn) o;
        return Objects.equals(arnId, that.arnId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(arnId);
    }
}