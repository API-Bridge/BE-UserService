package org.example.Usersvc.service;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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
@Setter
public class SecretMetadata {
    // 시크릿의 ARN (Amazon Resource Name)
    // AWS에서 시크릿을 고유하게 식별하는 리소스 이름
    private String arn;

     // 시크릿 이름
     // 사용자가 지정한 시크릿의 이름
    private String name;
    

    //시크릿 설명
    //시크릿에 대한 설명 정보
    private String description;

    // 시크릿 생성 시간
    // 시크릿이 AWS Secrets Manager에 처음 생성된 시간
    private LocalDateTime createdDate;

    // 시크릿 마지막 수정 시간
    // 시크릿의 내용이나 설정이 마지막으로 변경된 시간
    private LocalDateTime lastChangedDate;

    // 시크릿 마지막 접근 시간
    // 시크릿이 마지막으로 조회되거나 사용된 시간
    private LocalDateTime lastAccessedDate;

    // KMS 키 ID는 시크릿의 보안을 유지하기 위해 사용됨
    private String kmsKeyId;

    // 시크릿 버전 ID
    // 현재 활성화된 시크릿 버전의 식별자
    private String versionId;

     // 삭제 예정 여부
     // 시크릿이 삭제 예정 상태인지 나타냅니다.
    // 시크릿이 삭제 예정 상태인 경우 true
    private boolean deletionDate;

    // true인 경우 자동 로테이션이 활성화
    // 시크릿의 자동 로테이션 기능이 활성화되어 있는지 나타냅니다.
    private boolean rotationEnabled;

    // 로테이션 주기 (일 단위)
    // 자동 로테이션이 활성화된 경우의 로테이션 주기를 나타냅니다.
    private Integer rotationIntervalDays;

    // AWS 리전 정보
    // 시크릿이 저장된 AWS 리전 (예: us-west-2)
    private String region;

    public SecretMetadata() {

    }

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