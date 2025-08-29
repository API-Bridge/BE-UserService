-- ================================
-- MySQL 테스트 데이터 스크립트 (TossPay 전용) - DDL + INSERT
-- ================================

-- Foreign Key 제약 조건 임시 비활성화
SET FOREIGN_KEY_CHECKS = 0;

-- 기존 테이블 삭제 (순서 중요)
DROP TABLE IF EXISTS active_user_record;
DROP TABLE IF EXISTS api_usage_record;
-- DROP TABLE IF EXISTS user_saved_api;  -- 공유 기능 비활성화
-- DROP TABLE IF EXISTS shared_api;      -- 공유 기능 비활성화
-- DROP TABLE IF EXISTS custom_api;      -- Custom API Service로 이관
DROP TABLE IF EXISTS user_secrets_arn;
DROP TABLE IF EXISTS subscription;
DROP TABLE IF EXISTS plan;
DROP TABLE IF EXISTS `user`;

-- 1. Plan 테이블 (구독 플랜) - 먼저 생성
CREATE TABLE plan (
    plan_id INT AUTO_INCREMENT PRIMARY KEY,
    plan_name ENUM('FREE', 'PRO') NOT NULL UNIQUE,
    price DECIMAL(10, 2) NOT NULL,
    description TEXT,
    features JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_plan_name (plan_name)
) ENGINE=InnoDB;

-- 2. User 테이블 (사용자) - 두 번째 생성
CREATE TABLE `user` (
    user_id VARCHAR(36) PRIMARY KEY,
    auth0_id VARCHAR(255) UNIQUE NOT NULL,
    user_email VARCHAR(255) NOT NULL UNIQUE,
    admin BOOLEAN NOT NULL DEFAULT FALSE COMMENT '관리자 권한 여부',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_email (user_email),
    INDEX idx_auth0_id (auth0_id),
    INDEX idx_admin (admin)
) ENGINE=InnoDB;

-- 3. Subscription 테이블 (사용자 구독) - TossPay 전용
CREATE TABLE subscription (
    subscription_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    plan_id INT NOT NULL,
    plan_payment_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    plan_update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    billing_key VARCHAR(255) NULL COMMENT 'TossPay 빌링키 (결제 자동화용)',
    payment_provider ENUM('STRIPE', 'TOSSPAY') DEFAULT 'TOSSPAY' COMMENT '결제 제공자',
    FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
    FOREIGN KEY (plan_id) REFERENCES plan(plan_id) ON DELETE RESTRICT,
    INDEX idx_billing_key (billing_key),
    INDEX idx_payment_provider (payment_provider),
    INDEX idx_user_id (user_id),
    INDEX idx_plan_id (plan_id)
) ENGINE=InnoDB;

-- 4. User Secrets ARN 테이블
CREATE TABLE user_secrets_arn (
    arn_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    arn VARCHAR(500) NOT NULL,
    arn_description VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_arn (arn)
) ENGINE=InnoDB;

-- 8. Active User Record 테이블 (DAU/MAU 측정용)
CREATE TABLE active_user_record (
    record_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    activity_type VARCHAR(50) NOT NULL DEFAULT 'API_CALL' COMMENT '활동 유형 (LOGIN, API_CALL, PAGE_VIEW)',
    active_date DATE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_active_date (active_date),
    INDEX idx_activity_type (activity_type),
    UNIQUE KEY uk_user_activity_date (user_id, activity_type, active_date)
) ENGINE=InnoDB;

-- 1. Plan 데이터 (planName enum에 맞게 생성)
INSERT INTO plan (plan_name, price, description, features) VALUES 
('FREE', 0.00, '무료 플랜 - 시작하기에 완벽', '["월 100회 API 호출", "분당 10회 제한", "시간당 100회 제한", "일일 1,000회 제한", "최대 5개 커스텀 API", "기본 지원", "커뮤니티 액세스"]'),
('PRO', 22.00, '프로 플랜 - 비즈니스용', '["월 10,000회 API 호출", "분당 60회 제한", "시간당 3,600회 제한", "일일 86,400회 제한", "최대 50개 커스텀 API", "우선 지원", "고급 분석", "API 키 관리", "TossPay 결제"]');

