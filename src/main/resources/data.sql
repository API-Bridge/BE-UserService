-- =====================================================
-- 사용자 서비스 (User Service) 데이터베이스 스키마
-- =====================================================

CREATE DATABASE IF NOT EXISTS user_service_db
DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE user_service_db;

-- 시스템에서 제공하는 모든 구독 플랜 정보
CREATE TABLE plan (
                      plan_id INT NOT NULL AUTO_INCREMENT,
                      plan_name VARCHAR(255) NOT NULL,
                      price DECIMAL(10, 2) NOT NULL,
                      description TEXT NULL,
                      features JSON NULL,
                      PRIMARY KEY (plan_id),
                      UNIQUE KEY uk_plan_name (plan_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 사용자의 핵심 신원 정보
CREATE TABLE user (
                      user_id VARCHAR(36) NOT NULL,
                      auth0_id VARCHAR(255) NOT NULL,
                      user_email VARCHAR(255) NOT NULL,
                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      PRIMARY KEY (user_id),
                      UNIQUE KEY uk_auth0_id (auth0_id),
                      UNIQUE KEY uk_user_email (user_email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 사용자의 구독 플랜 정보
CREATE TABLE subscription (
                              subscription_id VARCHAR(36) NOT NULL,
                              user_id VARCHAR(36) NOT NULL,
                              plan_id INT NOT NULL,
                              plan_payment_date DATETIME NOT NULL,
                              plan_update_date DATETIME NOT NULL,
                              is_active BOOLEAN NOT NULL DEFAULT FALSE,
                              PRIMARY KEY (subscription_id),
                              INDEX idx_user_id (user_id),
                              INDEX idx_plan_id (plan_id),
    -- MSA 환경에서는 외래키 제약조건 제거
    -- 데이터 일관성은 애플리케이션 레벨과 이벤트를 통해 관리
    -- CONSTRAINT fk_subscriptions_to_users FOREIGN KEY (user_id) REFERENCES user (user_id),
    -- CONSTRAINT fk_subscriptions_to_plans FOREIGN KEY (plan_id) REFERENCES plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 사용자의 BYOK(Bring Your Own Key) 정보
CREATE TABLE user_secrets_arn (
                                  arn_id VARCHAR(36) NOT NULL,
                                  user_id VARCHAR(36) NOT NULL,
                                  arn VARCHAR(255) NOT NULL,
                                  arn_description VARCHAR(255) NULL,
                                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  PRIMARY KEY (arn_id),
                                  INDEX idx_user_id (user_id)
    -- MSA 환경에서는 외래키 제약조건 제거
    -- CONSTRAINT fk_user_secrets_to_users FOREIGN KEY (user_id) REFERENCES user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;