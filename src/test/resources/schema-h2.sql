-- H2 데이터베이스용 테스트 스키마
-- H2 호환 문법으로 작성된 테스트용 스키마입니다.

-- 사용자의 핵심 신원 정보
CREATE TABLE user (
    user_id VARCHAR(36) NOT NULL,
    auth0_id VARCHAR(255) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id)
);

-- 유니크 인덱스 생성
CREATE UNIQUE INDEX uk_auth0_id ON user(auth0_id);
CREATE UNIQUE INDEX uk_user_email ON user(user_email);

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