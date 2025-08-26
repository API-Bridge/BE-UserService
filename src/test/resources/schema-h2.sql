-- H2 데이터베이스용 테스트 스키마
-- H2 호환 문법으로 작성된 테스트용 스키마입니다.

-- 사용자의 핵심 신원 정보
CREATE TABLE users (
    user_id VARCHAR(36) NOT NULL,
    auth0_id VARCHAR(255) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id)
);

-- 유니크 인덱스 생성
CREATE UNIQUE INDEX uk_auth0_id ON users(auth0_id);
CREATE UNIQUE INDEX uk_user_email ON users(user_email);

-- 사용자의 BYOK(Bring Your Own Key) 정보
CREATE TABLE user_secrets_arn (
    arn_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    arn VARCHAR(255) NOT NULL,
    arn_description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (arn_id)
);

-- 인덱스 생성
CREATE INDEX idx_user_id ON user_secrets_arn(user_id);
CREATE INDEX idx_arn ON user_secrets_arn(arn);

-- 구독 플랜 정보
CREATE TABLE subscription_plan (
    plan_id VARCHAR(36) NOT NULL,
    plan_name VARCHAR(100) NOT NULL,
    max_api_calls_per_month INTEGER NOT NULL,
    max_custom_apis INTEGER NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT NULL,
    PRIMARY KEY (plan_id)
);

-- 사용자 구독 정보
CREATE TABLE user_subscription (
    subscription_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    plan_id VARCHAR(36) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    auto_renewal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT NULL,
    PRIMARY KEY (subscription_id)
);

-- API 사용량 기록
CREATE TABLE api_usage_record (
    usage_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    api_endpoint VARCHAR(500) NOT NULL,
    usage_date DATE NOT NULL,
    call_count INTEGER NOT NULL DEFAULT 1,
    response_time INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (usage_id)
);

-- 커스텀 API 정보
CREATE TABLE custom_apis (
    api_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    api_name VARCHAR(255) NOT NULL,
    description TEXT,
    data_count INTEGER NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT NULL,
    PRIMARY KEY (api_id)
);

-- 공유 API 정보
CREATE TABLE shared_apis (
    shared_api_id VARCHAR(36) NOT NULL,
    original_api_id VARCHAR(36) NOT NULL,
    creator_id VARCHAR(36) NOT NULL,
    api_name VARCHAR(255) NOT NULL,
    description TEXT,
    data_count INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT NULL,
    PRIMARY KEY (shared_api_id)
);

-- 사용자 저장 API 정보
CREATE TABLE user_saved_apis (
    user_api_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    shared_api_id VARCHAR(36) NOT NULL,
    api_name VARCHAR(255) NOT NULL,
    description TEXT,
    data_count INTEGER NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT NULL,
    PRIMARY KEY (user_api_id)
);

-- 인덱스 생성
CREATE INDEX idx_custom_apis_user_id ON custom_apis(user_id);
CREATE INDEX idx_custom_apis_deleted ON custom_apis(is_deleted);
CREATE INDEX idx_shared_apis_creator_id ON shared_apis(creator_id);
CREATE INDEX idx_shared_apis_original_api_id ON shared_apis(original_api_id);
CREATE INDEX idx_shared_apis_active ON shared_apis(is_active);
CREATE INDEX idx_user_saved_apis_user_id ON user_saved_apis(user_id);
CREATE INDEX idx_user_saved_apis_shared_api_id ON user_saved_apis(shared_api_id);
CREATE INDEX idx_user_saved_apis_deleted ON user_saved_apis(is_deleted);