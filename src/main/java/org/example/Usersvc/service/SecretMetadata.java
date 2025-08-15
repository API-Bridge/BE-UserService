package org.example.Usersvc.service;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * AWS Secrets Manager 시크릿 메타데이터 정보
 * 
 * AWS Secrets Manager에 저장된 시크릿의 메타데이터 정보를
 * 담는 데이터 클래스입니다. 실제 시크릿 값은 포함하지 않으며,
 * 시크릿의 생성 정보, 상태, 설정 등의 메타데이터만 포함합니다.
 * 
 * 주요 정보:
 * - 시크릿의 기본 식별 정보 (ARN, 이름)
 * - 생성 및 수정 시간 정보
 * - 시크릿 상태 및 설정 정보
 * - 암호화 및 보안 관련 설정
 */
@Getter
@Builder
public class SecretMetadata {
    
    /**
     * 시크릿의 ARN (Amazon Resource Name)
     * 
     * AWS에서 시크릿을 고유하게 식별하는 리소스 이름입니다.
     */
    private final String arn;
    
    /**
     * 시크릿 이름
     * 
     * 사용자가 지정한 시크릿의 이름입니다.
     */
    private final String name;
    
    /**
     * 시크릿 설명
     * 
     * 시크릿에 대한 설명 정보입니다.
     */
    private final String description;
    
    /**
     * 시크릿 생성 시간
     * 
     * 시크릿이 AWS Secrets Manager에 처음 생성된 시간입니다.
     */
    private final LocalDateTime createdDate;
    
    /**
     * 시크릿 마지막 수정 시간
     * 
     * 시크릿이 마지막으로 수정된 시간입니다.
     */
    private final LocalDateTime lastChangedDate;
    
    /**
     * 시크릿 마지막 접근 시간
     * 
     * 시크릿이 마지막으로 조회된 시간입니다.
     */
    private final LocalDateTime lastAccessedDate;
    
    /**
     * KMS 키 ID
     * 
     * 시크릿 암호화에 사용된 AWS KMS 키의 식별자입니다.
     */
    private final String kmsKeyId;
    
    /**
     * 시크릿 버전 ID
     * 
     * 현재 활성화된 시크릿 버전의 식별자입니다.
     */
    private final String versionId;
    
    /**
     * 삭제 예정 여부
     * 
     * 시크릿이 삭제 예정 상태인지 나타냅니다.
     */
    private final boolean deletionDate;
    
    /**
     * 자동 로테이션 활성화 여부
     * 
     * 시크릿의 자동 로테이션 기능이 활성화되어 있는지 나타냅니다.
     */
    private final boolean rotationEnabled;
    
    /**
     * 로테이션 주기 (일)
     * 
     * 자동 로테이션이 활성화된 경우의 로테이션 주기입니다.
     */
    private final Integer rotationIntervalDays;
    
    /**
     * AWS 리전
     * 
     * 시크릿이 저장된 AWS 리전입니다.
     */
    private final String region;
    
    /**
     * 시크릿이 최근에 생성되었는지 확인
     * 
     * @param days 최근으로 간주할 일수
     * @return 지정된 일수 내에 생성된 경우 true
     */
    public boolean isRecentlyCreated(int days) {
        if (createdDate == null) {
            return false;
        }
        return createdDate.isAfter(LocalDateTime.now().minusDays(days));
    }
    
    /**
     * 시크릿이 최근에 수정되었는지 확인
     * 
     * @param days 최근으로 간주할 일수
     * @return 지정된 일수 내에 수정된 경우 true
     */
    public boolean isRecentlyModified(int days) {
        if (lastChangedDate == null) {
            return false;
        }
        return lastChangedDate.isAfter(LocalDateTime.now().minusDays(days));
    }
    
    /**
     * 시크릿이 최근에 접근되었는지 확인
     * 
     * @param days 최근으로 간주할 일수
     * @return 지정된 일수 내에 접근된 경우 true
     */
    public boolean isRecentlyAccessed(int days) {
        if (lastAccessedDate == null) {
            return false;
        }
        return lastAccessedDate.isAfter(LocalDateTime.now().minusDays(days));
    }
    
    /**
     * 시크릿의 수명 계산 (생성부터 현재까지의 일수)
     * 
     * @return 시크릿이 생성된 후 경과된 일수
     */
    public long getAgeInDays() {
        if (createdDate == null) {
            return 0;
        }
        return java.time.Duration.between(createdDate, LocalDateTime.now()).toDays();
    }
    
    /**
     * ARN에서 시크릿 이름 추출
     * 
     * @return ARN에서 추출한 시크릿 이름
     */
    public String getNameFromArn() {
        if (arn == null || !arn.contains(":secret:")) {
            return name;
        }
        
        try {
            String secretPart = arn.substring(arn.indexOf(":secret:") + 8);
            int suffixIndex = secretPart.lastIndexOf("-");
            if (suffixIndex > 0 && secretPart.length() - suffixIndex == 7) {
                return secretPart.substring(0, suffixIndex);
            }
            return secretPart;
        } catch (Exception e) {
            return name;
        }
    }
}