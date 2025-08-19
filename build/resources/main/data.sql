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
    plan_name VARCHAR(50) NOT NULL UNIQUE,
    price DECIMAL(10, 2) NOT NULL,
    description TEXT,
    features JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_plan_name (plan_name)
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

-- 1. Plan 데이터
INSERT INTO plan (plan_name, price, description, features) VALUES 
('Free', 0.00, '무료 플랜', '["월 100회 API 호출", "기본 지원", "커뮤니티 액세스"]'),
('Pro', 22.00, '프로 플랜', '["월 10,000회 API 호출", "우선 지원", "고급 분석", "API 키 관리"]');

-- 2. User 데이터
INSERT INTO `user` (user_id, auth0_id, user_email, created_at) VALUES 
('user-001', 'auth0|user001', 'user001@example.com', '2024-01-15 10:00:00'),
('user-002', 'auth0|user002', 'user002@example.com', '2024-01-16 11:00:00'),
('user-003', 'auth0|user003', 'user003@example.com', '2024-01-17 12:00:00'),
('user-004', 'auth0|user004', 'user004@example.com', '2024-01-18 13:00:00'),
('user-005', 'auth0|user005', 'user005@example.com', '2024-01-19 14:00:00'),
('user-123', 'auth0|user123', 'test@example.com', '2024-01-20 15:00:00');

-- 3. Subscription 데이터 (stripe_subscription_id는 일단 NULL)
INSERT INTO subscription (subscription_id, user_id, plan_id, plan_payment_date, plan_update_date, is_active) VALUES 
('sub-001', 'user-001', 1, '2024-01-15 10:00:00', '2024-01-15 10:00:00', true),
('sub-002', 'user-002', 2, '2024-01-16 11:00:00', '2024-01-16 11:00:00', true),
('sub-003', 'user-003', 1, '2024-01-17 12:00:00', '2024-01-17 12:00:00', true),
('sub-004', 'user-004', 2, '2024-01-18 13:00:00', '2024-01-18 13:00:00', false),
('sub-005', 'user-005', 1, '2024-01-19 14:00:00', '2024-01-19 14:00:00', true),
('sub-123', 'user-123', 1, '2024-01-20 15:00:00', '2024-01-20 15:00:00', true);

-- 4. User Secrets ARN 데이터
INSERT INTO user_secrets_arn (arn_id, user_id, arn, arn_description, created_at) VALUES 
('arn-001', 'user-001', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:user001-api-key-AbCdEf', 'User 001 API Key', '2024-01-15 10:00:00'),
('arn-002', 'user-002', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:user002-api-key-GhIjKl', 'User 002 API Key', '2024-01-16 11:00:00'),
('arn-003', 'user-003', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:user003-api-key-MnOpQr', 'User 003 API Key', '2024-01-17 12:00:00');

-- 5. Custom API 데이터
INSERT INTO custom_api (custom_api_id, user_id, name, description, created_at, updated_at) VALUES 
('api-001', 'user-001', 'Weather API', 'Get current weather information', '2024-01-15 10:30:00', '2024-01-15 10:30:00'),
('api-002', 'user-001', 'News API', 'Fetch latest news articles', '2024-01-15 11:00:00', '2024-01-15 11:00:00'),
('api-003', 'user-002', 'Stock API', 'Real-time stock prices', '2024-01-16 11:30:00', '2024-01-16 11:30:00'),
('api-004', 'user-002', 'Crypto API', 'Cryptocurrency data', '2024-01-16 12:00:00', '2024-01-16 12:00:00'),
('api-005', 'user-003', 'Translation API', 'Text translation service', '2024-01-17 12:30:00', '2024-01-17 12:30:00');

-- 6. Shared API 데이터
INSERT INTO shared_api (shared_api_id, original_api_id, creator_id, api_name, description, data_count, is_active, created_at, updated_at) VALUES 
('shared-001', 'api-001', 'user-001', 'Public Weather API', 'Shared weather data', 150, true, '2024-01-20 09:00:00', '2024-01-20 09:00:00'),
('shared-002', 'api-003', 'user-002', 'Stock Market Data', 'Shared stock information', 75, true, '2024-01-21 10:00:00', '2024-01-21 10:00:00'),
('shared-003', 'api-005', 'user-003', 'Language Translator', 'Shared translation service', 200, true, '2024-01-22 11:00:00', '2024-01-22 11:00:00');

-- 7. User Saved API 데이터
INSERT INTO user_saved_api (user_api_id, user_id, shared_api_id, api_name, description, data_count, is_deleted, created_at, updated_at) VALUES 
('saved-001', 'user-002', 'shared-001', 'Weather API', 'Saved weather data', 50, false, '2024-01-25 09:00:00', '2024-01-25 09:00:00'),
('saved-002', 'user-003', 'shared-001', 'Weather API', 'Saved weather data', 30, false, '2024-01-25 10:00:00', '2024-01-25 10:00:00'),
('saved-003', 'user-001', 'shared-002', 'Stock Market Data', 'Saved stock information', 25, false, '2024-01-25 11:00:00', '2024-01-25 11:00:00'),
('saved-004', 'user-004', 'shared-003', 'Language Translator', 'Saved translation service', 40, false, '2024-01-25 12:00:00', '2024-01-25 12:00:00');

-- 8. API Usage Record 데이터
INSERT INTO api_usage_record (record_id, user_id, api_endpoint, request_count, record_date, created_at) VALUES 
('usage-001', 'user-001', '/api/weather', 45, '2024-01-25', '2024-01-25 09:00:00'),
('usage-002', 'user-001', '/api/news', 32, '2024-01-25', '2024-01-25 10:00:00'),
('usage-003', 'user-002', '/api/stock', 58, '2024-01-25', '2024-01-25 11:00:00'),
('usage-004', 'user-002', '/api/crypto', 23, '2024-01-25', '2024-01-25 12:00:00'),
('usage-005', 'user-003', '/api/translate', 67, '2024-01-25', '2024-01-25 13:00:00'),
('usage-006', 'user-001', '/api/weather', 38, '2024-01-26', '2024-01-26 09:00:00'),
('usage-007', 'user-002', '/api/stock', 42, '2024-01-26', '2024-01-26 10:00:00');
