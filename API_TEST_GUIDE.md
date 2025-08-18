# 🚀 User Service API 엔드포인트 테스트 가이드

## 📋 테스트 환경 설정

### 1. 애플리케이션 실행
```bash
# Windows
gradlew.bat bootRun

# Linux/Mac
./gradlew bootRun
```

### 2. 기본 설정
- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`
- **인증**: `X-User-Id` 헤더 사용 (개발 환경)

---

## 🔧 1. 사용자 관리 API 테스트

### 1.1 사용자 생성
```bash
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "auth0Id": "auth0|test_user_123",
    "userEmail": "testuser@example.com"
  }'
```

**기대 응답 (201 Created):**
```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "userId": "generated-uuid",
    "auth0Id": "auth0|test_user_123",
    "userEmail": "testuser@example.com",
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

### 1.2 사용자 조회
```bash
curl -X GET "http://localhost:8080/api/users/{userId}"
```

---

## 🔑 2. 개인키 관리 API 테스트 (BYOK)

### 2.1 개인키 등록
```bash
curl -X POST "http://localhost:8080/api/users/{userId}/secrets" \
  -H "Content-Type: application/json" \
  -d '{
    "secretName": "my-openai-key",
    "secretValue": "sk-1234567890abcdef",
    "description": "OpenAI API Key for personal use"
  }'
```

**기대 응답 (201 Created):**
```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "arnId": "generated-uuid",
    "userId": "user-uuid",
    "arn": "arn:aws:secretsmanager:us-east-1:123456789012:secret:my-openai-key-AbCdEf",
    "arnDescription": "OpenAI API Key for personal use",
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

### 2.2 개인키 조회 (AI 서비스용)
```bash
curl -X GET "http://localhost:8080/api/secrets/{userId}/arn?arnId={arnId}"
```

### 2.3 사용자 키 목록 조회
```bash
curl -X GET "http://localhost:8080/api/users/{userId}/secrets"
```

---

## 💳 3. 구독 관리 API 테스트

### 3.1 사용자 구독 정보 조회
```bash
curl -X GET "http://localhost:8080/api/users/{userId}/subscription"
```

**기대 응답 (200 OK):**
```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "subscriptionId": "sub-uuid",
    "user": {
      "userId": "user-uuid",
      "userEmail": "user@example.com"
    },
    "plan": {
      "planId": 1,
      "planName": "Free",
      "price": 0.00,
      "description": "무료 기본 플랜"
    },
    "planPaymentDate": "2024-01-01T10:00:00",
    "planUpdateDate": "2024-01-01T10:00:00",
    "isActive": true
  }
}
```

### 3.2 구독 플랜 변경
```bash
curl -X PUT "http://localhost:8080/api/users/{userId}/subscription?planName=Pro"
```

### 3.3 API 사용량 조회
```bash
curl -X GET "http://localhost:8080/api/users/{userId}/usage"
```

**기대 응답 (200 OK):**
```json
{
  "success": true,
  "data": {
    "plan": {
      "name": "Free",
      "maxApiCount": 5,
      "rateLimitPerMinute": 10,
      "rateLimitPerHour": 100,
      "rateLimitPerDay": 1000
    },
    "currentUsage": {
      "minute": {"used": 2, "limit": 10},
      "hour": {"used": 15, "limit": 100},
      "day": {"used": 150, "limit": 1000}
    },
    "usageRatio": {
      "minute": 20.0,
      "hour": 15.0,
      "day": 15.0
    }
  }
}
```

### 3.4 Stripe 결제 세션 생성
```bash
curl -X POST "http://localhost:8080/api/subscription/checkout" \
  -H "Content-Type: application/json" \
  -d '{
    "priceId": "price_1234567890",
    "successUrl": "http://localhost:3000/success",
    "cancelUrl": "http://localhost:3000/cancel"
  }'
```

### 3.5 Stripe 프로덕트 정보 조회
```bash
curl -X GET "http://localhost:8080/api/subscription/products"
```

---

## 🔄 4. 공유 API 기능 테스트

### 4.1 API 공유 게시
```bash
curl -X POST "http://localhost:8080/api/shared-apis/share" \
  -H "X-User-Id: user-003" \
  -H "Content-Type: application/json" \
  -d 'customApiId=api-001&planType=FREE'
