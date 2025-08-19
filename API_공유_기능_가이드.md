# API 공유 기능 가이드

## 개요
User Service의 API 공유 기능은 사용자가 자신이 만든 커스텀 API를 다른 사용자들과 공유할 수 있게 해주는 기능입니다. 공유된 API는 공개 마켓플레이스에서 다른 사용자들이 검색하고 저장하여 사용할 수 있습니다.

## 시스템 아키텍처

### 핵심 컴포넌트
- **CustomApiController**: 사용자의 커스텀 API 관리
- **SharedApiController**: API 공유 및 공유된 API 관리
- **CustomApiService**: 커스텀 API 비즈니스 로직
- **SharedApiService**: 공유 API 비즈니스 로직
- **UserSavedApiService**: 사용자가 저장한 공유 API 관리

## 데이터베이스 스키마

### 1. custom_api 테이블
사용자가 생성한 커스텀 API의 메타 정보를 저장합니다.

```sql
CREATE TABLE custom_api (
    custom_api_id VARCHAR(36) NOT NULL COMMENT 'PK. 커스텀 API 고유 식별자',
    user_id VARCHAR(36) NOT NULL COMMENT 'FK. Users 테이블을 참조하는 외래키',
    name VARCHAR(255) NOT NULL COMMENT '커스텀 API의 이름',
    description TEXT NULL COMMENT '커스텀 API에 대한 설명',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    PRIMARY KEY (custom_api_id),
    CONSTRAINT fk_custom_api_to_user FOREIGN KEY (user_id) REFERENCES user (user_id)
);
```

### 2. shared_api 테이블
공유된 API 정보를 저장합니다.

```sql
CREATE TABLE shared_api (
    shared_api_id VARCHAR(36) NOT NULL,
    original_api_id VARCHAR(36) NOT NULL,    -- custom_api_id 참조
    creator_id VARCHAR(36) NOT NULL,         -- 공유한 사용자 ID
    api_name VARCHAR(255) NOT NULL,          -- 공유 시 설정한 이름
    description TEXT NULL,                   -- 공유 시 설정한 설명
    data_count INT NOT NULL DEFAULT 0,      -- 데이터 개수
    is_active BOOLEAN NOT NULL DEFAULT TRUE, -- 활성 상태
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (shared_api_id),
    INDEX idx_creator_id (creator_id),
    INDEX idx_api_name (api_name),
    INDEX idx_is_active (is_active),
    CONSTRAINT fk_shared_api_to_creator FOREIGN KEY (creator_id) REFERENCES user (user_id)
);
```

### 3. user_saved_api 테이블
사용자가 저장한 공유 API 정보를 저장합니다.

```sql
CREATE TABLE user_saved_api (
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
    CONSTRAINT fk_user_saved_api_to_user FOREIGN KEY (user_id) REFERENCES user (user_id),
    CONSTRAINT fk_user_saved_api_to_shared_api FOREIGN KEY (shared_api_id) REFERENCES shared_api (shared_api_id)
);
```

## API 공유 흐름

### 1. 커스텀 API 생성
```
User → CustomApi 생성 → custom_api 테이블에 저장
```

### 2. API 공유 프로세스
```
1. 사용자가 자신의 커스텀 API 목록 조회
   GET /api/custom-apis/my

2. 공유할 API 선택 및 공유 정보 입력
   - API ID
   - 공유용 이름 (선택사항)
   - 공유용 설명 (선택사항)
   - 플랜 타입

3. API 공유 요청
   POST /api/shared-apis/share
   파라미터:
   - customApiId: 원본 API ID
   - planType: 플랜 타입
   - apiName: 공유용 이름 (옵션)
   - apiDescription: 공유용 설명 (옵션)

4. 공유 처리 로직
   - 원본 API 존재 여부 확인
   - 소유권 검증 (본인의 API인지 확인)
   - 플랜별 공유 권한 검증
   - 중복 공유 방지 (이미 공유된 API인지 확인)
   - SharedApi 객체 생성 및 저장
```

### 3. 공유된 API 검색 및 저장
```
1. 공유 API 목록 조회
   GET /api/shared-apis

2. 공유 API 검색
   GET /api/shared-apis/search?keyword=날씨

3. 관심있는 API 저장
   POST /api/shared-apis/save?sharedApiId={id}

4. 내가 저장한 API 목록 조회
   GET /api/shared-apis/saved
```

## 주요 API 엔드포인트