-- 2. User 데이터 (2025년 1월 기준, 테스트하기 좋은 다양한 시나리오)
-- 주의: 관리자 권한은 admin 컬럼으로 관리됩니다.
INSERT INTO `user` (user_id, auth0_id, user_email, admin, created_at) VALUES 
-- 🧪 기본 테스트용 사용자들 (API 테스트에 최적화)
-- 첫 번째 사용자를 관리자로 설정
('test-user-001', 'auth0|testuser001', 'test@example.com', true, '2024-12-01 10:00:00'),
('demo-user-001', 'auth0|demouser001', 'demo@test.com', false, '2024-12-01 10:30:00'),
('api-tester-001', 'auth0|apitester001', 'apitester@example.com', false, '2024-12-01 11:00:00'),

-- 🆓 다양한 FREE 플랜 사용자들
('free-user-001', 'google-oauth2|117885903921309558140', 'freeuser@gmail.com', false, '2024-12-15 09:00:00'),
('free-user-002', 'auth0|freelancer2024', 'freelancer@startup.io', false, '2024-12-16 10:30:00'),
('free-user-003', 'github|developer2024', 'coder@github.io', false, '2024-12-17 14:15:00'),
('free-user-004', 'auth0|student2024', 'student@university.edu', false, '2024-12-18 16:00:00'),
('free-user-005', 'google-oauth2|hobbyist123', 'hobby@personal.com', false, '2024-12-19 12:30:00'),

-- 💎 다양한 PRO 플랜 사용자들 (TossPay 구독 포함)
('pro-user-001', 'google-oauth2|business2024', 'business@company.com', false, '2024-11-01 09:00:00'),
('pro-user-002', 'auth0|enterprise2024', 'enterprise@bigcorp.com', false, '2024-11-05 10:15:00'),
('pro-user-003', 'auth0|startup2024', 'startup@unicorn.com', false, '2024-11-10 14:30:00'),
('pro-user-004', 'google-oauth2|agency2024', 'agency@marketing.com', false, '2024-11-15 11:45:00'),

-- 🔄 플랜 변경 이력이 있는 사용자들
('switcher-001', 'auth0|planswitcher01', 'switcher1@plans.com', false, '2024-10-01 12:00:00'),
('switcher-002', 'google-oauth2|upgrader2024', 'upgrader@growth.com', false, '2024-10-15 13:30:00'),

-- 🆕 최근 가입한 사용자들 (자동 FREE 구독 테스트용)
('new-user-001', 'google-oauth2|newbie2025', 'newbie@fresh.com', false, '2025-01-15 10:00:00'),
('new-user-002', 'auth0|recent2025', 'recent@signup.com', false, '2025-01-16 14:20:00'),
('new-user-003', 'github|justjoined2025', 'joined@today.com', false, '2025-01-17 16:45:00'),

-- 🔧 개발/QA 테스트용 특수 사용자들
('dev-test-001', 'auth0|devtest001', 'dev.test@internal.com', false, '2024-12-01 08:00:00'),
('qa-test-001', 'auth0|qatest001', 'qa.test@internal.com', false, '2024-12-01 08:30:00'),
('load-test-001', 'auth0|loadtest001', 'load.test@performance.com', false, '2024-12-01 09:00:00'),

-- 🚀 고활용 사용자들 (메트릭 테스트용)
('power-user-001', 'auth0|poweruser001', 'power@intensive.com', false, '2024-11-01 07:00:00'),
('heavy-user-001', 'google-oauth2|heavyuser001', 'heavy@usage.com', false, '2024-11-01 07:30:00');

-- 3. Subscription 데이터 (TossPay 전용으로 수정)
INSERT INTO subscription (subscription_id, user_id, plan_id, plan_payment_date, plan_update_date, billing_key, payment_provider) VALUES 
-- 🧪 기본 테스트 사용자들 (FREE)
('sub-test-001', 'test-user-001', 1, '2024-12-01 10:00:00', '2024-12-01 10:00:00', NULL, 'TOSSPAY'),
('sub-demo-001', 'demo-user-001', 1, '2024-12-01 10:30:00', '2024-12-01 10:30:00', NULL, 'TOSSPAY'),
('sub-apitester-001', 'api-tester-001', 1, '2024-12-01 11:00:00', '2024-12-01 11:00:00', NULL, 'TOSSPAY'),

