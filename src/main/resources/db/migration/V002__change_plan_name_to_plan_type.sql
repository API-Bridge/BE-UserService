-- Plan 테이블의 plan_name 컬럼을 plan_type으로 변경
-- PlanType enum과 일치시키기 위한 마이그레이션

-- 1. 새로운 plan_type 컬럼 추가 (ENUM 타입)
ALTER TABLE plan ADD COLUMN plan_type VARCHAR(20) NOT NULL DEFAULT 'FREE';

-- 2. 기존 데이터를 새로운 컬럼으로 복사
UPDATE plan SET plan_type = 'FREE' WHERE plan_name = 'Free';
UPDATE plan SET plan_type = 'PRO' WHERE plan_name = 'Pro';

-- 3. 기존 plan_name 컬럼 삭제
ALTER TABLE plan DROP COLUMN plan_name;

-- 4. plan_type 컬럼에 인덱스 추가
CREATE INDEX idx_plan_type ON plan(plan_type);

-- 5. plan_type 컬럼에 체크 제약 조건 추가 (MySQL에서는 8.0.16+ 지원)
-- ALTER TABLE plan ADD CONSTRAINT chk_plan_type CHECK (plan_type IN ('FREE', 'PRO'));