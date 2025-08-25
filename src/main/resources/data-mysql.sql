-- ================================
-- MySQL 테스트 데이터 스크립트 (TossPay 전용) - DDL + INSERT
-- ================================

-- Foreign Key 제약 조건 임시 비활성화
SET FOREIGN_KEY_CHECKS = 0;

-- 기존 테이블 삭제 (순서 중요)
DROP TABLE IF EXISTS api_usage_record;
DROP TABLE IF EXISTS user_saved_api;
DROP TABLE IF EXISTS shared_api;
DROP TABLE IF EXISTS custom_api;
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
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_email (user_email),
    INDEX idx_auth0_id (auth0_id)
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

-- 5. Custom API 테이블
CREATE TABLE custom_api (
    custom_api_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_name (name)
) ENGINE=InnoDB;

-- 6. Shared API 테이블 
CREATE TABLE shared_api (
    shared_api_id VARCHAR(36) PRIMARY KEY,
    original_api_id VARCHAR(36) NOT NULL,
    creator_id VARCHAR(36) NOT NULL,
    api_name VARCHAR(255) NOT NULL,
    description TEXT,
    data_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (original_api_id) REFERENCES custom_api(custom_api_id) ON DELETE CASCADE,
    FOREIGN KEY (creator_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
    INDEX idx_creator_id (creator_id),
    INDEX idx_original_api_id (original_api_id),
    INDEX idx_api_name (api_name)
) ENGINE=InnoDB;

-- 7. User Saved API 테이블
CREATE TABLE user_saved_api (
    saved_api_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    shared_api_id VARCHAR(36) NOT NULL,
    api_name VARCHAR(255) NOT NULL,
    description TEXT,
    data_count INT DEFAULT 0,
    is_favorite BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
    FOREIGN KEY (shared_api_id) REFERENCES shared_api(shared_api_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_shared_api_id (shared_api_id)
) ENGINE=InnoDB;

-- 8. API Usage Record 테이블
CREATE TABLE api_usage_record (
    record_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    api_endpoint VARCHAR(255) NOT NULL,
    request_count INT DEFAULT 1,
    record_date DATE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_record_date (record_date),
    INDEX idx_api_endpoint (api_endpoint),
    UNIQUE KEY uk_user_api_date (user_id, api_endpoint, record_date)
) ENGINE=InnoDB;

-- 1. Plan 데이터 (planName enum에 맞게 생성)
INSERT INTO plan (plan_name, price, description, features) VALUES 
('FREE', 0.00, '무료 플랜 - 시작하기에 완벽', '["월 100회 API 호출", "분당 10회 제한", "시간당 100회 제한", "일일 1,000회 제한", "최대 5개 커스텀 API", "최대 3개 공유 API", "기본 지원", "커뮤니티 액세스"]'),
('PRO', 22.00, '프로 플랜 - 비즈니스용', '["월 10,000회 API 호출", "분당 60회 제한", "시간당 3,600회 제한", "일일 86,400회 제한", "최대 50개 커스텀 API", "최대 20개 공유 API", "우선 지원", "고급 분석", "API 키 관리", "Stripe 결제"]');

-- 2. User 데이터 (2025년 1월 기준, 테스트하기 좋은 다양한 시나리오)
-- 주의: 관리자 권한은 이제 Auth0 역할로 관리됩니다.
INSERT INTO `user` (user_id, auth0_id, user_email, created_at) VALUES 
-- 🧪 기본 테스트용 사용자들 (API 테스트에 최적화)
-- 관리자 권한은 이제 Auth0에서 'admin' 역할로 관리됩니다
('test-user-001', 'auth0|testuser001', 'test@example.com', '2024-12-01 10:00:00'),
('demo-user-001', 'auth0|demouser001', 'demo@test.com', '2024-12-01 10:30:00'),
('api-tester-001', 'auth0|apitester001', 'apitester@example.com', '2024-12-01 11:00:00'),

-- 🆓 다양한 FREE 플랜 사용자들
('free-user-001', 'google-oauth2|117885903921309558140', 'freeuser@gmail.com', '2024-12-15 09:00:00'),
('free-user-002', 'auth0|freelancer2024', 'freelancer@startup.io', '2024-12-16 10:30:00'),
('free-user-003', 'github|developer2024', 'coder@github.io', '2024-12-17 14:15:00'),
('free-user-004', 'auth0|student2024', 'student@university.edu', '2024-12-18 16:00:00'),
('free-user-005', 'google-oauth2|hobbyist123', 'hobby@personal.com', '2024-12-19 12:30:00'),

-- 💎 다양한 PRO 플랜 사용자들 (Stripe 구독 포함)
('pro-user-001', 'google-oauth2|business2024', 'business@company.com', '2024-11-01 09:00:00'),
('pro-user-002', 'auth0|enterprise2024', 'enterprise@bigcorp.com', '2024-11-05 10:15:00'),
('pro-user-003', 'auth0|startup2024', 'startup@unicorn.com', '2024-11-10 14:30:00'),
('pro-user-004', 'google-oauth2|agency2024', 'agency@marketing.com', '2024-11-15 11:45:00'),

-- 🔄 플랜 변경 이력이 있는 사용자들
('switcher-001', 'auth0|planswitcher01', 'switcher1@plans.com', '2024-10-01 12:00:00'),
('switcher-002', 'google-oauth2|upgrader2024', 'upgrader@growth.com', '2024-10-15 13:30:00'),

-- 🆕 최근 가입한 사용자들 (자동 FREE 구독 테스트용)
('new-user-001', 'google-oauth2|newbie2025', 'newbie@fresh.com', '2025-01-15 10:00:00'),
('new-user-002', 'auth0|recent2025', 'recent@signup.com', '2025-01-16 14:20:00'),
('new-user-003', 'github|justjoined2025', 'joined@today.com', '2025-01-17 16:45:00'),

-- 🔧 개발/QA 테스트용 특수 사용자들
('dev-test-001', 'auth0|devtest001', 'dev.test@internal.com', '2024-12-01 08:00:00'),
('qa-test-001', 'auth0|qatest001', 'qa.test@internal.com', '2024-12-01 08:30:00'),
('load-test-001', 'auth0|loadtest001', 'load.test@performance.com', '2024-12-01 09:00:00'),

-- 🚀 고활용 사용자들 (메트릭 테스트용)
('power-user-001', 'auth0|poweruser001', 'power@intensive.com', '2024-11-01 07:00:00'),
('heavy-user-001', 'google-oauth2|heavyuser001', 'heavy@usage.com', '2024-11-01 07:30:00');

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

-- 5. Custom API 데이터 (테스트하기 좋은 다양한 API)
INSERT INTO custom_api (custom_api_id, user_id, name, description, created_at, updated_at) VALUES 
-- 🧪 기본 테스트용 APIs
('api-test-001', 'test-user-001', 'Simple Weather API', 'Basic weather information service', '2024-12-01 10:30:00', '2024-12-01 10:30:00'),
('api-test-002', 'test-user-001', 'News Feed API', 'Latest news articles', '2024-12-01 11:00:00', '2024-12-01 11:00:00'),
('api-demo-001', 'demo-user-001', 'Demo Calculator API', 'Mathematical operations API', '2024-12-01 11:30:00', '2024-12-01 11:30:00'),

-- 🆓 FREE 사용자들의 APIs (제한된 개수)
('api-free-001', 'free-user-001', 'Personal Weather', 'Personal weather tracking', '2024-12-15 10:00:00', '2024-12-15 10:00:00'),
('api-free-002', 'free-user-001', 'Todo List API', 'Simple task management', '2024-12-15 11:00:00', '2024-12-15 11:00:00'),
('api-free-003', 'free-user-002', 'Freelance Timer', 'Time tracking for freelancers', '2024-12-16 11:00:00', '2024-12-16 11:00:00'),
('api-free-004', 'free-user-003', 'Code Snippets', 'Programming code examples', '2024-12-17 15:00:00', '2024-12-17 15:00:00'),
('api-free-005', 'free-user-003', 'Git Helper API', 'Git command assistance', '2024-12-17 16:00:00', '2024-12-17 16:00:00'),

-- 💎 PRO 사용자들의 APIs (풍부하고 복잡한 기능)
('api-pro-001', 'pro-user-001', 'Advanced Analytics Dashboard', 'Real-time business analytics', '2024-11-01 10:00:00', '2024-11-01 10:00:00'),
('api-pro-002', 'pro-user-001', 'Customer CRM Integration', 'Complete customer management', '2024-11-02 09:30:00', '2024-11-02 09:30:00'),
('api-pro-003', 'pro-user-001', 'Payment Gateway API', 'Secure payment processing', '2024-11-03 14:45:00', '2024-11-03 14:45:00'),
('api-pro-004', 'pro-user-001', 'Inventory Management', 'Real-time inventory tracking', '2024-11-04 11:20:00', '2024-11-04 11:20:00'),
('api-pro-005', 'pro-user-002', 'Enterprise SSO', 'Single sign-on authentication', '2024-11-05 11:00:00', '2024-11-05 11:00:00'),
('api-pro-006', 'pro-user-002', 'Data Pipeline API', 'Big data processing pipeline', '2024-11-06 10:15:00', '2024-11-06 10:15:00'),
('api-pro-007', 'pro-user-002', 'ML Model Deployment', 'Machine learning model API', '2024-11-07 13:30:00', '2024-11-07 13:30:00'),
('api-pro-008', 'pro-user-003', 'Startup Metrics API', 'KPI tracking for startups', '2024-11-10 15:00:00', '2024-11-10 15:00:00'),

-- 🔄 플랜 변경 사용자들의 APIs (업그레이드 후 더 많은 API)
('api-switch-001', 'switcher-001', 'Stock Analysis API', 'Advanced stock market analysis', '2024-10-15 11:00:00', '2024-11-02 09:00:00'),
('api-switch-002', 'switcher-001', 'Crypto Trading Bot', 'Automated crypto trading', '2024-11-02 10:00:00', '2024-11-02 10:00:00'),
('api-switch-003', 'switcher-001', 'Portfolio Tracker', 'Investment portfolio management', '2024-11-05 14:00:00', '2024-11-05 14:00:00'),
('api-switch-004', 'switcher-002', 'Growth Metrics API', 'Business growth analytics', '2024-11-15 16:00:00', '2024-11-15 16:00:00'),

-- 🔧 개발/QA 테스트용 APIs
('api-qa-001', 'qa-test-001', 'QA Testing API', 'Automated testing endpoints', '2024-12-01 09:00:00', '2024-12-01 09:00:00'),
('api-load-001', 'load-test-001', 'Load Test API', 'Performance testing endpoints', '2024-12-01 09:30:00', '2024-12-01 09:30:00'),

-- 🚀 고활용 사용자들의 APIs
('api-power-001', 'power-user-001', 'High Volume API', 'Heavy traffic processing', '2024-11-01 08:00:00', '2024-11-01 08:00:00'),
('api-heavy-001', 'heavy-user-001', 'Intensive Processing API', 'CPU intensive operations', '2024-11-01 08:30:00', '2024-11-01 08:30:00');

-- 6. Shared API 데이터 (활발한 공유 환경)
INSERT INTO shared_api (shared_api_id, original_api_id, creator_id, api_name, description, data_count, created_at, updated_at) VALUES 
-- 🧪 테스트용 공유 APIs
('shared-test-001', 'api-test-001', 'test-user-001', 'Public Weather API', 'Free weather data for testing', 120, '2024-12-05 10:00:00', '2024-12-05 10:00:00'),
('shared-demo-001', 'api-demo-001', 'demo-user-001', 'Calculator Service', 'Demo calculation service', 85, '2024-12-05 11:00:00', '2024-12-05 11:00:00'),

-- 🆓 FREE 사용자들의 공유
('shared-free-001', 'api-free-001', 'free-user-001', 'Community Weather', 'Community weather service', 95, '2024-12-20 10:00:00', '2024-12-20 10:00:00'),
('shared-free-002', 'api-free-004', 'free-user-003', 'Code Examples Library', 'Programming snippets collection', 156, '2024-12-20 16:00:00', '2024-12-20 16:00:00'),
('shared-free-003', 'api-free-005', 'free-user-003', 'Git Commands Helper', 'Git command reference API', 203, '2024-12-21 09:00:00', '2024-12-21 09:00:00'),

-- 💎 PRO 사용자들의 공유 (고품질 서비스)
('shared-pro-001', 'api-pro-001', 'pro-user-001', 'Business Analytics Suite', 'Professional analytics tools', 847, '2024-11-15 11:00:00', '2024-11-15 11:00:00'),
('shared-pro-002', 'api-pro-005', 'pro-user-002', 'Enterprise Auth System', 'Enterprise-grade authentication', 562, '2024-11-20 14:30:00', '2024-11-20 14:30:00'),
('shared-pro-003', 'api-pro-006', 'pro-user-002', 'Data Processing Pipeline', 'Big data processing service', 731, '2024-11-25 16:00:00', '2024-11-25 16:00:00'),
('shared-pro-004', 'api-pro-008', 'pro-user-003', 'Startup KPI Tracker', 'Startup metrics dashboard', 394, '2024-11-30 12:00:00', '2024-11-30 12:00:00'),

-- 🔄 플랜 변경 사용자들의 공유
('shared-switch-001', 'api-switch-001', 'switcher-001', 'Stock Insights Pro', 'Advanced stock analysis', 425, '2024-11-10 12:00:00', '2024-11-10 12:00:00'),
('shared-switch-002', 'api-switch-004', 'switcher-002', 'Growth Analytics', 'Business growth tracking', 287, '2024-12-01 15:00:00', '2024-12-01 15:00:00');

-- 7. User Saved API 데이터 (다양한 저장 패턴)
INSERT INTO user_saved_api (saved_api_id, user_id, shared_api_id, api_name, description, data_count, is_favorite, created_at, updated_at) VALUES 
-- 🧪 테스트 사용자들의 저장
('saved-test-001', 'test-user-001', 'shared-pro-001', 'Business Analytics', 'Testing analytics features', 25, false, '2024-12-10 14:00:00', '2024-12-10 14:00:00'),
('saved-demo-001', 'demo-user-001', 'shared-free-002', 'Code Examples', 'Demo code samples', 12, false, '2024-12-10 15:00:00', '2024-12-10 15:00:00'),

-- 🆓 FREE 사용자들의 저장 (학습 목적)
('saved-free-001', 'free-user-001', 'shared-pro-001', 'Analytics for Learning', 'Learning business analytics', 18, false, '2024-12-22 09:00:00', '2024-12-22 09:00:00'),
('saved-free-002', 'free-user-002', 'shared-free-003', 'Git Commands', 'Git reference guide', 32, false, '2024-12-22 11:00:00', '2024-12-22 11:00:00'),
('saved-free-003', 'free-user-003', 'shared-switch-001', 'Stock Analysis Study', 'Learning stock analysis', 8, false, '2024-12-22 14:30:00', '2024-12-22 14:30:00'),
('saved-free-004', 'free-user-004', 'shared-pro-002', 'Auth System Study', 'Learning authentication', 15, false, '2024-12-22 14:30:00', '2024-12-22 14:30:00'),

-- 💎 PRO 사용자들의 저장 (비즈니스 활용)
('saved-pro-001', 'pro-user-001', 'shared-switch-001', 'Stock Analysis for Business', 'Business investment analysis', 78, false, '2024-11-25 10:00:00', '2024-11-25 10:00:00'),
('saved-pro-002', 'pro-user-002', 'shared-pro-001', 'Cross-Company Analytics', 'Inter-company analytics', 134, false, '2024-11-26 15:00:00', '2024-11-26 15:00:00'),
('saved-pro-003', 'pro-user-003', 'shared-pro-003', 'Data Pipeline Integration', 'Startup data processing', 89, false, '2024-12-01 11:00:00', '2024-12-01 11:00:00'),
('saved-pro-004', 'pro-user-004', 'shared-switch-002', 'Growth Metrics Analysis', 'Agency growth tracking', 67, false, '2024-12-05 13:00:00', '2024-12-05 13:00:00'),

-- 🔧 개발/QA 테스트용 저장
('saved-qa-001', 'qa-test-001', 'shared-test-001', 'QA Weather Testing', 'QA testing weather API', 45, false, '2024-12-10 09:30:00', '2024-12-10 09:30:00'),
('saved-load-001', 'load-test-001', 'shared-pro-003', 'Load Test Data Pipeline', 'Performance testing pipeline', 156, false, '2024-12-10 10:00:00', '2024-12-10 10:00:00');

-- 8. API Usage Record 데이터 (현재 날짜 기준 현실적인 사용 패턴)
INSERT INTO api_usage_record (record_id, user_id, api_endpoint, request_count, record_date, created_at) VALUES 
-- 🧪 테스트 사용자들 (오늘과 어제 데이터)
('usage-test-001', 'test-user-001', '/api/weather', 25, '2025-01-19', '2025-01-19 09:00:00'),
('usage-test-002', 'test-user-001', '/api/weather', 18, '2025-01-20', '2025-01-20 10:00:00'),
('usage-test-003', 'test-user-001', '/api/news', 12, '2025-01-20', '2025-01-20 11:00:00'),
('usage-demo-001', 'demo-user-001', '/api/calculator', 8, '2025-01-20', '2025-01-20 12:00:00'),

-- 🆓 FREE 사용자들 (제한된 사용량 - 일일 100회 이하)
('usage-free-001', 'free-user-001', '/api/weather', 35, '2025-01-19', '2025-01-19 09:00:00'),
('usage-free-002', 'free-user-001', '/api/weather', 42, '2025-01-20', '2025-01-20 10:00:00'),
('usage-free-003', 'free-user-001', '/api/todo', 18, '2025-01-20', '2025-01-20 11:30:00'),
('usage-free-004', 'free-user-002', '/api/timer', 28, '2025-01-20', '2025-01-20 12:00:00'),
('usage-free-005', 'free-user-003', '/api/code-snippets', 67, '2025-01-20', '2025-01-20 15:00:00'),
('usage-free-006', 'free-user-003', '/api/git-helper', 23, '2025-01-20', '2025-01-20 16:00:00'),
('usage-free-007', 'free-user-004', '/api/study', 15, '2025-01-20', '2025-01-20 17:00:00'),
('usage-free-008', 'free-user-005', '/api/hobby', 9, '2025-01-20', '2025-01-20 18:00:00'),

-- 💎 PRO 사용자들 (높은 사용량 - 일일 1000-5000회)
('usage-pro-001', 'pro-user-001', '/api/analytics', 1247, '2025-01-19', '2025-01-19 08:00:00'),
('usage-pro-002', 'pro-user-001', '/api/analytics', 1356, '2025-01-20', '2025-01-20 09:00:00'),
('usage-pro-003', 'pro-user-001', '/api/crm', 832, '2025-01-20', '2025-01-20 09:30:00'),
('usage-pro-004', 'pro-user-001', '/api/payments', 456, '2025-01-20', '2025-01-20 14:15:00'),
('usage-pro-005', 'pro-user-001', '/api/inventory', 623, '2025-01-20', '2025-01-20 16:00:00'),
('usage-pro-006', 'pro-user-002', '/api/enterprise-auth', 743, '2025-01-20', '2025-01-20 10:00:00'),
('usage-pro-007', 'pro-user-002', '/api/data-pipeline', 1789, '2025-01-20', '2025-01-20 11:30:00'),
('usage-pro-008', 'pro-user-002', '/api/ml-model', 567, '2025-01-20', '2025-01-20 16:30:00'),
('usage-pro-009', 'pro-user-003', '/api/startup-metrics', 892, '2025-01-20', '2025-01-20 12:00:00'),
('usage-pro-010', 'pro-user-004', '/api/agency-analytics', 634, '2025-01-20', '2025-01-20 13:45:00'),

-- 🔄 플랜 변경 사용자들 (업그레이드 후 사용량 급증)
('usage-switch-001', 'switcher-001', '/api/stock-analysis', 89, '2024-10-30', '2024-10-30 11:00:00'), -- FREE 시절
('usage-switch-002', 'switcher-001', '/api/stock-analysis', 1234, '2025-01-19', '2025-01-19 12:00:00'), -- PRO 전환 후
('usage-switch-003', 'switcher-001', '/api/crypto-bot', 756, '2025-01-20', '2025-01-20 14:00:00'),
('usage-switch-004', 'switcher-001', '/api/portfolio', 432, '2025-01-20', '2025-01-20 15:30:00'),
('usage-switch-005', 'switcher-002', '/api/growth-metrics', 687, '2025-01-20', '2025-01-20 16:00:00'),

-- 🆕 최근 가입 사용자들 (낮은 사용량)
('usage-new-001', 'new-user-001', '/api/weather', 5, '2025-01-20', '2025-01-20 12:00:00'),
('usage-new-002', 'new-user-002', '/api/test', 2, '2025-01-20', '2025-01-20 15:00:00'),
('usage-new-003', 'new-user-003', '/api/demo', 1, '2025-01-20', '2025-01-20 17:00:00'),

-- 🔧 개발/QA 테스트용 (집중적인 테스트 패턴)
('usage-qa-001', 'qa-test-001', '/api/qa-testing', 234, '2025-01-20', '2025-01-20 09:00:00'),
('usage-qa-002', 'qa-test-001', '/api/qa-testing', 189, '2025-01-20', '2025-01-20 14:00:00'),
('usage-load-001', 'load-test-001', '/api/load-test', 2567, '2025-01-20', '2025-01-20 10:00:00'),
('usage-load-002', 'load-test-001', '/api/performance', 1834, '2025-01-20', '2025-01-20 15:00:00'),

-- 🚀 고활용 사용자들 (매우 높은 사용량)
('usage-power-001', 'power-user-001', '/api/high-volume', 4567, '2025-01-20', '2025-01-20 08:00:00'),
('usage-power-002', 'power-user-001', '/api/intensive', 3421, '2025-01-20', '2025-01-20 13:00:00'),
('usage-heavy-001', 'heavy-user-001', '/api/cpu-intensive', 3789, '2025-01-20', '2025-01-20 09:00:00'),
('usage-heavy-002', 'heavy-user-001', '/api/data-processing', 2987, '2025-01-20', '2025-01-20 16:00:00');

-- ================================
-- 🎯 테스트 시나리오별 사용자 가이드
-- ================================

/*
🧪 **기본 API 테스트용 사용자들:**
- test-user-001: 기본 API 테스트 계정 (test@example.com)
- demo-user-001: 데모 및 시연용 계정 (demo@test.com)  
- api-tester-001: API 기능 테스트 계정 (apitester@example.com)

🆓 **FREE 플랜 테스트 사용자들:**
- free-user-001~005: 다양한 OAuth 제공자 (Google, Auth0, GitHub)
- 각각 제한된 API 사용량 (일일 100회 이하)
- 최대 5개 커스텀 API, 3개 공유 API 제한 테스트 가능

💎 **PRO 플랜 테스트 사용자들:**
- pro-user-001~004: TossPay 구독 ID 포함
- 높은 사용량 (일일 1000-5000회 API 호출)
- 최대 50개 커스텀 API, 20개 공유 API 활용

🔄 **플랜 변경 테스트 사용자들:**
- switcher-001: FREE → PRO (사용량 89회 → 1234회로 급증)
- switcher-002: FREE → PRO (성장 메트릭 분석 비즈니스)

🆕 **신규 가입 테스트 사용자들:**
- new-user-001~003: 2025년 1월 가입
- 자동 FREE 구독 생성 테스트 가능
- 초기 사용량 매우 적음 (1-5회)

🔧 **개발/QA 전용 테스트 사용자들:**
- dev-test-001: 개발 환경 테스트 (FREE)
- qa-test-001: QA 테스트 (PRO with TossPay)
- load-test-001: 부하 테스트 (PRO with 높은 사용량)

🚀 **고사용량 테스트 사용자들:**
- power-user-001: 초고사용량 (일일 4000+회)
- heavy-user-001: CPU 집약적 처리 (일일 3000+회)

📊 **실제 사용 패턴 데이터:**
- 오늘(2025-01-20) 기준 최신 사용량 데이터
- FREE: 1-90회/일, PRO: 500-5000회/일
- 플랜 업그레이드 전후 사용량 비교 가능
- 다양한 API 엔드포인트별 사용 분포

🔑 **API Key 보유 사용자들:**
- 모든 PRO 사용자 + 테스트 사용자들
- AWS Secrets Manager ARN 형태로 저장
- 실제 키 등록/삭제 테스트 가능

🎯 **주요 테스트 시나리오:**

1. **신규 가입 플로우**: new-user-001~003 사용
2. **플랜 업그레이드**: switcher-001~002 참고
3. **사용량 제한 테스트**: free-user vs pro-user 비교
4. **API 생성/공유 제한**: FREE(5개) vs PRO(50개)
5. **TossPay 결제 연동**: pro-user들의 실제 구독 ID 활용
6. **부하 테스트**: load-test-001, power-user-001 활용
7. **로깅/메트릭 테스트**: 모든 사용자의 실제 활동 데이터
8. **OAuth 테스트**: Google, Auth0, GitHub 다양한 제공자

📈 **메트릭 및 로깅 테스트 데이터:**
- 실시간 사용량 증가 패턴
- 플랜별 사용자 분포 (FREE: 10명, PRO: 8명)
- API 공유/저장 패턴 분석
- 구독 변경 이벤트 추적
*/

-- Foreign Key 제약 조건 재활성화
SET FOREIGN_KEY_CHECKS = 1;