-- 🆓 활성 FREE 구독 사용자들
('sub-free-001', 'free-user-001', 1, '2024-12-15 09:00:00', '2024-12-15 09:00:00', NULL, 'TOSSPAY'),
('sub-free-002', 'free-user-002', 1, '2024-12-16 10:30:00', '2024-12-16 10:30:00', NULL, 'TOSSPAY'),
('sub-free-003', 'free-user-003', 1, '2024-12-17 14:15:00', '2024-12-17 14:15:00', NULL, 'TOSSPAY'),
('sub-free-004', 'free-user-004', 1, '2024-12-18 16:00:00', '2024-12-18 16:00:00', NULL, 'TOSSPAY'),
('sub-free-005', 'free-user-005', 1, '2024-12-19 12:30:00', '2024-12-19 12:30:00', NULL, 'TOSSPAY'),

-- 💎 활성 PRO 구독 사용자들 (TossPay 빌링키 포함)
('sub-pro-001', 'pro-user-001', 2, '2024-11-01 09:00:00', '2024-11-01 09:00:00', 'BK_1QRxyzABCDEF123456', 'TOSSPAY'),
('sub-pro-002', 'pro-user-002', 2, '2024-11-05 10:15:00', '2024-11-05 10:15:00', 'BK_1QSabcDEFGHI789012', 'TOSSPAY'),
('sub-pro-003', 'pro-user-003', 2, '2024-11-10 14:30:00', '2024-11-10 14:30:00', 'BK_1QTdefGHIJKL345678', 'TOSSPAY'),
('sub-pro-004', 'pro-user-004', 2, '2024-11-15 11:45:00', '2024-11-15 11:45:00', 'BK_1QUghiJKLMNO456789', 'TOSSPAY'),

-- 🔄 플랜 변경 이력 (PRO로 업그레이드)
('sub-switch-old-001', 'switcher-001', 1, '2024-10-01 12:00:00', '2024-11-01 10:00:00', NULL, 'TOSSPAY'), -- 이전 FREE
('sub-switch-new-001', 'switcher-001', 2, '2024-11-01 10:00:00', '2024-11-01 10:00:00', 'BK_1QVjklMNOPQR567890', 'TOSSPAY'), -- 현재 PRO
('sub-switch-old-002', 'switcher-002', 1, '2024-10-15 13:30:00', '2024-11-15 15:00:00', NULL, 'TOSSPAY'), -- 이전 FREE  
('sub-switch-new-002', 'switcher-002', 2, '2024-11-15 15:00:00', '2024-11-15 15:00:00', 'BK_1QWmnoPQRSTU678901', 'TOSSPAY'), -- 현재 PRO

-- 🆕 최근 가입 사용자들 (자동 생성된 FREE 구독)
('sub-new-001', 'new-user-001', 1, '2025-01-15 10:00:00', '2025-01-15 10:00:00', NULL, 'TOSSPAY'),
('sub-new-002', 'new-user-002', 1, '2025-01-16 14:20:00', '2025-01-16 14:20:00', NULL, 'TOSSPAY'),
('sub-new-003', 'new-user-003', 1, '2025-01-17 16:45:00', '2025-01-17 16:45:00', NULL, 'TOSSPAY'),

-- 🔧 개발/QA 테스트용 구독
('sub-dev-001', 'dev-test-001', 1, '2024-12-01 08:00:00', '2024-12-01 08:00:00', NULL, 'TOSSPAY'),
('sub-qa-001', 'qa-test-001', 2, '2024-12-01 08:30:00', '2024-12-01 08:30:00', 'BK_1QXpqrSTUVWX789012', 'TOSSPAY'), -- QA용 PRO
('sub-load-001', 'load-test-001', 2, '2024-12-01 09:00:00', '2024-12-01 09:00:00', 'BK_1QYstUVWXYZ890123', 'TOSSPAY'), -- 부하 테스트용 PRO

