-- =====================================================
-- User Service 통합 스키마 & 테스트 데이터
-- 제공된 SQL 스키마에 맞게 수정
-- =====================================================

-- MySQL 데이터베이스 사용

-- =====================================================
-- 스키마 생성 (테이블 정의) - 제공된 스키마 기준
-- =====================================================

-- 1. 구독 플랜 테이블 (plan)
CREATE TABLE IF NOT EXISTS plan (
    plan_id INT NOT NULL AUTO_INCREMENT,
    plan_name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    description TEXT NULL,
    features JSON NULL,
    PRIMARY KEY (plan_id),
    UNIQUE KEY uk_plan_name (plan_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 사용자 기본 정보 테이블 (user) - 백틱으로 감싸기
CREATE TABLE IF NOT EXISTS `user` (
    user_id VARCHAR(36) NOT NULL,
    auth0_id VARCHAR(255) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_auth0_id (auth0_id),
    UNIQUE KEY uk_user_email (user_email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 사용자 구독 정보 테이블 (subscription)
CREATE TABLE IF NOT EXISTS subscription (
    subscription_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    plan_id INT NOT NULL,
    plan_payment_date DATETIME NOT NULL,
    plan_update_date DATETIME NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (subscription_id),
    INDEX idx_user_id (user_id),
    INDEX idx_plan_id (plan_id),
    CONSTRAINT fk_subscriptions_to_users FOREIGN KEY (user_id) REFERENCES `user` (user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_subscriptions_to_plans FOREIGN KEY (plan_id) REFERENCES plan (plan_id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 사용자 시크릿 ARN 관리 테이블
CREATE TABLE IF NOT EXISTS user_secrets_arn (
    arn_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    arn VARCHAR(255) NOT NULL,
    arn_description VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (arn_id),
    INDEX idx_user_id (user_id),
    CONSTRAINT fk_user_secrets_to_users FOREIGN KEY (user_id) REFERENCES `user` (user_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 커스텀 API 테이블
CREATE TABLE IF NOT EXISTS custom_api (
    custom_api_id VARCHAR(36) NOT NULL COMMENT 'PK. 커스텀 API 고유 식별자',
    user_id VARCHAR(36) NOT NULL COMMENT 'FK. Users 테이블을 참조하는 외래키',
    name VARCHAR(255) NOT NULL COMMENT '커스텀 API의 이름',
    description TEXT NULL COMMENT '커스텀 API에 대한 설명',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    PRIMARY KEY (custom_api_id),
    CONSTRAINT fk_custom_api_to_user FOREIGN KEY (user_id) REFERENCES `user` (user_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자가 생성한 커스텀 API의 메타 정보를 관리합니다.';

-- 인덱스는 테이블 생성 시 이미 포함되어 있음

-- =================================================================
-- 2. 공유 API 서비스 관련 테이블
-- =================================================================

-- 6. 공유 API 정보 테이블
CREATE TABLE IF NOT EXISTS shared_api (
    shared_api_id VARCHAR(36) NOT NULL,
    original_api_id VARCHAR(36) NOT NULL,
    creator_id VARCHAR(36) NOT NULL,
    api_name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    data_count INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (shared_api_id),
    INDEX idx_creator_id (creator_id),
    INDEX idx_api_name (api_name),
    INDEX idx_is_active (is_active),
    INDEX idx_created_at (created_at),
    CONSTRAINT fk_shared_api_to_creator FOREIGN KEY (creator_id) REFERENCES `user` (user_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자가 공유한 API 정보';

-- 7. 사용자 저장 API 테이블
CREATE TABLE IF NOT EXISTS user_saved_api (
    user_api_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    shared_api_id VARCHAR(36) NOT NULL,
    api_name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    data_count INT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_api_id),
    INDEX idx_user_id (user_id),
    INDEX idx_shared_api_id (shared_api_id),
    INDEX idx_is_deleted (is_deleted),
    INDEX idx_created_at (created_at),
    CONSTRAINT fk_user_saved_api_to_user FOREIGN KEY (user_id) REFERENCES `user` (user_id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_user_saved_api_to_shared_api FOREIGN KEY (shared_api_id) REFERENCES shared_api (shared_api_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자가 저장한 공유 API 정보';

-- 8. API 사용량 기록 테이블
CREATE TABLE IF NOT EXISTS api_usage_record (
    record_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    api_endpoint VARCHAR(255) NOT NULL,
    request_count INT NOT NULL DEFAULT 0,
    record_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (record_id),
    INDEX idx_user_id (user_id),
    INDEX idx_record_date (record_date),
    INDEX idx_api_endpoint (api_endpoint),
    INDEX idx_user_date (user_id, record_date),
    CONSTRAINT fk_api_usage_to_user FOREIGN KEY (user_id) REFERENCES `user` (user_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API 사용량 기록 정보';

-- =====================================================
-- 테스트 데이터 삽입 - 새 스키마에 맞게 수정
-- =====================================================

-- 구독 플랜 데이터 (plan 테이블)
INSERT IGNORE INTO plan (plan_name, price, description, features) VALUES
('Free', 0.00, '무료 기본 플랜', '{"maxApiCount": 5, "rateLimitPerMinute": 10, "rateLimitPerHour": 100, "rateLimitPerDay": 1000}'),
('Pro', 299.00, '프로 플랜', '{"maxApiCount": 100, "rateLimitPerMinute": 200, "rateLimitPerHour": 5000, "rateLimitPerDay": 50000}');

-- 테스트 사용자 데이터 (user 테이블)
INSERT IGNORE INTO `user` (user_id, auth0_id, user_email, created_at) VALUES
('user-001', 'auth0|6894dba797884cb2154bc2729', 'testuser1@example.com', CURRENT_TIMESTAMP),
('user-002', 'auth0|test_user_2', 'testuser2@example.com', CURRENT_TIMESTAMP),
('user-003', 'auth0|test_admin', 'admin@example.com', CURRENT_TIMESTAMP),
('user-004', 'auth0|test_developer', 'developer@example.com', CURRENT_TIMESTAMP),
('user-005', 'auth0|test_premium', 'premium@example.com', CURRENT_TIMESTAMP);

-- 사용자 구독 데이터 (subscription 테이블)
INSERT IGNORE INTO subscription (subscription_id, user_id, plan_id, plan_payment_date, plan_update_date, is_active) VALUES
('sub-user-001', 'user-001', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
('sub-user-004', 'user-004', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
('sub-user-002', 'user-002', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
('sub-user-005', 'user-005', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true),
('sub-user-003', 'user-003', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, true);

-- 사용자 시크릿 ARN 데이터
INSERT IGNORE INTO user_secrets_arn (arn_id, user_id, arn, arn_description, created_at) VALUES
('arn-001', 'user-002', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:user-api-key-1-AbCdEf', 'OpenAI API Key', CURRENT_TIMESTAMP),
('arn-002', 'user-003', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:user-api-key-2-GhIjKl', 'Claude API Key', CURRENT_TIMESTAMP),
('arn-003', 'user-005', 'arn:aws:secretsmanager:us-east-1:123456789012:secret:user-api-key-3-MnOpQr', 'Gemini API Key', CURRENT_TIMESTAMP);

-- 커스텀 API 데이터 (custom_api 테이블)
INSERT IGNORE INTO custom_api (custom_api_id, user_id, name, description, created_at, updated_at) VALUES
('api-001', 'user-003', 'Weather API', '전 세계 날씨 정보를 제공하는 API', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api-002', 'user-003', 'News API', '실시간 뉴스 정보 API', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api-003', 'user-005', 'Stock Price API', '주식 가격 정보 API', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api-004', 'user-002', 'Crypto API', '암호화폐 가격 정보 API', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('api-005', 'user-004', 'Sports API', '스포츠 경기 결과 API', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- =====================================================
-- 공유 API 관련 테스트 데이터
-- =====================================================

-- 공유 API 데이터 (shared_api 테이블)
INSERT IGNORE INTO shared_api (shared_api_id, original_api_id, creator_id, api_name, description, data_count, is_active, created_at, updated_at) VALUES
('shared-001', 'api-001', 'user-003', 'Weather API', '전 세계 날씨 정보를 제공하는 API', 10000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('shared-002', 'api-002', 'user-003', 'News API', '실시간 뉴스 정보 API', 5000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('shared-003', 'api-003', 'user-005', 'Stock Price API', '주식 가격 정보 API', 15000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('shared-004', 'api-004', 'user-002', 'Crypto API', '암호화폐 가격 정보 API', 8000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('shared-005', 'api-005', 'user-004', 'Sports API', '스포츠 경기 결과 API', 3000, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 사용자 저장 API 데이터 (user_saved_api 테이블)
INSERT IGNORE INTO user_saved_api (user_api_id, user_id, shared_api_id, api_name, description, data_count, is_deleted, created_at, updated_at) VALUES
('saved-001', 'user-001', 'shared-001', 'Weather API', '날씨 정보 API', 100, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('saved-002', 'user-002', 'shared-001', 'Weather API', '날씨 정보 API', 250, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('saved-003', 'user-002', 'shared-002', 'News API', '뉴스 정보 API', 150, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('saved-004', 'user-004', 'shared-003', 'Stock Price API', '주식 가격 API', 300, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('saved-005', 'user-001', 'shared-004', 'Crypto API', '암호화폐 가격 API', 75, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('saved-006', 'user-005', 'shared-001', 'Weather API', '날씨 정보 API', 500, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- API 사용량 기록 데이터 (api_usage_record 테이블)
INSERT IGNORE INTO api_usage_record (record_id, user_id, api_endpoint, request_count, record_date, created_at) VALUES
('usage-001', 'user-002', '/api/chat/completions', 25, CURRENT_DATE, CURRENT_TIMESTAMP),
('usage-002', 'user-002', '/api/embeddings', 10, CURRENT_DATE, CURRENT_TIMESTAMP),
('usage-003', 'user-003', '/api/chat/completions', 150, CURRENT_DATE, CURRENT_TIMESTAMP),
('usage-004', 'user-005', '/api/generate', 75, CURRENT_DATE, CURRENT_TIMESTAMP),
('usage-005', 'user-001', '/api/weather', 45, CURRENT_DATE, CURRENT_TIMESTAMP),
('usage-006', 'user-004', '/api/stocks', 88, CURRENT_DATE, CURRENT_TIMESTAMP),
('usage-007', 'user-002', '/api/news', 32, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), CURRENT_TIMESTAMP),
('usage-008', 'user-003', '/api/crypto', 120, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), CURRENT_TIMESTAMP);