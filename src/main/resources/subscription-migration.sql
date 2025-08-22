-- ================================
-- 구독 데이터 무결성 개선 마이그레이션
-- 다중 구독 레코드를 단일 레코드로 통합
-- ================================

-- 실행 전 백업 생성 권장:
-- CREATE TABLE subscription_backup AS SELECT * FROM subscription;

-- 1. 현재 상태 분석 쿼리
-- 사용자별 구독 레코드 개수 확인
SELECT 
    user_id,
    COUNT(*) as subscription_count,
    SUM(CASE WHEN is_active = true THEN 1 ELSE 0 END) as active_count,
    SUM(CASE WHEN is_active = false THEN 1 ELSE 0 END) as inactive_count
FROM subscription 
GROUP BY user_id 
HAVING COUNT(*) > 1
ORDER BY subscription_count DESC;

-- 2. 다중 구독을 가진 사용자 상세 정보
SELECT 
    s.user_id,
    s.subscription_id,
    s.plan_id,
    p.plan_type,
    s.is_active,
    s.plan_payment_date,
    s.plan_update_date,
    s.billing_key,
    ROW_NUMBER() OVER (PARTITION BY s.user_id ORDER BY s.plan_update_date DESC, s.plan_payment_date DESC) as rn
FROM subscription s
JOIN plan p ON s.plan_id = p.plan_id
WHERE s.user_id IN (
    SELECT user_id 
    FROM subscription 
    GROUP BY user_id 
    HAVING COUNT(*) > 1
)
ORDER BY s.user_id, rn;

-- 3. 마이그레이션 실행 단계

-- Step 3-1: 사용자별 최신 구독만 활성화 (나머지는 비활성화)
-- 주의: 이 쿼리는 실제 데이터를 변경합니다!
UPDATE subscription s
SET is_active = false
WHERE s.subscription_id NOT IN (
    -- 각 사용자의 최신 구독 ID만 선택
    SELECT latest_sub.subscription_id
    FROM (
        SELECT s2.subscription_id,
               ROW_NUMBER() OVER (
                   PARTITION BY s2.user_id 
                   ORDER BY 
                       CASE WHEN s2.is_active = true THEN 0 ELSE 1 END,  -- 활성 구독 우선
                       s2.plan_update_date DESC,                          -- 최신 업데이트 우선
                       s2.plan_payment_date DESC,                         -- 최신 결제 우선
                       CASE WHEN p.plan_type = 'PRO' THEN 0 ELSE 1 END   -- PRO 플랜 우선
               ) as rn
        FROM subscription s2
        JOIN plan p ON s2.plan_id = p.plan_id
        WHERE s2.user_id IN (
            SELECT user_id 
            FROM subscription 
            GROUP BY user_id 
            HAVING COUNT(*) > 1
        )
    ) latest_sub
    WHERE latest_sub.rn = 1
)
AND s.user_id IN (
    SELECT user_id 
    FROM subscription 
    GROUP BY user_id 
    HAVING COUNT(*) > 1
);

-- Step 3-2: 최신 구독은 활성화 보장
UPDATE subscription s
SET is_active = true
WHERE s.subscription_id IN (
    SELECT latest_sub.subscription_id
    FROM (
        SELECT s2.subscription_id,
               ROW_NUMBER() OVER (
                   PARTITION BY s2.user_id 
                   ORDER BY 
                       CASE WHEN s2.is_active = true THEN 0 ELSE 1 END,
                       s2.plan_update_date DESC,
                       s2.plan_payment_date DESC,
                       CASE WHEN p.plan_type = 'PRO' THEN 0 ELSE 1 END
               ) as rn
        FROM subscription s2
        JOIN plan p ON s2.plan_id = p.plan_id
        WHERE s2.user_id IN (
            SELECT user_id 
            FROM subscription 
            GROUP BY user_id 
            HAVING COUNT(*) > 1
        )
    ) latest_sub
    WHERE latest_sub.rn = 1
);

