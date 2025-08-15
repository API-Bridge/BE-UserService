package org.example.Usersvc.repository;

import org.example.Usersvc.domain.UserSecretsArn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 암호화 키 ARN 데이터 액세스 인터페이스
 * 
 * UserSecretsArn 엔티티에 대한 데이터베이스 연산을 제공하는 Repository 인터페이스입니다.
 * BYOK(Bring Your Own Key) 기능을 지원하기 위해 AWS Secrets Manager ARN 정보의
 * 저장, 조회, 관리 기능을 제공합니다.
 * 
 * 주요 기능:
 * - 기본 CRUD 연산 (상속된 JpaRepository 기능)
 * - 사용자별 ARN 목록 관리
 * - ARN 문자열 기반 조회
 * - ARN 존재 여부 확인
 * - 생성 기간별 ARN 조회
 * - 사용자별 ARN 개수 통계
 * 
 * 성능 최적화:
 * - user_id와 arn 필드에 인덱스 적용
 * - 자주 사용되는 쿼리 패턴 최적화
 * - 통계성 쿼리를 위한 효율적인 카운트 메서드
 */
@Repository
public interface UserSecretsArnRepository extends JpaRepository<UserSecretsArn, String> {
    
    /**
     * 사용자 ID로 ARN 목록 조회
     * 
     * 특정 사용자가 소유한 모든 ARN 정보를 조회합니다.
     * 사용자는 여러 개의 암호화 키를 관리할 수 있으므로,
     * 사용자별 키 목록 조회에 필수적인 기능입니다.
     * 
     * 사용 사례:
     * - 사용자의 키 관리 페이지에서 목록 표시
     * - 특정 사용자의 모든 키 정보 조회
     * - 사용자별 키 사용량 통계
     * - 키 삭제나 업데이트 시 사용자 검증
     * 
     * @param userId 조회할 사용자의 식별자
     * @return 해당 사용자의 ARN 목록, 생성 시간 역순으로 정렬 (최신순)
     */
    @Query("SELECT usa FROM UserSecretsArn usa WHERE usa.userId = :userId ORDER BY usa.createdAt DESC")
    List<UserSecretsArn> findByUserId(@Param("userId") String userId);
    
    /**
     * ARN 문자열로 조회
     * 
     * AWS Secrets Manager의 실제 ARN 문자열을 사용하여
     * 해당하는 ARN 정보를 조회합니다.
     * 
     * 사용 사례:
     * - AWS에서 반환받은 ARN으로 기존 등록 여부 확인
     * - 암호화/복호화 작업 시 ARN 정보 조회
     * - ARN 중복 등록 방지
     * - AWS 콜백에서 ARN 매칭
     * 
     * @param arn 조회할 AWS Secrets Manager ARN
     * @return 해당 ARN에 대한 정보, 없으면 Optional.empty()
     */
    Optional<UserSecretsArn> findByArn(String arn);
    
    /**
     * ARN 존재 여부 확인
     * 
     * 특정 ARN이 이미 시스템에 등록되어 있는지 확인합니다.
     * 전체 객체를 조회하지 않고도 존재 여부만 빠르게 확인할 수 있어
     * 성능상 유리합니다.
     * 
     * 사용 사례:
     * - ARN 중복 등록 방지
     * - AWS ARN 유효성 검증
     * - 배치 작업에서 ARN 존재 여부 확인
     * - API 요청 검증
     * 
     * @param arn 확인할 AWS Secrets Manager ARN
     * @return 존재하면 true, 존재하지 않으면 false
     */
    boolean existsByArn(String arn);
    
    /**
     * 사용자별 ARN 개수 조회
     * 
     * 특정 사용자가 소유한 ARN의 개수를 조회합니다.
     * 사용자별 키 관리 제한 정책이나 통계에 활용됩니다.
     * 
     * 사용 사례:
     * - 사용자별 키 개수 제한 검증
     * - 키 사용량 통계 및 분석
     * - 요금 계산 (키 개수 기반)
     * - 사용자별 저장 용량 관리
     * 
     * @param userId 조회할 사용자의 식별자
     * @return 해당 사용자가 소유한 ARN의 개수
     */
    long countByUserId(String userId);
    