```

**기대 응답 (200 OK):**
```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "sharedApiId": "shared-uuid",
    "originalApiId": "api-001",
    "creatorId": "user-003",
    "apiName": "Weather API",
    "description": "전 세계 날씨 정보를 제공하는 API",
    "dataCount": 0,
    "isActive": true,
    "createdAt": "2024-01-01T10:00:00"
  }
}
```

### 4.2 API 공유 취소
```bash
curl -X DELETE "http://localhost:8080/api/shared-apis/unshare" \
  -H "X-User-Id: user-003" \
  -d 'customApiId=api-001'
```

### 4.3 공유 API 목록 조회 (페이징)
```bash
curl -X GET "http://localhost:8080/api/shared-apis?page=0&size=10"
```

### 4.4 특정 사용자의 공유 API 조회
```bash
curl -X GET "http://localhost:8080/api/shared-apis/creator/{creatorId}"
```

### 4.5 공유 API 검색
```bash
curl -X GET "http://localhost:8080/api/shared-apis/search?keyword=weather"
```

### 4.6 공유 API 저장
```bash
curl -X POST "http://localhost:8080/api/shared-apis/{sharedApiId}/save" \
  -H "X-User-Id: user-001"
```

### 4.7 사용자 저장 API 목록 조회
```bash
curl -X GET "http://localhost:8080/api/shared-apis/saved" \
  -H "X-User-Id: user-001"
```

### 4.8 저장된 API 삭제
```bash
curl -X DELETE "http://localhost:8080/api/shared-apis/saved/{userApiId}" \
  -H "X-User-Id: user-001"
```

---

## 🏥 5. 헬스체크 API

### 5.1 애플리케이션 상태 확인
```bash
curl -X GET "http://localhost:8080/health"
```

**기대 응답 (200 OK):**
```json
{
  "success": true,
  "message": "User Service is running",
  "data": {
    "status": "UP",
    "timestamp": "2024-01-01T10:00:00",
    "version": "1.0.0"
  }
}
```

---

## 🧪 6. 테스트 시나리오 예시

### 시나리오 1: 새 사용자 온보딩
1. **사용자 생성** → 2. **구독 정보 확인** → 3. **사용량 조회**

### 시나리오 2: API 공유 플로우
1. **사용자 생성** → 2. **API 공유** → 3. **공유 목록 확인** → 4. **다른 사용자가 저장**

### 시나리오 3: 구독 업그레이드
1. **현재 구독 확인** → 2. **Stripe 결제 세션 생성** → 3. **플랜 변경** → 4. **새 사용량 한도 확인**

### 시나리오 4: BYOK 플로우
1. **개인키 등록** → 2. **키 목록 확인** → 3. **AI 서비스에서 키 조회**

---

## ⚠️ 에러 처리 테스트

### 잘못된 요청 테스트
```bash
# 필수 필드 누락
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "auth0Id": "auth0|test"
    // userEmail 누락
  }'
```

**기대 응답 (400 Bad Request):**
```json
{
  "success": false,
  "message": "이메일은 필수입니다.",
  "errorCode": "INVALID_REQUEST"
}
```

### 존재하지 않는 리소스
```bash
curl -X GET "http://localhost:8080/api/users/non-existent-id"
```

**기대 응답 (404 Not Found):**
```json
{
  "success": false,
  "message": "사용자를 찾을 수 없습니다.",
  "errorCode": "USER_NOT_FOUND"
}
```

---

## 🔧 Postman 컬렉션

테스트를 더 쉽게 하려면 Postman 컬렉션을 생성하는 것을 권장합니다:

1. **Environment 설정**:
   - `base_url`: `http://localhost:8080`
   - `test_user_id`: `user-001`

2. **Global Headers**:
   - `Content-Type`: `application/json`
   - `X-User-Id`: `{{test_user_id}}`

---

## 📊 성능 테스트

### Apache Bench (ab) 예시
```bash
# 동시 연결 10개로 100개 요청
ab -n 100 -c 10 -H "X-User-Id: user-001" \
   http://localhost:8080/api/users/user-001/subscription
```

### JMeter 스크립트
- 사용자 등록 부하 테스트
- API 공유 기능 부하 테스트  
- 구독 조회 성능 테스트

---

이 가이드를 통해 모든 API 엔드포인트를 체계적으로 테스트할 수 있습니다! 🎯