-- Step 3-3: (선택사항) 비활성 구독 레코드 삭제
-- 주의: 이력이 완전히 사라집니다. 신중하게 결정하세요.
/*
DELETE FROM subscription 
WHERE is_active = false 
AND user_id IN (
    SELECT user_id 
    FROM (
        SELECT user_id 
        FROM subscription 
        GROUP BY user_id 
        HAVING COUNT(*) > 1
    ) multi_subs
);
*/

-- 4. 마이그레이션 결과 검증

-- 4-1: 사용자별 활성 구독 개수 확인 (모두 1이어야 함)
SELECT 
    user_id,
    COUNT(*) as total_subscriptions,
    SUM(CASE WHEN is_active = true THEN 1 ELSE 0 END) as active_subscriptions
FROM subscription 
GROUP BY user_id
HAVING SUM(CASE WHEN is_active = true THEN 1 ELSE 0 END) != 1
ORDER BY active_subscriptions DESC;

-- 4-2: 전체 구독 현황 요약
SELECT 
    'Total Users' as metric,
    COUNT(DISTINCT user_id) as count
FROM subscription
UNION ALL
SELECT 
    'Total Subscriptions' as metric,
    COUNT(*) as count
FROM subscription
UNION ALL
SELECT 
    'Active Subscriptions' as metric,
    COUNT(*) as count
FROM subscription 
WHERE is_active = true
UNION ALL
SELECT 
    'Inactive Subscriptions' as metric,
    COUNT(*) as count
FROM subscription 
WHERE is_active = false
UNION ALL
SELECT 
    'Users with Multiple Records' as metric,
    COUNT(*) as count
FROM (
    SELECT user_id 
    FROM subscription 
    GROUP BY user_id 
    HAVING COUNT(*) > 1
) multi_users;

-- 4-3: 플랜별 활성 구독 분포
SELECT 
    p.plan_type,
    COUNT(*) as active_subscriptions,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM subscription WHERE is_active = true), 2) as percentage
FROM subscription s
JOIN plan p ON s.plan_id = p.plan_id
WHERE s.is_active = true
GROUP BY p.plan_type
ORDER BY active_subscriptions DESC;

-- 5. 데이터 정합성 검증 쿼리들

-- 5-1: 고아 구독 레코드 확인 (user가 없는 구독)
SELECT s.subscription_id, s.user_id, 'Missing User' as issue
FROM subscription s
LEFT JOIN `user` u ON s.user_id = u.user_id
WHERE u.user_id IS NULL;

-- 5-2: 잘못된 플랜 참조 확인
SELECT s.subscription_id, s.plan_id, 'Invalid Plan' as issue
FROM subscription s
LEFT JOIN plan p ON s.plan_id = p.plan_id
WHERE p.plan_id IS NULL;

-- 5-3: 중복 활성 구독 확인 (마이그레이션 후 0이어야 함)
SELECT 
    user_id,
    COUNT(*) as active_count,
    GROUP_CONCAT(subscription_id) as subscription_ids
FROM subscription 
WHERE is_active = true 
GROUP BY user_id 
HAVING COUNT(*) > 1;

-- ================================
-- 실행 가이드
-- ================================

/*
1. 마이그레이션 전:
   - 데이터베이스 백업 필수
   - 1, 2번 분석 쿼리로 현재 상태 파악
   
2. 마이그레이션 실행:
   - Step 3-1, 3-2 순서대로 실행
   - 각 단계 후 4번 검증 쿼리로 확인
   
3. 마이그레이션 후:
   - 5번 정합성 검증 쿼리로 최종 확인
   - 애플리케이션 테스트 실행
   
4. Step 3-3 삭제는 선택사항:
   - 이력 보존이 중요하면 실행하지 않음
   - 깔끔한 데이터베이스 원하면 실행
*/