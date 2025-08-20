-- ================================
-- 완전한 DB 재생성 스크립트
-- ================================

-- 기존 데이터베이스 삭제 후 새로 생성
DROP DATABASE IF EXISTS userservice;
CREATE DATABASE userservice;
USE userservice;

-- 1. Plan 테이블 (구독 플랜)
CREATE TABLE plan (
    plan_id INT AUTO_INCREMENT PRIMARY KEY,
    plan_type VARCHAR(20) NOT NULL UNIQUE,
    price DECIMAL(10, 2) NOT NULL,
    description TEXT,
    features JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_plan_type (plan_type)
);

-- 2. User 테이블 (사용자)
CREATE TABLE `user` (
    user_id VARCHAR(36) PRIMARY KEY,
    auth0_id VARCHAR(255) UNIQUE,
    user_email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_email (user_email),
    INDEX idx_auth0_id (auth0_id)
);

-- 3. Subscription 테이블 (사용자 구독) - stripe_subscription_id 포함
CREATE TABLE subscription (
    subscription_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    plan_id INT NOT NULL,
    plan_payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    plan_update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    stripe_subscription_id VARCHAR(255) NULL COMMENT 'Stripe에서 생성한 구독 ID (sub_xxx 형태)',
    FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
    FOREIGN KEY (plan_id) REFERENCES plan(plan_id) ON DELETE RESTRICT,
    INDEX idx_stripe_subscription_id (stripe_subscription_id),
    INDEX idx_user_id (user_id),
    INDEX idx_plan_id (plan_id),
    INDEX idx_is_active (is_active)
);

-- 4. User Secrets ARN 테이블
CREATE TABLE user_secrets_arn (
    arn_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    arn VARCHAR(500) NOT NULL,
    arn_description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
);

-- 5. Custom API 테이블
CREATE TABLE custom_api (
    custom_api_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_name (name)
);