    /**
     * 생성 날짜 범위로 ARN 조회
     * 
     * 특정 기간 동안 생성된 ARN 정보들을 조회합니다.
     * 통계 분석, 감사, 모니터링 목적으로 활용됩니다.
     * 
     * 사용 사례:
     * - 특정 기간 키 생성 통계
     * - 월별/일별 키 등록 분석
     * - 감사 보고서 작성
     * - 시스템 사용량 모니터링
     * 
     * @param startDate 조회 시작 날짜 (포함)
     * @param endDate 조회 종료 날짜 (포함)
     * @return 해당 기간에 생성된 ARN 목록, 생성 시간 순으로 정렬
     */
    @Query("SELECT usa FROM UserSecretsArn usa WHERE usa.createdAt BETWEEN :startDate AND :endDate ORDER BY usa.createdAt DESC")
    List<UserSecretsArn> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
    
    /**
     * 특정 날짜 이후 생성된 ARN 조회
     * 
     * 지정된 날짜 이후에 생성된 모든 ARN을 조회합니다.
     * 최근 등록된 키 관리나 신규 키 대상 작업에 활용됩니다.
     * 
     * 사용 사례:
     * - 최근 N일 내 등록된 키 조회
     * - 신규 키 대상 검증 작업
     * - 최근 키 등록 트렌드 분석
     * - 실시간 모니터링
     * 
     * @param date 기준 날짜 (이 날짜 이후 생성된 ARN 조회)
     * @return 기준 날짜 이후 생성된 ARN 목록, 생성 시간 순으로 정렬
     */
    @Query("SELECT usa FROM UserSecretsArn usa WHERE usa.createdAt > :date ORDER BY usa.createdAt DESC")
    List<UserSecretsArn> findByCreatedAtAfter(@Param("date") LocalDateTime date);
    
    /**
     * 설명 패턴으로 ARN 검색
     * 
     * ARN 설명에 특정 패턴이 포함된 ARN들을 검색합니다.
     * 관리자 기능이나 키 검색 기능에서 활용됩니다.
     * 
     * 사용 사례:
     * - 용도별 키 검색 (예: "API", "문서", "백업" 등)
     * - 관리자의 키 관리 기능
     * - 특정 목적의 키 일괄 관리
     * - 키 분류 및 정리
     * 
     * @param descriptionPattern 검색할 설명 패턴 (% 와일드카드 사용 가능)
     * @return 패턴에 매치되는 ARN 목록, 생성 시간 순으로 정렬
     */
    @Query("SELECT usa FROM UserSecretsArn usa WHERE usa.arnDescription LIKE :descriptionPattern ORDER BY usa.createdAt DESC")
    List<UserSecretsArn> findByArnDescriptionContaining(@Param("descriptionPattern") String descriptionPattern);
    
    /**
     * 사용자와 설명으로 ARN 조회
     * 
     * 특정 사용자의 ARN 중에서 설명에 특정 패턴이 포함된 것들을 조회합니다.
     * 사용자별 키 검색 및 관리에 활용됩니다.
     * 
     * 사용 사례:
     * - 사용자의 특정 용도 키 검색
     * - 사용자별 키 분류 관리
     * - 키 이름 기반 필터링
     * - 사용자 맞춤 키 목록 제공
     * 
     * @param userId 조회할 사용자의 식별자
     * @param descriptionPattern 검색할 설명 패턴
     * @return 조건에 매치되는 ARN 목록, 생성 시간 순으로 정렬
     */
    @Query("SELECT usa FROM UserSecretsArn usa WHERE usa.userId = :userId AND usa.arnDescription LIKE :descriptionPattern ORDER BY usa.createdAt DESC")
    List<UserSecretsArn> findByUserIdAndArnDescriptionContaining(@Param("userId") String userId, 
                                                                @Param("descriptionPattern") String descriptionPattern);
    