### 커스텀 API 관리
| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/custom-apis/my` | 내 커스텀 API 목록 조회 |
| GET | `/api/custom-apis` | 커스텀 API 목록 조회 (페이징) |
| GET | `/api/custom-apis/{id}` | 커스텀 API 상세 조회 |
| GET | `/api/custom-apis/search` | 커스텀 API 검색 |

### 공유 API 관리
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/shared-apis/share` | API 공유하기 |
| DELETE | `/api/shared-apis/unshare` | API 공유 취소 |
| GET | `/api/shared-apis` | 공유 API 목록 조회 |
| GET | `/api/shared-apis/search` | 공유 API 검색 |
| GET | `/api/shared-apis/my` | 내가 공유한 API 목록 |

### 저장된 API 관리
| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/shared-apis/save` | 공유 API 저장 |
| GET | `/api/shared-apis/saved` | 저장된 API 목록 조회 |
| DELETE | `/api/shared-apis/saved/{id}` | 저장된 API 삭제 |

## 테스트 방법

### 1. 테스트 페이지 접속
브라우저에서 다음 URL에 접속:
```
http://localhost:8081/shared-api-test.html
```

### 2. 테스트 시나리오

#### 시나리오 1: API 공유하기
1. **내 API 목록 확인**
   - "내 API 새로고침" 버튼 클릭
   - 보유한 커스텀 API 목록이 카드 형태로 표시됨

2. **API 공유 설정**
   - API 카드에서 "공유하기" 버튼 클릭
   - 공유 폼에 API 정보가 자동으로 채워짐
   - 필요시 "공유할 API 이름"과 "API 설명" 수정
   - 플랜 타입 선택 (FREE/PRO/ENTERPRISE)

3. **공유 실행**
   - "API 공유하기" 버튼 클릭
   - 성공 응답 확인:
   ```json
   {
     "success": true,
     "data": {
       "sharedApiId": "...",
       "apiName": "사용자가 입력한 이름",
       "description": "사용자가 입력한 설명"
     }
   }
   ```

#### 시나리오 2: 공유된 API 검색 및 저장
1. **공유 API 목록 탭**으로 이동
2. **전체 목록** 또는 **키워드 검색**으로 API 찾기
3. 관심있는 API의 **"저장하기"** 버튼 클릭
4. **"저장된 API" 탭**에서 저장된 API 확인

#### 시나리오 3: 내 공유 API 관리
1. **"내 공유 API" 탭**으로 이동
2. 내가 공유한 API 목록 확인
3. 필요시 공유 취소 (별도 기능)

### 3. 데이터베이스 확인
테스트 후 다음 쿼리로 데이터 확인:

```sql
-- 공유된 API 확인
SELECT * FROM shared_api WHERE is_active = true;

-- 저장된 API 확인
SELECT * FROM user_saved_api WHERE is_deleted = false;

-- 커스텀 API 확인
SELECT * FROM custom_api;
```

## 주요 기능 특징

### 1. 커스터마이징 가능한 공유
- 원본 API 이름/설명과 다른 공유용 이름/설명 설정 가능
- 입력하지 않으면 원본 정보 사용

### 2. 권한 검증
- 본인의 API만 공유 가능
- 플랜별 공유 권한 검증
- 중복 공유 방지

### 3. 사용자 친화적 UI
- 드래그 앤 드롭 없이 간단한 버튼 클릭
- 실시간 API 목록 로딩
- 폼 자동 완성 및 검증

### 4. 검색 및 필터링
- 키워드 기반 검색
- 페이지네이션 지원
- 활성/비활성 상태 관리

## 오류 처리

### 일반적인 오류 상황
- **"API를 찾을 수 없습니다"**: 잘못된 customApiId
- **"본인의 API만 공유할 수 있습니다"**: 권한 없는 API 공유 시도
- **"이미 공유된 API입니다"**: 중복 공유 시도
- **"현재 플랜에서는 이 API를 공유할 수 없습니다"**: 플랜 제한

### 해결 방법
1. 유효한 API ID인지 확인
2. 로그인한 사용자의 API인지 확인
3. 이미 공유되었는지 확인
4. 사용자의 구독 플랜 확인

## 확장 가능성

### 향후 개발 가능한 기능
1. **API 리뷰 시스템**: 사용자들이 공유 API에 리뷰 작성
2. **카테고리 분류**: API를 카테고리별로 분류
3. **인기도 순 정렬**: 사용량 기반 인기 API 표시
4. **API 버전 관리**: 같은 API의 여러 버전 지원
5. **수익 분배**: 유료 API 공유 시 수익 분배

## 보안 고려사항

### 현재 구현된 보안 기능
- JWT 기반 사용자 인증
- 소유권 검증
- SQL 인젝션 방지 (JPA 사용)
- XSS 방지 (입력값 검증)

### 추가 보안 강화 방안
- API 공유 시 악성 코드 검증
- 과도한 공유 방지 (Rate Limiting)
- 민감 정보 노출 방지 검증
- 공유 API 사용량 모니터링