-- 6. Shared API 테이블 
CREATE TABLE shared_api (
    shared_api_id VARCHAR(36) PRIMARY KEY,
    original_api_id VARCHAR(36) NOT NULL,
    creator_id VARCHAR(36) NOT NULL,
    api_name VARCHAR(255) NOT NULL,
    description TEXT,
    data_count INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (original_api_id) REFERENCES custom_api(custom_api_id) ON DELETE CASCADE,
    FOREIGN KEY (creator_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
    INDEX idx_creator_id (creator_id),
    INDEX idx_original_api_id (original_api_id),
    INDEX idx_api_name (api_name),
    INDEX idx_is_active (is_active)
);

-- 7. User Saved API 테이블
CREATE TABLE user_saved_api (
    user_api_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    shared_api_id VARCHAR(36) NOT NULL,
    api_name VARCHAR(255) NOT NULL,
    description TEXT,
    data_count INT DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
    FOREIGN KEY (shared_api_id) REFERENCES shared_api(shared_api_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_shared_api_id (shared_api_id),
    INDEX idx_is_deleted (is_deleted)
);

-- 8. API Usage Record 테이블
CREATE TABLE api_usage_record (
    record_id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    api_endpoint VARCHAR(255) NOT NULL,
    request_count INT DEFAULT 1,
    record_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_record_date (record_date),
    INDEX idx_api_endpoint (api_endpoint),
    UNIQUE KEY uk_user_api_date (user_id, api_endpoint, record_date)
);

-- ================================
-- 초기 데이터 삽입
-- ================================

-- 1. Plan 데이터 (planType enum에 맞게 생성)
INSERT INTO plan (plan_type, price, description, features) VALUES 
('FREE', 0.00, '무료 플랜 - 시작하기에 완벽', '["월 100회 API 호출", "분당 10회 제한", "시간당 100회 제한", "일일 1,000회 제한", "최대 5개 커스텀 API", "최대 3개 공유 API", "기본 지원", "커뮤니티 액세스"]'),
('PRO', 22.00, '프로 플랜 - 비즈니스용', '["월 10,000회 API 호출", "분당 60회 제한", "시간당 3,600회 제한", "일일 86,400회 제한", "최대 50개 커스텀 API", "최대 20개 공유 API", "우선 지원", "고급 분석", "API 키 관리", "Stripe 결제"]');

-- 2. User 데이터 (다양한 시나리오의 사용자들)
INSERT INTO `user` (user_id, auth0_id, user_email, created_at) VALUES 
-- 개발/테스트용 기본 사용자
('user-001', 'auth0|user001', 'user001@example.com', '2024-01-15 10:00:00'),
('user-123', 'auth0|user123', 'test@example.com', '2024-01-20 15:00:00'),

-- FREE 플랜 사용자들
('user-free-01', 'google-oauth2|117885903921309558140', 'freemium@gmail.com', '2024-12-01 09:00:00'),
('user-free-02', 'auth0|freebee2024', 'startup@email.com', '2024-12-02 10:30:00'),
('user-free-03', 'github|developer123', 'developer@github.io', '2024-12-03 14:15:00'),

-- PRO 플랜 사용자들
('user-pro-01', 'google-oauth2|business001', 'business@company.com', '2024-11-15 11:00:00'),
('user-pro-02', 'auth0|enterprise456', 'enterprise@bigcorp.com', '2024-11-20 16:30:00'),

-- 최근 가입한 사용자들 (자동 FREE 구독 테스트용)
('user-new-01', 'google-oauth2|newuser2024', 'newuser@test.com', '2024-12-15 10:00:00'),
('user-new-02', 'auth0|recent789', 'recent@signup.com', '2024-12-16 14:20:00'),

-- 구독 변경 이력이 있는 사용자
('user-switcher', 'auth0|planswitcher', 'switcher@plans.com', '2024-10-01 12:00:00');

-- 3. Subscription 데이터 (현실적인 시나리오 반영)
INSERT INTO subscription (subscription_id, user_id, plan_id, plan_payment_date, plan_update_date, is_active, stripe_subscription_id) VALUES 
-- 기본 테스트 사용자들 (FREE)
('sub-001', 'user-001', 1, '2024-01-15 10:00:00', '2024-01-15 10:00:00', true, NULL),
('sub-123', 'user-123', 1, '2024-01-20 15:00:00', '2024-01-20 15:00:00', true, NULL),

-- 활성 FREE 구독 사용자들
('sub-free-01', 'user-free-01', 1, '2024-12-01 09:00:00', '2024-12-01 09:00:00', true, NULL),
('sub-free-02', 'user-free-02', 1, '2024-12-02 10:30:00', '2024-12-02 10:30:00', true, NULL),
('sub-free-03', 'user-free-03', 1, '2024-12-03 14:15:00', '2024-12-03 14:15:00', true, NULL),

-- 활성 PRO 구독 사용자들 (Stripe 연동)
('sub-pro-01', 'user-pro-01', 2, '2024-11-15 11:00:00', '2024-11-15 11:00:00', true, 'sub_1QRxyzABCDEF123456'),
('sub-pro-02', 'user-pro-02', 2, '2024-11-20 16:30:00', '2024-11-20 16:30:00', true, 'sub_1QSabcDEFGHI789012'),

-- 새로운 사용자들 (자동 생성된 FREE 구독)
('sub-new-01', 'user-new-01', 1, '2024-12-15 10:00:00', '2024-12-15 10:00:00', true, NULL),
('sub-new-02', 'user-new-02', 1, '2024-12-16 14:20:00', '2024-12-16 14:20:00', true, NULL),

-- 플랜 변경 이력 (현재는 PRO, 이전에 FREE 구독이 있었음)
('sub-switcher-old', 'user-switcher', 1, '2024-10-01 12:00:00', '2024-11-01 10:00:00', false, NULL),
('sub-switcher-current', 'user-switcher', 2, '2024-11-01 10:00:00', '2024-11-01 10:00:00', true, 'sub_1QTdefGHIJKL345678');

-- 4. User Secrets ARN 데이터 (PRO 사용자들과 활발한 사용자들)
INSERT INTO user_secrets_arn (arn_id, user_id, arn, arn_description, created_at) VALUES 
('arn-001', 'user-001', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:user001-api-key-AbCdEf', 'User 001 API Key', '2024-01-15 10:00:00'),
('arn-pro-01', 'user-pro-01', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:business-api-key-XyZ123', 'Business User API Key', '2024-11-15 11:30:00'),
('arn-pro-02', 'user-pro-02', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:enterprise-api-key-Abc789', 'Enterprise User API Key', '2024-11-20 17:00:00'),
('arn-switcher', 'user-switcher', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:switcher-api-key-Def456', 'Plan Switcher API Key', '2024-11-01 10:30:00');

-- 5. Custom API 데이터 (다양한 사용자들의 API)
INSERT INTO custom_api (custom_api_id, user_id, name, description, created_at, updated_at) VALUES 
-- 기본 테스트 사용자 APIs
('api-001', 'user-001', 'Weather API', 'Get current weather information', '2024-01-15 10:30:00', '2024-01-15 10:30:00'),
('api-002', 'user-001', 'News API', 'Fetch latest news articles', '2024-01-15 11:00:00', '2024-01-15 11:00:00'),

-- FREE 사용자들의 APIs (제한된 개수)
('api-free-01', 'user-free-01', 'Simple Weather', 'Basic weather data', '2024-12-01 10:00:00', '2024-12-01 10:00:00'),
('api-free-02', 'user-free-02', 'Startup News', 'Tech startup news feed', '2024-12-02 11:00:00', '2024-12-02 11:00:00'),
('api-free-03', 'user-free-03', 'Code Snippets', 'Programming code examples', '2024-12-03 15:00:00', '2024-12-03 15:00:00'),

-- PRO 사용자들의 APIs (많은 개수와 복잡한 기능)
('api-pro-01', 'user-pro-01', 'Advanced Analytics', 'Business intelligence dashboard', '2024-11-15 12:00:00', '2024-11-15 12:00:00'),
('api-pro-02', 'user-pro-01', 'Customer Data API', 'CRM integration service', '2024-11-16 09:30:00', '2024-11-16 09:30:00'),
('api-pro-03', 'user-pro-01', 'Payment Gateway', 'Secure payment processing', '2024-11-17 14:45:00', '2024-11-17 14:45:00'),
('api-pro-04', 'user-pro-02', 'Enterprise Auth', 'SSO authentication system', '2024-11-20 17:30:00', '2024-11-20 17:30:00'),
('api-pro-05', 'user-pro-02', 'Data Pipeline', 'Real-time data processing', '2024-11-21 10:15:00', '2024-11-21 10:15:00'),

-- 플랜 변경 사용자의 APIs
('api-switcher-01', 'user-switcher', 'Stock Analysis', 'Advanced stock market analysis', '2024-10-15 11:00:00', '2024-11-02 09:00:00'),
('api-switcher-02', 'user-switcher', 'Crypto Trading Bot', 'Automated cryptocurrency trading', '2024-11-02 10:00:00', '2024-11-02 10:00:00');

-- 6. Shared API 데이터 (PRO 사용자들이 더 많이 공유)
INSERT INTO shared_api (shared_api_id, original_api_id, creator_id, api_name, description, data_count, is_active, created_at, updated_at) VALUES 
-- FREE 사용자들의 공유 (제한적)
('shared-001', 'api-001', 'user-001', 'Public Weather API', 'Simple weather data for everyone', 150, true, '2024-01-20 09:00:00', '2024-01-20 09:00:00'),
('shared-free-01', 'api-free-01', 'user-free-01', 'Basic Weather', 'Free weather service', 45, true, '2024-12-05 10:00:00', '2024-12-05 10:00:00'),
('shared-free-02', 'api-free-03', 'user-free-03', 'Code Examples', 'Programming snippets library', 78, true, '2024-12-10 16:00:00', '2024-12-10 16:00:00'),

-- PRO 사용자들의 공유 (풍부한 데이터)
('shared-pro-01', 'api-pro-01', 'user-pro-01', 'Business Analytics', 'Professional analytics suite', 500, true, '2024-11-25 11:00:00', '2024-11-25 11:00:00'),
('shared-pro-02', 'api-pro-04', 'user-pro-02', 'Enterprise SSO', 'Single sign-on solution', 320, true, '2024-11-28 14:30:00', '2024-11-28 14:30:00'),
('shared-switcher', 'api-switcher-01', 'user-switcher', 'Stock Insights', 'Premium stock analysis', 280, true, '2024-11-05 12:00:00', '2024-11-05 12:00:00');

-- 7. User Saved API 데이터 (다양한 사용자들의 저장 패턴)
INSERT INTO user_saved_api (user_api_id, user_id, shared_api_id, api_name, description, data_count, is_deleted, created_at, updated_at) VALUES 
-- FREE 사용자들이 저장한 APIs
('saved-free-01', 'user-free-01', 'shared-001', 'Weather API', 'Saved weather service', 25, false, '2024-12-07 09:00:00', '2024-12-07 09:00:00'),
('saved-free-02', 'user-free-02', 'shared-free-02', 'Code Examples', 'Saved programming snippets', 15, false, '2024-12-12 11:00:00', '2024-12-12 11:00:00'),
('saved-free-03', 'user-free-03', 'shared-pro-01', 'Business Analytics', 'Saved analytics for learning', 8, false, '2024-12-14 13:30:00', '2024-12-14 13:30:00'),

-- PRO 사용자들이 저장한 APIs (더 많이 활용)
('saved-pro-01', 'user-pro-01', 'shared-switcher', 'Stock Insights', 'Saved for business analysis', 45, false, '2024-11-26 10:00:00', '2024-11-26 10:00:00'),
('saved-pro-02', 'user-pro-02', 'shared-pro-01', 'Business Analytics', 'Cross-company analytics', 60, false, '2024-11-29 15:00:00', '2024-11-29 15:00:00'),

-- 기본 사용자의 저장
('saved-001', 'user-001', 'shared-pro-01', 'Business Analytics', 'Basic user analytics access', 12, false, '2024-12-01 14:00:00', '2024-12-01 14:00:00'),
('saved-123', 'user-123', 'shared-free-01', 'Basic Weather', 'Test user weather save', 5, false, '2024-12-15 16:00:00', '2024-12-15 16:00:00');

-- 8. API Usage Record 데이터 (현실적인 사용 패턴)
INSERT INTO api_usage_record (record_id, user_id, api_endpoint, request_count, record_date, created_at) VALUES 
-- FREE 사용자들의 사용량 (제한된 범위 내)
('usage-free-01', 'user-free-01', '/api/weather', 15, '2024-12-20', '2024-12-20 09:00:00'),
('usage-free-02', 'user-free-01', '/api/weather', 22, '2024-12-21', '2024-12-21 10:00:00'),
('usage-free-03', 'user-free-02', '/api/news', 8, '2024-12-20', '2024-12-20 11:00:00'),
('usage-free-04', 'user-free-03', '/api/code-snippets', 35, '2024-12-20', '2024-12-20 15:00:00'),

-- PRO 사용자들의 사용량 (높은 사용량)
('usage-pro-01', 'user-pro-01', '/api/analytics', 450, '2024-12-20', '2024-12-20 08:00:00'),
('usage-pro-02', 'user-pro-01', '/api/customer-data', 320, '2024-12-20', '2024-12-20 09:30:00'),
('usage-pro-03', 'user-pro-01', '/api/payments', 180, '2024-12-20', '2024-12-20 14:15:00'),
('usage-pro-04', 'user-pro-02', '/api/enterprise-auth', 280, '2024-12-20', '2024-12-20 10:00:00'),
('usage-pro-05', 'user-pro-02', '/api/data-pipeline', 520, '2024-12-20', '2024-12-20 16:30:00'),

-- 플랜 변경 사용자의 사용량 (PRO 전환 후 증가)
('usage-switcher-01', 'user-switcher', '/api/stock-analysis', 75, '2024-10-30', '2024-10-30 11:00:00'), -- FREE 시절
('usage-switcher-02', 'user-switcher', '/api/stock-analysis', 350, '2024-11-15', '2024-11-15 12:00:00'), -- PRO 전환 후
('usage-switcher-03', 'user-switcher', '/api/crypto-bot', 180, '2024-12-20', '2024-12-20 14:00:00'),

-- 기본 테스트 사용자들
('usage-001', 'user-001', '/api/weather', 45, '2024-12-20', '2024-12-20 09:00:00'),
('usage-002', 'user-001', '/api/news', 32, '2024-12-20', '2024-12-20 10:00:00'),
('usage-123', 'user-123', '/api/test', 12, '2024-12-20', '2024-12-20 11:00:00'),

-- 최근 가입 사용자들 (아직 사용량 적음)
('usage-new-01', 'user-new-01', '/api/weather', 3, '2024-12-20', '2024-12-20 12:00:00'),
('usage-new-02', 'user-new-02', '/api/test', 1, '2024-12-20', '2024-12-20 15:00:00');

-- ================================
-- 더미 데이터 요약
-- ================================

/*
🎯 테스트 시나리오별 사용자 목록:

📱 기본 테스트 사용자:
- user-001: 기본 테스트 계정 (FREE)
- user-123: 테스트 계정 (FREE)

🆓 FREE 플랜 사용자들:
- user-free-01: 프리미엄 Gmail 사용자 (Google OAuth)
- user-free-02: 스타트업 사용자 (Auth0)  
- user-free-03: 개발자 사용자 (GitHub OAuth)

💎 PRO 플랜 사용자들:
- user-pro-01: 비즈니스 사용자 (Stripe: sub_1QRxyzABCDEF123456)
- user-pro-02: 엔터프라이즈 사용자 (Stripe: sub_1QSabcDEFGHI789012)

🔄 플랜 변경 이력 사용자:
- user-switcher: FREE → PRO 전환 (Stripe: sub_1QTdefGHIJKL345678)

🆕 최근 가입 사용자들 (자동 FREE 구독):
- user-new-01: 새 사용자 1
- user-new-02: 새 사용자 2

📊 사용량 패턴:
- FREE 사용자: 일일 10-50회 API 호출
- PRO 사용자: 일일 200-1000회 API 호출  
- 플랜 변경 후: 사용량 대폭 증가 (75 → 350회)

🔑 API Key 보유 사용자:
- user-001, user-pro-01, user-pro-02, user-switcher

🚀 공유 API 현황:
- FREE 사용자: 기본적인 API 공유 (날씨, 코드 예제)
- PRO 사용자: 고급 비즈니스 API 공유 (분석, SSO)

💾 저장된 API 사용 패턴:
- FREE 사용자: 다른 사용자의 API를 학습 목적으로 저장
- PRO 사용자: 비즈니스 목적으로 활발한 API 교차 활용

🎯 테스트 가능한 시나리오:
1. 신규 사용자 가입 → 자동 FREE 구독 생성
2. FREE → PRO 업그레이드 → 사용량 증가
3. PRO → 취소 → FREE 다운그레이드  
4. API 생성/공유/저장 제한 테스트
5. 사용량 제한 및 모니터링 테스트
6. Stripe 결제 연동 테스트
7. 다양한 OAuth 제공자 테스트
*/