-- 🚀 고활용 사용자들
('sub-power-001', 'power-user-001', 2, '2024-11-01 07:00:00', '2024-11-01 07:00:00', 'BK_1QZuvWXYZABC901234', 'TOSSPAY'),
('sub-heavy-001', 'heavy-user-001', 2, '2024-11-01 07:30:00', '2024-11-01 07:30:00', 'BK_1QAxyZABCDEF012345', 'TOSSPAY');

-- 4. User Secrets ARN 데이터 (API 키 테스트용)
INSERT INTO user_secrets_arn (arn_id, user_id, arn, arn_description, created_at) VALUES 
-- 🧪 기본 테스트 사용자들
('arn-test-001', 'test-user-001', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:test-api-key-AbCdEf', 'Test User API Key', '2024-12-01 10:30:00'),
('arn-demo-001', 'demo-user-001', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:demo-api-key-GhIjKl', 'Demo User API Key', '2024-12-01 11:00:00'),

-- 💎 PRO 사용자들의 API 키
('arn-pro-001', 'pro-user-001', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:business-api-key-XyZ123', 'Business User API Key', '2024-11-01 09:30:00'),
('arn-pro-002', 'pro-user-002', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:enterprise-api-key-Abc789', 'Enterprise User API Key', '2024-11-05 10:45:00'),
('arn-pro-003', 'pro-user-003', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:startup-api-key-Def456', 'Startup User API Key', '2024-11-10 15:00:00'),

-- 🔄 플랜 변경 사용자들
('arn-switch-001', 'switcher-001', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:switcher-api-key-Mno789', 'Switcher API Key', '2024-11-01 10:30:00'),
('arn-switch-002', 'switcher-002', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:upgrader-api-key-Pqr012', 'Upgrader API Key', '2024-11-15 15:30:00'),

-- 🔧 개발/QA 테스트용
('arn-qa-001', 'qa-test-001', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:qa-test-api-key-QaTest', 'QA Test API Key', '2024-12-01 09:00:00'),
('arn-load-001', 'load-test-001', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:load-test-api-key-LoadTest', 'Load Test API Key', '2024-12-01 09:30:00'),

-- 🚀 고활용 사용자들
('arn-power-001', 'power-user-001', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:power-api-key-PowerUser', 'Power User API Key', '2024-11-01 07:30:00'),
('arn-heavy-001', 'heavy-user-001', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:heavy-api-key-HeavyUser', 'Heavy User API Key', '2024-11-01 08:00:00');

-- 8. Active User Record 데이터 (DAU/MAU 측정용 활성 사용자 기록)
INSERT INTO active_user_record (record_id, user_id, activity_type, active_date, created_at) VALUES 
-- 🧪 테스트 사용자들 (최근 활동)
('active-test-001', 'test-user-001', 'API_CALL', '2025-01-19', '2025-01-19 09:00:00'),
('active-test-002', 'test-user-001', 'API_CALL', '2025-01-20', '2025-01-20 10:00:00'),
('active-test-003', 'test-user-001', 'LOGIN', '2025-01-20', '2025-01-20 08:30:00'),
('active-demo-001', 'demo-user-001', 'API_CALL', '2025-01-20', '2025-01-20 12:00:00'),
('active-api-001', 'api-tester-001', 'API_CALL', '2025-01-20', '2025-01-20 14:00:00'),

-- 🆓 FREE 사용자들 (다양한 활동 패턴)
('active-free-001', 'free-user-001', 'LOGIN', '2025-01-19', '2025-01-19 09:00:00'),
('active-free-002', 'free-user-001', 'API_CALL', '2025-01-20', '2025-01-20 10:00:00'),
('active-free-003', 'free-user-002', 'API_CALL', '2025-01-20', '2025-01-20 12:00:00'),
('active-free-004', 'free-user-003', 'LOGIN', '2025-01-20', '2025-01-20 15:00:00'),
('active-free-005', 'free-user-003', 'API_CALL', '2025-01-20', '2025-01-20 15:30:00'),
('active-free-006', 'free-user-004', 'API_CALL', '2025-01-20', '2025-01-20 17:00:00'),
('active-free-007', 'free-user-005', 'API_CALL', '2025-01-20', '2025-01-20 18:00:00'),

-- 💎 PRO 사용자들 (높은 활동성)
('active-pro-001', 'pro-user-001', 'LOGIN', '2025-01-19', '2025-01-19 08:00:00'),
('active-pro-002', 'pro-user-001', 'API_CALL', '2025-01-19', '2025-01-19 08:30:00'),
('active-pro-003', 'pro-user-001', 'API_CALL', '2025-01-20', '2025-01-20 09:00:00'),
('active-pro-004', 'pro-user-002', 'LOGIN', '2025-01-20', '2025-01-20 10:00:00'),
('active-pro-005', 'pro-user-002', 'API_CALL', '2025-01-20', '2025-01-20 10:30:00'),
('active-pro-006', 'pro-user-003', 'API_CALL', '2025-01-20', '2025-01-20 12:00:00'),
('active-pro-007', 'pro-user-004', 'API_CALL', '2025-01-20', '2025-01-20 13:45:00'),

-- 🔄 플랜 변경 사용자들 (업그레이드 후 활발한 활동)
('active-switch-001', 'switcher-001', 'API_CALL', '2024-10-30', '2024-10-30 11:00:00'), -- FREE 시절
('active-switch-002', 'switcher-001', 'LOGIN', '2025-01-19', '2025-01-19 11:30:00'), -- PRO 전환 후
('active-switch-003', 'switcher-001', 'API_CALL', '2025-01-19', '2025-01-19 12:00:00'),
('active-switch-004', 'switcher-001', 'API_CALL', '2025-01-20', '2025-01-20 14:00:00'),
('active-switch-005', 'switcher-002', 'API_CALL', '2025-01-20', '2025-01-20 16:00:00'),

-- 🆕 최근 가입 사용자들 (초기 활동)
('active-new-001', 'new-user-001', 'LOGIN', '2025-01-20', '2025-01-20 11:30:00'),
('active-new-002', 'new-user-001', 'API_CALL', '2025-01-20', '2025-01-20 12:00:00'),
('active-new-003', 'new-user-002', 'API_CALL', '2025-01-20', '2025-01-20 15:00:00'),
('active-new-004', 'new-user-003', 'API_CALL', '2025-01-20', '2025-01-20 17:00:00'),

-- 🔧 개발/QA 테스트용 (집중적인 테스트 활동)
('active-qa-001', 'qa-test-001', 'LOGIN', '2025-01-20', '2025-01-20 08:30:00'),
('active-qa-002', 'qa-test-001', 'API_CALL', '2025-01-20', '2025-01-20 09:00:00'),
('active-load-001', 'load-test-001', 'API_CALL', '2025-01-20', '2025-01-20 10:00:00'),
('active-dev-001', 'dev-test-001', 'API_CALL', '2025-01-20', '2025-01-20 11:00:00'),

-- 🚀 고활용 사용자들 (매우 활발한 활동)
('active-power-001', 'power-user-001', 'LOGIN', '2025-01-19', '2025-01-19 07:30:00'),
('active-power-002', 'power-user-001', 'API_CALL', '2025-01-19', '2025-01-19 08:00:00'),
('active-power-003', 'power-user-001', 'API_CALL', '2025-01-20', '2025-01-20 08:00:00'),
('active-heavy-001', 'heavy-user-001', 'LOGIN', '2025-01-20', '2025-01-20 08:30:00'),
('active-heavy-002', 'heavy-user-001', 'API_CALL', '2025-01-20', '2025-01-20 09:00:00'),

-- 📊 과거 데이터 (통계 테스트용)
('active-past-001', 'test-user-001', 'API_CALL', '2025-01-15', '2025-01-15 10:00:00'),
('active-past-002', 'pro-user-001', 'API_CALL', '2025-01-15', '2025-01-15 11:00:00'),
('active-past-003', 'free-user-001', 'API_CALL', '2025-01-16', '2025-01-16 12:00:00'),
('active-past-004', 'pro-user-002', 'API_CALL', '2025-01-17', '2025-01-17 13:00:00'),
('active-past-005', 'switcher-001', 'API_CALL', '2025-01-18', '2025-01-18 14:00:00');



-- Foreign Key 제약 조건 재활성화
SET FOREIGN_KEY_CHECKS = 1;