    /**
     * 전체 ARN 개수 조회 (최적화된 카운트 쿼리)
     * 
     * 시스템에 등록된 전체 ARN의 개수를 조회합니다.
     * 대시보드나 통계에서 활용됩니다.
     * 
     * 사용 사례:
     * - 시스템 현황 대시보드
     * - 전체 키 관리 통계
     * - 시스템 용량 모니터링
     * - 페이징 처리를 위한 전체 건수
     * 
     * @return 전체 ARN 개수
     */
    @Query("SELECT COUNT(usa) FROM UserSecretsArn usa")
    long countAllArns();
    
    /**
     * 특정 기간 내 생성된 ARN 개수 조회
     * 
     * 지정된 기간 동안 생성된 ARN의 개수를 조회합니다.
     * 기간별 통계 분석에 활용됩니다.
     * 
     * 사용 사례:
     * - 일별/월별 키 등록 통계
     * - 성장률 분석
     * - 사용량 트렌드 분석
     * - 보고서 작성
     * 
     * @param startDate 조회 시작 날짜
     * @param endDate 조회 종료 날짜
     * @return 해당 기간 내 생성된 ARN 개수
     */
    @Query("SELECT COUNT(usa) FROM UserSecretsArn usa WHERE usa.createdAt BETWEEN :startDate AND :endDate")
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                @Param("endDate") LocalDateTime endDate);
    
    /**
     * 최근 생성된 ARN 목록 조회 (제한된 개수)
     * 
     * 가장 최근에 생성된 ARN들을 지정된 개수만큼 조회합니다.
     * 대시보드나 최근 활동 표시에 활용됩니다.
     * 
     * 사용 사례:
     * - 대시보드의 최근 키 등록 현황
     * - 최근 활동 피드
     * - 실시간 모니터링 화면
     * - 관리자 알림
     * 
     * @param limit 조회할 최대 개수
     * @return 최근 생성된 ARN 목록 (생성 시간 역순)
     */
    @Query(value = "SELECT usa FROM UserSecretsArn usa ORDER BY usa.createdAt DESC")
    List<UserSecretsArn> findRecentArns(@Param("limit") int limit);
    
    /**
     * 특정 사용자의 가장 최근 ARN 조회
     * 
     * 특정 사용자가 가장 최근에 등록한 ARN 정보를 조회합니다.
     * 사용자별 최신 키 정보 표시에 활용됩니다.
     * 
     * 사용 사례:
     * - 사용자의 기본 키 표시
     * - 최근 사용한 키 자동 선택
     * - 사용자별 최신 활동 추적
     * - 키 추천 시스템
     * 
     * @param userId 조회할 사용자의 식별자
     * @return 해당 사용자의 가장 최근 ARN, 없으면 Optional.empty()
     */
    @Query("SELECT usa FROM UserSecretsArn usa WHERE usa.userId = :userId ORDER BY usa.createdAt DESC LIMIT 1")
    Optional<UserSecretsArn> findMostRecentByUserId(@Param("userId") String userId);
    
    /**
     * 설명이 없는 ARN 목록 조회
     * 
     * 설명 정보가 설정되지 않은 ARN들을 조회합니다.
     * 데이터 정리나 사용자 안내에 활용할 수 있습니다.
     * 
     * 사용 사례:
     * - 미완성 키 등록 정보 정리
     * - 사용자에게 설명 추가 안내
     * - 데이터 품질 관리
     * - 배치 정리 작업
     * 
     * @return 설명이 없는 ARN 목록, 생성 시간 순으로 정렬
     */
    @Query("SELECT usa FROM UserSecretsArn usa WHERE usa.arnDescription IS NULL OR usa.arnDescription = '' ORDER BY usa.createdAt DESC")
    List<UserSecretsArn> findArnsWithoutDescription();
}