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

---

# 🚀 UserService 전체 기능 Postman 테스트 가이드 (2024 업데이트)

## 📋 목차
1. [기본 설정 (업데이트)](#기본-설정-업데이트)
2. [사용자 관리 API (확장)](#사용자-관리-api-확장)
3. [플랜 관리 API (신규)](#플랜-관리-api-신규)
4. [구독 관리 API (확장)](#구독-관리-api-확장)
5. [공유 API 관리 (확장)](#공유-api-관리-확장)
6. [API 사용량 추적 (신규)](#api-사용량-추적-신규)
7. [실제 Stripe 결제 시스템 테스트](#실제-stripe-결제-시스템-테스트)
8. [통합 테스트 시나리오](#통합-테스트-시나리오)

---

## 기본 설정 (업데이트)

### 환경 변수 설정 (Environment Variables)
```json
{
  "baseUrl": "http://localhost:8081",
  "testUserId": "user-001",
  "proUserId": "user-002",
  "adminUserId": "user-003"
}
```

### 공통 헤더
```json
{
  "Content-Type": "application/json",
  "X-User-Id": "{{testUserId}}"
}
```

---

## 사용자 관리 API (확장)

### 1. 사용자 생성
```http
POST {{baseUrl}}/api/users
Content-Type: application/json

{
  "auth0Id": "auth0|test123456789",
  "userEmail": "test@example.com",
  "userName": "Test User"
}
```

**예상 응답:**
```json
{
  "success": true,
  "data": {
    "userId": "user-001",
    "auth0Id": "auth0|test123456789",
    "userEmail": "test@example.com",
    "userName": "Test User",
    "createdAt": "2024-01-01T00:00:00"
  },
  "message": "사용자가 성공적으로 생성되었습니다.",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### 2. 사용자 조회
```http
GET {{baseUrl}}/api/users/{{testUserId}}
```

### 3. 사용자 개인키(BYOK) 등록
```http
POST {{baseUrl}}/api/users/{{testUserId}}/secrets
Content-Type: application/json

{
  "secretName": "my-openai-key",
  "secretValue": "sk-1234567890abcdef",
  "description": "OpenAI API 키"
}
```

### 4. 사용자 개인키 목록 조회
```http
GET {{baseUrl}}/api/users/{{testUserId}}/secrets
```

### 5. 플랜별 기능 조회 (신규)
```http
GET {{baseUrl}}/api/users/plan-features
X-User-Id: {{testUserId}}
```

**예상 응답:**
```json
{
  "success": true,
  "data": {
    "message": "플랜 기능 정보는 /api/plan/features 엔드포인트를 사용하세요.",
    "redirectTo": "/api/plan/features"
  }
}
```

---

## 플랜 관리 API (신규)

### 1. 플랜 제한 조회
```http
GET {{baseUrl}}/api/plan/limits
X-User-Id: {{testUserId}}
```

**예상 응답:**
```json
{
  "success": true,
  "data": {
    "planType": "FREE",
    "maxCustomApiCount": 5,
    "currentCustomApiCount": 2,
    "maxSharedApiCount": 3,
    "currentSharedApiCount": 1,
    "maxDataBundleCount": 3,
    "maxApiCallsPerMonth": 100,
    "rateLimitPerMinute": 10,
    "rateLimitPerHour": 100,
    "rateLimitPerDay": 1000,
    "isCustomApiLimitReached": false,
    "isSharedApiLimitReached": false,
    "customApiRemaining": 3,
    "sharedApiRemaining": 2
  }
}
```

### 2. 사용량 통계 조회
```http
GET {{baseUrl}}/api/plan/usage-stats
X-User-Id: {{testUserId}}
```

**예상 응답:**
```json
{
  "success": true,
  "data": {
    "currentMinute": 5,
    "currentHour": 45,
    "currentDay": 120,
    "currentMonth": 850,
    "userId": "user-001",
    "timestamp": "2024-01-01T12:30:00"
  }
}
```

### 3. 플랜 기능 상세 조회
```http
GET {{baseUrl}}/api/plan/features
X-User-Id: {{testUserId}}
```

**예상 응답:**
```json
{
  "success": true,
  "data": {
    "planType": "FREE",
    "canCreateCustomApi": true,
    "canShareApi": true,
    "customApiRemaining": 3,
    "sharedApiRemaining": 2,
    "rateLimits": {
      "perMinute": 10,
      "perHour": 100,
      "perDay": 1000
    },
    "maxApiCallsPerMonth": 100,
    "maxDataBundleCount": 3
  }
}
```

### 4. 구독 제한 상태 조회
```http
GET {{baseUrl}}/api/plan/limits-status
X-User-Id: {{testUserId}}
```

**예상 응답:**
```json
{
  "success": true,
  "data": {
    "planType": "FREE",
    "limits": {
      "customApi": {
        "current": 2,
        "max": 5,
        "remaining": 3,
        "limitReached": false,
        "needsUpgrade": false
      },
      "sharedApi": {
        "current": 1,
        "max": 3,
        "remaining": 2,
        "limitReached": false,
        "needsUpgrade": false
      },
      "usage": {
        "dailyApiCalls": 120,
        "monthlyApiCalls": 850,
        "maxMonthlyApiCalls": 100
      }
    },
    "recommendations": {
      "upgradeRecommended": false,
      "reason": null
    }
  }
}
```

---

## 구독 관리 API (확장)

### 1. 구독 생성 (실제 Stripe Checkout)
```http
POST {{baseUrl}}/api/subscription/checkout
Content-Type: application/json
X-User-Id: {{testUserId}}

{
  "priceId": "price_1234567890abcdef",
  "userId": "{{testUserId}}",
  "plan": "Pro"
}
```

**예상 응답 (실제 Stripe 테스트 환경):**
```json
{
  "success": true,
  "data": {
    "checkoutUrl": "https://checkout.stripe.com/c/pay/cs_test_...",
    "sessionUrl": "https://checkout.stripe.com/c/pay/cs_test_...",
    "sessionId": "cs_test_a1b2c3d4e5f6g7h8i9j0",
    "priceId": "price_1234567890abcdef"
  }
}
```

### 2. 현재 구독 조회
```http
GET {{baseUrl}}/api/subscription/current
X-User-Id: {{testUserId}}
```

### 3. 구독 제한 상태 조회
```http
GET {{baseUrl}}/api/subscription/limits-status
X-User-Id: {{testUserId}}
```

**예상 응답:**
```json
{
  "success": true,
  "data": {
    "hasActiveSubscription": true,
    "planType": "PRO",
    "planName": "Pro Plan",
    "subscriptionId": "sub_1234567890",
    "isActive": true,
    "planPaymentDate": "2024-01-01T00:00:00",
    "planUpdateDate": "2024-01-01T00:00:00",
    "features": {
      "maxCustomApiCount": 100,
      "maxSharedApiCount": 50,
      "maxDataBundleCount": 20,
      "rateLimitPerMinute": 100,
      "rateLimitPerHour": 5000,
      "rateLimitPerDay": 50000
    },
    "message": "자세한 사용량 정보는 /api/plan/limits-status 엔드포인트를 사용하세요.",
    "redirectTo": "/api/plan/limits-status"
  }
}
```

### 4. 구독 취소
```http
DELETE {{baseUrl}}/api/subscription/cancel
X-User-Id: {{testUserId}}
```

### 5. 사용량 조회
```http
GET {{baseUrl}}/api/subscription/usage
X-User-Id: {{testUserId}}
```

### 6. Rate Limit 확인
```http
GET {{baseUrl}}/api/subscription/rate-limit/check
X-User-Id: {{testUserId}}
```

---

## 공유 API 관리 (확장)

### 1. API 공유하기
```http
POST {{baseUrl}}/api/shared-apis/share
Content-Type: application/json
X-User-Id: {{testUserId}}

{
  "customApiId": "api-test-001",
  "planType": "PRO",
  "apiName": "Test Shared API",
  "apiDescription": "테스트용 공유 API"
}
```

**예상 응답:**
```json
{
  "success": true,
  "data": {
    "sharedApiId": "shared-api-123",
    "originalApiId": "api-test-001",
    "creatorId": "user-001",
    "apiName": "Test Shared API",
    "description": "테스트용 공유 API",
    "active": true,
    "createdAt": "2024-01-01T00:00:00"
  }
}
```

### 2. 공유 API 목록 조회
```http
GET {{baseUrl}}/api/shared-apis
```

### 3. 내가 공유한 API 목록
```http
GET {{baseUrl}}/api/shared-apis/my-shared
X-User-Id: {{testUserId}}
```

### 4. API 공유 취소 (Custom API ID로)
```http
DELETE {{baseUrl}}/api/shared-apis/unshare?customApiId=api-test-001
X-User-Id: {{testUserId}}
```

### 5. API 공유 취소 (Shared API ID로)
```http
DELETE {{baseUrl}}/api/shared-apis/unshare/shared-api-123
X-User-Id: {{testUserId}}
```

### 6. 공유 API 저장
```http
POST {{baseUrl}}/api/shared-apis/save
Content-Type: application/x-www-form-urlencoded
X-User-Id: {{testUserId}}

sharedApiId=shared-api-123
```

### 7. 저장된 API 목록
```http
GET {{baseUrl}}/api/shared-apis/saved
X-User-Id: {{testUserId}}
```

---

## API 사용량 추적 (신규)

### 📊 실시간 사용량 모니터링

모든 API 호출이 자동으로 추적됩니다. `ApiUsageTrackingInterceptor`가 다음 엔드포인트들을 제외한 모든 API 호출을 추적합니다:

**추적 제외 엔드포인트:**
- `/actuator/*`
- `/api/health`
- `/swagger-*`
- `/v3/api-docs`
- `/webjars/*`
- `/static/*`, `/css/*`, `/js/*`, `/images/*`
- `/favicon.ico`
- `/test/*`
- `/`

### 사용량 확인 방법

**1. 실시간 사용량 조회:**
```http
GET {{baseUrl}}/api/plan/usage-stats
X-User-Id: {{testUserId}}
```

**2. 플랜 제한 대비 사용량 확인:**
```http
GET {{baseUrl}}/api/plan/limits-status
X-User-Id: {{testUserId}}
```

### 사용량 테스트 시나리오

**1. API 호출 부하 테스트:**
```http
# 여러 번 반복 호출하여 사용량 증가 확인
GET {{baseUrl}}/api/users/{{testUserId}}
X-User-Id: {{testUserId}}
```

**2. Rate Limiting 테스트:**
```http
# 빠르게 연속 호출하여 Rate Limit 동작 확인
# FREE 플랜: 분당 10회 제한
GET {{baseUrl}}/api/shared-apis
X-User-Id: {{testUserId}}
```

---

## 실제 Stripe 결제 시스템 테스트

### 🔗 실제 Stripe 테스트 환경 설정

**환경변수 설정 (.env 파일):**
```env
STRIPE_SECRET_KEY=sk_test_실제테스트키
STRIPE_PUBLIC_KEY=pk_test_실제테스트키
STRIPE_WEBHOOK_SECRET=whsec_실제웹훅시크릿
```

### 실제 결제 플로우 테스트

**1. 통합 테스트 대시보드 접속:**
```
{{baseUrl}}/test/dashboard
```

**2. 실제 Stripe 결제 테스트:**
1. 대시보드에서 Pro 플랜 선택
2. "Stripe 결제 시작" 버튼 클릭
3. **실제 Stripe Checkout 페이지로 리다이렉션**
4. 테스트 카드 정보 입력:
   - 카드 번호: `4242 4242 4242 4242`
   - 만료일: 미래 날짜 (예: `12/25`)
   - CVC: 임의 3자리 (예: `123`)
   - 우편번호: 임의 5자리 (예: `12345`)
5. 결제 완료 후 대시보드로 리다이렉션
6. 실시간 DB 업데이트 확인

**3. 결제 성공 후 자동 검증:**
- URL 파라미터 `?success=true` 확인
- 30초간 폴링으로 DB 업데이트 확인
- 구독 상태 실시간 업데이트
- 플랜 제한 자동 업데이트

### Stripe Webhook 테스트

**실제 Webhook 이벤트 처리:**
```http
POST {{baseUrl}}/webhook
Content-Type: application/json
Stripe-Signature: [실제 Stripe 서명]

{
  "type": "checkout.session.completed",
  "data": {
    "object": {
      "id": "cs_test_실제세션ID",
      "customer": "cus_실제고객ID",
      "subscription": "sub_실제구독ID"
    }
  }
}
```

### 테스트 카드 정보

**성공 케이스:**
- `4242424242424242` - Visa (기본)
- `4000056655665556` - Visa (Debit)
- `5555555555554444` - Mastercard

**실패 케이스:**
- `4000000000000002` - 카드 거절
- `4000000000009995` - 자금 부족
- `4000000000000069` - 만료된 카드

---

## 통합 테스트 시나리오

### 🎯 시나리오 1: 신규 사용자 전체 플로우

1. **사용자 생성**
```http
POST {{baseUrl}}/api/users
{
  "auth0Id": "auth0|newuser123",
  "userEmail": "newuser@example.com",
  "userName": "New User"
}
```

2. **초기 플랜 상태 확인**
```http
GET {{baseUrl}}/api/plan/limits
X-User-Id: [새로생성된userId]
```

3. **개인키 등록**
```http
POST {{baseUrl}}/api/users/[userId]/secrets
{
  "secretName": "openai-key",
  "secretValue": "sk-test123",
  "description": "Test OpenAI Key"
}
```

4. **API 공유 테스트**
```http
POST {{baseUrl}}/api/shared-apis/share
{
  "customApiId": "api-new-001",
  "planType": "FREE",
  "apiName": "My First API",
  "apiDescription": "첫 번째 공유 API"
}
```

5. **사용량 확인**
```http
GET {{baseUrl}}/api/plan/usage-stats
X-User-Id: [userId]
```

6. **실제 Stripe 프로 플랜 업그레이드**
```http
POST {{baseUrl}}/api/subscription/checkout
{
  "priceId": "price_실제PRO플랜ID",
  "userId": "[userId]"
}
```

7. **업그레이드 후 제한 확인**
```http
GET {{baseUrl}}/api/plan/limits
X-User-Id: [userId]
```

### 🎯 시나리오 2: Rate Limiting 테스트

1. **현재 Rate Limit 확인**
```http
GET {{baseUrl}}/api/plan/features
X-User-Id: {{testUserId}}
```

2. **연속 API 호출 (Rate Limit 도달)**
```bash
# Postman Runner나 Newman으로 반복 실행
for i in {1..15}; do
  curl -H "X-User-Id: {{testUserId}}" {{baseUrl}}/api/shared-apis
done
```

3. **Rate Limit 초과 확인**
```http
GET {{baseUrl}}/api/subscription/rate-limit/check
X-User-Id: {{testUserId}}
```

### 🎯 시나리오 3: 실제 결제 및 웹훅 테스트

1. **실제 Stripe Checkout 세션 생성**
2. **테스트 카드로 결제 완료**
3. **Webhook 이벤트 자동 처리 확인**
4. **DB 업데이트 실시간 확인**
5. **플랜 업그레이드 반영 확인**

---

## 🔧 Postman Collection 설정 (업데이트)

### Collection Variables
```json
{
  "baseUrl": "http://localhost:8081",
  "testUserId": "user-001",
  "proUserId": "user-002",
  "adminUserId": "user-003",
  "testApiId": "api-test-001",
  "sharedApiId": "",
  "sessionId": "",
  "stripePublicKey": "pk_test_실제키",
  "stripePriceId": "price_실제프로플랜ID"
}
```

### Pre-request Scripts (Collection Level)
```javascript
// 자동으로 X-User-Id 헤더 설정
pm.request.headers.add({
    key: "X-User-Id",
    value: pm.collectionVariables.get("testUserId")
});

// 실시간 타임스탬프 추가
pm.globals.set("timestamp", new Date().toISOString());
```

### Tests (응답 검증)
```javascript
// 공통 응답 검증
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has success field", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property("success");
    pm.expect(jsonData.success).to.be.true;
});

// API 사용량 자동 확인
pm.test("Usage tracking works", function () {
    setTimeout(() => {
        pm.sendRequest({
            url: pm.collectionVariables.get("baseUrl") + "/api/plan/usage-stats",
            method: "GET",
            header: {
                "X-User-Id": pm.collectionVariables.get("testUserId")
            }
        }, function (err, res) {
            console.log("Current usage:", res.json());
        });
    }, 1000);
});

// sharedApiId 저장 (API 공유 후)
if (pm.response.json().data && pm.response.json().data.sharedApiId) {
    pm.collectionVariables.set("sharedApiId", pm.response.json().data.sharedApiId);
}

// Stripe sessionId 저장 (결제 세션 생성 후)
if (pm.response.json().data && pm.response.json().data.sessionId) {
    pm.collectionVariables.set("sessionId", pm.response.json().data.sessionId);
    console.log("Stripe Checkout URL:", pm.response.json().data.checkoutUrl);
}
```

---

## 📈 모니터링 및 로그

### 애플리케이션 로그 확인
```bash
# 실시간 로그 확인
tail -f logs/userservice.log

# API 사용량 추적 로그 필터링
grep "API 사용량 추적됨" logs/userservice.log

# Rate Limiting 로그 확인
grep "Rate limit" logs/userservice.log

# Stripe 관련 로그 확인
grep "Stripe" logs/userservice.log
```

### Actuator 엔드포인트 활용
```http
GET {{baseUrl}}/actuator/health
GET {{baseUrl}}/actuator/metrics
GET {{baseUrl}}/actuator/loggers
```

### 실시간 대시보드 모니터링
- **URL**: `{{baseUrl}}/test/dashboard`
- **실시간 사용량 표시**
- **플랜 제한 현황**
- **결제 상태 모니터링**
- **DB 업데이트 실시간 확인**

---

## 🚨 트러블슈팅

### 일반적인 문제들

**1. Stripe 키 설정 문제:**
```bash
# 환경변수 확인
echo $STRIPE_SECRET_KEY
# 또는 application.yml 확인
```

**2. Rate Limiting 테스트 실패:**
```http
# 카운터 리셋 (개발용)
POST {{baseUrl}}/test/api/reset-counters/{{testUserId}}
```

**3. DB 연결 문제:**
```http
GET {{baseUrl}}/actuator/health
```

**4. 웹훅 처리 실패:**
```bash
# 웹훅 로그 확인
grep "웹훅" logs/userservice.log
```

이 업데이트된 가이드를 통해 모든 신규 기능과 실제 Stripe 연동을 포함한 전체 시스템을 체계적으로 테스트할 수 있습니다! 🚀

---

## 🆕 신규 기능 테스트 가이드 (2024년 8월 업데이트)

### ✨ 새로 추가된 주요 기능들

1. **통합 사용자 정보 조회 API** - 커스텀 API 서비스용 전용 엔드포인트
2. **구독 취소 시 FREE 플랜 자동 전환** - 사용자가 구독 없는 상태가 되지 않도록 보장
3. **강화된 플랜 제한 검증** - API 생성/공유 시 실시간 제한 검사
4. **개선된 예외 처리** - 사용자 친화적인 오류 메시지 및 업그레이드 권장

---

## 🎯 신규 API 엔드포인트 테스트

### 1. 통합 사용자 정보 조회 (커스텀 API 서비스용)

**⭐ 새로운 핵심 엔드포인트!**

```http
GET {{baseUrl}}/api/users/info
X-User-Id: {{testUserId}}
```

**예상 응답:**
```json
{
  "success": true,
  "data": {
    "userId": "user-001",
    "userEmail": "test@example.com",
    "createdAt": "2024-01-15T10:00:00",
    "planInfo": {
      "planType": "FREE",
      "planName": "Free Plan",
      "isActive": true,
      "planPaymentDate": "2024-01-15T10:00:00",
      "planUpdateDate": "2024-01-15T10:00:00",
      "stripeSubscriptionId": null,
      "price": 0.0,
      "description": "무료 플랜 - 시작하기에 완벽"
    },
    "usageLimits": {
      "maxCustomApiCount": 5,
      "maxSharedApiCount": 3,
      "maxDataBundleCount": 10,
      "rateLimitPerMinute": 10,
      "rateLimitPerHour": 100,
      "rateLimitPerDay": 1000
    },
    "currentUsage": {
      "customApiCount": 2,
      "sharedApiCount": 1,
      "savedApiCount": 3,
      "minuteUsage": 5,
      "hourUsage": 45,
      "dayUsage": 320
    }
  }
}
```

**🔍 테스트 시나리오:**
1. **FREE 플랜 사용자 정보 조회**
2. **PRO 플랜 사용자 정보 조회** (업그레이드 후)
3. **구독이 없는 사용자 정보 조회** (기본값 확인)

---

### 2. 개선된 구독 취소 테스트

**구독 취소 후 FREE 플랜 자동 전환 확인:**

```http
POST {{baseUrl}}/api/subscription/users/{{proUserId}}/subscription/cancel
```

**예상 응답 (개선됨):**
```json
{
  "success": true,
  "data": {
    "status": "CANCELLED",
    "message": "구독이 성공적으로 취소되어 FREE 플랜으로 전환되었습니다",
    "cancelledAt": "2024-08-20T14:30:00",
    "previousPlanType": "PRO",
    "newPlanType": "FREE",
    "newSubscriptionId": "sub-free-uuid-123"
  }
}
```

**🔍 테스트 단계:**
1. **PRO 구독 생성**
2. **구독 취소 요청**
3. **응답에서 FREE 플랜 전환 확인**
4. **통합 정보 조회로 플랜 변경 확인:**
   ```http
   GET {{baseUrl}}/api/users/{{proUserId}}/info
   ```

---

### 3. 강화된 플랜 제한 검증 테스트

#### 3.1 커스텀 API 생성 제한 테스트

**FREE 플랜 제한 초과 시나리오:**

1. **현재 제한 상태 확인:**
   ```http
   GET {{baseUrl}}/api/users/info
   X-User-Id: {{testUserId}}
   ```

2. **제한에 가까운 상태에서 API 생성 시도:**
   ```http
   POST {{baseUrl}}/api/custom-apis
   X-User-Id: {{testUserId}}
   {
     "apiName": "Test API 6",
     "description": "제한 초과 테스트"
   }
   ```

**예상 오류 응답 (새로운 개선된 메시지):**
```json
{
  "success": false,
  "message": "커스텀 API 제한을 초과했습니다. 현재: 5개, 최대: 5개. PRO 플랜으로 업그레이드하여 더 많은 기능을 이용하세요.",
  "errorCode": "PLAN_LIMIT_EXCEEDED"
}
```

#### 3.2 공유 API 생성 제한 테스트

**공유 API 제한 초과 시나리오:**

```http
POST {{baseUrl}}/api/shared-apis/share
X-User-Id: {{testUserId}}
{
  "customApiId": "api-test-004",
  "planType": "FREE",
  "apiName": "제한 초과 테스트 API",
  "apiDescription": "FREE 플랜 공유 제한 테스트"
}
```

**예상 오류 응답:**
```json
{
  "success": false,
  "message": "공유 API 제한을 초과했습니다. 현재: 3개, 최대: 3개. PRO 플랜으로 업그레이드하여 더 많은 기능을 이용하세요.",
  "errorCode": "PLAN_LIMIT_EXCEEDED"
}
```

---

## 🧪 신규 기능 통합 테스트 시나리오

### 🎯 시나리오 1: 신규 사용자 완전한 라이프사이클

```http
# 1. 사용자 생성
POST {{baseUrl}}/api/users
{
  "auth0Id": "auth0|newuser2024",
  "userEmail": "newuser2024@example.com"
}

# 2. 생성 즉시 통합 정보 확인 (FREE 플랜 자동 할당)
GET {{baseUrl}}/api/users/[새생성된userId]/info

# 3. API 생성 (제한 내)
POST {{baseUrl}}/api/custom-apis
{
  "apiName": "First API",
  "description": "첫 번째 API"
}

# 4. API 공유 (제한 내)
POST {{baseUrl}}/api/shared-apis/share
{
  "customApiId": "first-api",
  "planType": "FREE"
}

# 5. 통합 정보로 사용량 확인
GET {{baseUrl}}/api/users/[userId]/info

# 6. 제한 도달까지 API 생성 반복

# 7. 제한 초과 시도 → 오류 확인

# 8. PRO 플랜 업그레이드
POST {{baseUrl}}/api/subscription/checkout
{
  "priceId": "price_pro_plan",
  "userId": "[userId]"
}

# 9. 업그레이드 후 통합 정보 확인 (제한 증가 확인)
GET {{baseUrl}}/api/users/[userId]/info

# 10. 구독 취소 → FREE 자동 전환 확인
POST {{baseUrl}}/api/subscription/users/[userId]/subscription/cancel

# 11. 취소 후 통합 정보 확인 (FREE 플랜 복구 확인)
GET {{baseUrl}}/api/users/[userId]/info
```

### 🎯 시나리오 2: 플랜 제한 검증 스트레스 테스트

```javascript
// Postman Pre-request Script
const userId = pm.collectionVariables.get("testUserId");
const maxAttempts = 10;

// Newman이나 Postman Runner로 반복 실행하여 제한 도달 확인
for (let i = 1; i <= maxAttempts; i++) {
    pm.sendRequest({
        url: `${pm.environment.get("baseUrl")}/api/shared-apis/share`,
        method: "POST",
        header: {
            "Content-Type": "application/json",
            "X-User-Id": userId
        },
        body: {
            mode: "raw",
            raw: JSON.stringify({
                customApiId: `stress-test-api-${i}`,
                planType: "FREE",
                apiName: `Stress Test API ${i}`,
                apiDescription: `스트레스 테스트 API ${i}`
            })
        }
    }, function(err, res) {
        if (res.code === 403) {
            console.log(`제한 도달: ${i}번째 시도에서 차단됨`);
            pm.test(`Limit reached at attempt ${i}`, () => {
                pm.expect(res.json().errorCode).to.eql("PLAN_LIMIT_EXCEEDED");
            });
        }
    });
}
```

### 🎯 시나리오 3: 통합 정보 API 성능 테스트

```http
# 대량 사용자 데이터가 있는 상태에서 응답 시간 측정
GET {{baseUrl}}/api/users/info
X-User-Id: {{testUserId}}
```

**성능 목표:**
- 응답 시간: < 500ms
- 동시 요청 처리: 100 req/sec
- 메모리 사용량: 안정적

---

## 📊 새로운 모니터링 포인트

### 실시간 모니터링 대시보드 업데이트

기존 대시보드에 새로운 메트릭 추가:

```http
GET {{baseUrl}}/test/dashboard
```

**새로 추가된 모니터링 항목:**
1. **통합 정보 API 호출 횟수**
2. **플랜 제한 위반 시도 횟수**
3. **구독 취소 → FREE 전환 성공률**
4. **오류 메시지별 발생 빈도**

### 로그 모니터링

```bash
# 새로운 기능 관련 로그 확인
grep "통합 사용자 정보 조회" logs/userservice.log
grep "플랜 제한 초과" logs/userservice.log
grep "FREE 플랜 전환" logs/userservice.log
grep "PlanLimitExceededException" logs/userservice.log
```

---

## 🔧 Postman Collection 업데이트

### 새로운 Collection Variables

```json
{
  "baseUrl": "http://localhost:8081",
  "testUserId": "user-001",
  "proUserId": "user-002",
  "newUserId": "",
  "integrationTestUserId": "",
  "stressTestApiCounter": 0
}
```

### 새로운 Test Scripts

**통합 정보 API 테스트:**
```javascript
pm.test("User info response structure", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.data).to.have.property("userId");
    pm.expect(jsonData.data).to.have.property("planInfo");
    pm.expect(jsonData.data).to.have.property("usageLimits");
    pm.expect(jsonData.data).to.have.property("currentUsage");
});

pm.test("Plan info completeness", function () {
    const planInfo = pm.response.json().data.planInfo;
    pm.expect(planInfo).to.have.property("planType");
    pm.expect(planInfo).to.have.property("isActive");
    pm.expect(planInfo).to.have.property("price");
});

pm.test("Usage limits defined", function () {
    const limits = pm.response.json().data.usageLimits;
    pm.expect(limits.maxCustomApiCount).to.be.a('number');
    pm.expect(limits.maxSharedApiCount).to.be.a('number');
    pm.expect(limits.rateLimitPerMinute).to.be.a('number');
});
```

**플랜 제한 검증 테스트:**
```javascript
pm.test("Plan limit error response", function () {
    if (pm.response.code === 403) {
        const jsonData = pm.response.json();
        pm.expect(jsonData.errorCode).to.eql("PLAN_LIMIT_EXCEEDED");
        pm.expect(jsonData.message).to.include("제한을 초과했습니다");
        pm.expect(jsonData.message).to.include("PRO 플랜으로 업그레이드");
    }
});
```

---

## 📈 성능 벤치마크

### 새로운 성능 목표

**통합 정보 API:**
- 평균 응답 시간: < 300ms
- 95% 응답 시간: < 500ms
- 동시 요청 100개: 성공률 99%+

**플랜 제한 검증:**
- 검증 시간: < 50ms
- 캐시 히트율: > 80%

### Apache Bench 테스트

```bash
# 통합 정보 API 성능 테스트
ab -n 1000 -c 50 -H "X-User-Id: user-001" \
   http://localhost:8081/api/users/user-001/info

# 플랜 제한 API 성능 테스트
ab -n 500 -c 25 -H "X-User-Id: user-001" \
   http://localhost:8081/api/plan/limits
```

---

## 🚨 새로운 트러블슈팅 가이드

### 자주 발생하는 문제들

**1. 통합 정보 API 느린 응답:**
```bash
# DB 연결 풀 상태 확인
curl http://localhost:8081/actuator/metrics/hikari.connections.active

# JVM 메모리 상태 확인
curl http://localhost:8081/actuator/metrics/jvm.memory.used
```

**2. 플랜 제한 검증 실패:**
```http
# 사용자 현재 상태 확인
GET {{baseUrl}}/api/users/info
X-User-Id: {{testUserId}}

# 플랜 타입 확인
GET {{baseUrl}}/api/subscription/current
X-User-Id: {{testUserId}}
```

**3. 구독 취소 후 FREE 전환 실패:**
```bash
# 구독 생성 로그 확인
grep "FREE 구독 생성" logs/userservice.log

# 트랜잭션 롤백 로그 확인
grep "Transaction rolled back" logs/userservice.log
```

---

## 🔗 공유 API 테스트 가이드

### 📋 공유 API 엔드포인트 목록

#### 1. 사용자별 공유 API 관리 (`/api/users/{userId}/shared-apis`)

**API 공유하기:**
```http
POST {{baseUrl}}/api/users/{{testUserId}}/shared-apis/share
?customApiId=custom-api-001
&planType=FREE
&apiName=My Shared Weather API
&apiDescription=날씨 정보를 제공하는 공유 API
```

**예상 응답:**
```json
{
  "success": true,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "sharedApiId": "shared-api-uuid-001",
    "originalApiId": "custom-api-001", 
    "creatorId": "{{testUserId}}",
    "apiName": "My Shared Weather API",
    "description": "날씨 정보를 제공하는 공유 API",
    "dataCount": 150,
    "isActive": true,
    "createdAt": "2024-08-20T15:30:00",
    "updatedAt": "2024-08-20T15:30:00"
  }
}
```

**내가 공유한 API 목록 조회:**
```http
GET {{baseUrl}}/api/users/{{testUserId}}/shared-apis/my
```

**예상 응답:**
```json
{
  "success": true,
  "data": [
    {
      "sharedApiId": "shared-api-uuid-001",
      "originalApiId": "custom-api-001",
      "creatorId": "{{testUserId}}",
      "apiName": "My Shared Weather API", 
      "description": "날씨 정보를 제공하는 공유 API",
      "dataCount": 150,
      "isActive": true,
      "createdAt": "2024-08-20T15:30:00"
    }
  ]
}
```

**공유 취소하기 (Shared API ID로):**
```http
DELETE {{baseUrl}}/api/users/{{testUserId}}/shared-apis/unshare/shared-api-uuid-001
```

**공유 취소하기 (Custom API ID로):**
```http
DELETE {{baseUrl}}/api/users/{{testUserId}}/shared-apis/unshare
?customApiId=custom-api-001
```

**공유 API 저장하기:**
```http
POST {{baseUrl}}/api/users/{{testUserId}}/shared-apis/save
?sharedApiId=shared-api-uuid-002
```

**예상 응답:**
```json
{
  "success": true,
  "data": {
    "userApiId": "user-saved-api-uuid-001",
    "userId": "{{testUserId}}",
    "sharedApiId": "shared-api-uuid-002",
    "apiName": "Weather Forecast API",
    "description": "7일간 날씨 예보 API",
    "dataCount": 200,
    "isDeleted": false,
    "createdAt": "2024-08-20T15:45:00"
  }
}
```

**저장된 API 목록 조회 (페이징):**
```http
GET {{baseUrl}}/api/users/{{testUserId}}/shared-apis/saved
?page=0&size=10
```

**예상 응답:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "userApiId": "user-saved-api-uuid-001",
        "userId": "{{testUserId}}",
        "sharedApiId": "shared-api-uuid-002", 
        "apiName": "Weather Forecast API",
        "description": "7일간 날씨 예보 API",
        "dataCount": 200,
        "isDeleted": false,
        "createdAt": "2024-08-20T15:45:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": { "sorted": false }
    },
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

**저장된 API 삭제:**
```http
DELETE {{baseUrl}}/api/users/{{testUserId}}/shared-apis/saved/user-saved-api-uuid-001
```

#### 2. 글로벌 공유 API 조회 (`/api/shared-apis`)

**모든 공유 API 목록 조회:**
```http
GET {{baseUrl}}/api/shared-apis
?page=0&size=20
```

**예상 응답:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "sharedApiId": "shared-api-uuid-001",
        "originalApiId": "custom-api-001",
        "creatorId": "user-creator-001", 
        "apiName": "Weather API Collection",
        "description": "전 세계 도시 날씨 정보 API",
        "dataCount": 500,
        "isActive": true,
        "createdAt": "2024-08-20T14:00:00"
      },
      {
        "sharedApiId": "shared-api-uuid-002", 
        "originalApiId": "custom-api-002",
        "creatorId": "user-creator-002",
        "apiName": "Stock Price API",
        "description": "실시간 주식 가격 정보",
        "dataCount": 300,
        "isActive": true,
        "createdAt": "2024-08-20T13:30:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 2,
    "totalPages": 1
  }
}
```

**공유 API 검색:**
```http
GET {{baseUrl}}/api/shared-apis/search
?keyword=weather
```

**예상 응답:**
```json
{
  "success": true,
  "data": [
    {
      "sharedApiId": "shared-api-uuid-001",
      "originalApiId": "custom-api-001",
      "creatorId": "user-creator-001",
      "apiName": "Weather API Collection", 
      "description": "전 세계 도시 날씨 정보 API",
      "dataCount": 500,
      "isActive": true,
      "createdAt": "2024-08-20T14:00:00"
    }
  ]
}
```

### 🧪 공유 API 테스트 시나리오

#### 시나리오 1: 기본 공유/저장 플로우

```http
# 1. 커스텀 API를 공유 게시판에 게시
POST {{baseUrl}}/api/users/{{testUserId}}/shared-apis/share
?customApiId=my-custom-api-001
&planType=FREE
&apiName=Amazing Weather API
&apiDescription=정확한 날씨 예보를 제공합니다

# 2. 내가 공유한 API 목록 확인
GET {{baseUrl}}/api/users/{{testUserId}}/shared-apis/my

# 3. 글로벌 공유 API 목록에서 확인
GET {{baseUrl}}/api/shared-apis?page=0&size=10

# 4. 다른 사용자가 내 API를 저장
POST {{baseUrl}}/api/users/{{anotherUserId}}/shared-apis/save
?sharedApiId={{sharedApiId}}

# 5. 저장된 API 목록 확인  
GET {{baseUrl}}/api/users/{{anotherUserId}}/shared-apis/saved

# 6. 공유 취소
DELETE {{baseUrl}}/api/users/{{testUserId}}/shared-apis/unshare/{{sharedApiId}}

# 7. 취소 후 글로벌 목록에서 사라졌는지 확인
GET {{baseUrl}}/api/shared-apis?page=0&size=10
```

#### 시나리오 2: FREE 플랜 공유 제한 테스트

```http
# 1. 현재 사용자 정보 및 제한 확인
GET {{baseUrl}}/api/users/info
X-User-Id: {{freeUserId}}

# 2. 공유 API 3개까지 생성 (FREE 플랜 제한)
POST {{baseUrl}}/api/users/{{freeUserId}}/shared-apis/share
?customApiId=api-001&planType=FREE&apiName=API 1

POST {{baseUrl}}/api/users/{{freeUserId}}/shared-apis/share  
?customApiId=api-002&planType=FREE&apiName=API 2

POST {{baseUrl}}/api/users/{{freeUserId}}/shared-apis/share
?customApiId=api-003&planType=FREE&apiName=API 3

# 3. 4번째 공유 시도 - 제한 초과 오류 확인
POST {{baseUrl}}/api/users/{{freeUserId}}/shared-apis/share
?customApiId=api-004&planType=FREE&apiName=API 4
```

**예상 오류 응답:**
```json
{
  "success": false,
  "message": "공유 API 제한을 초과했습니다. 현재: 3개, 최대: 3개. PRO 플랜으로 업그레이드하여 더 많은 기능을 이용하세요.",
  "errorCode": "PLAN_LIMIT_EXCEEDED"
}
```

#### 시나리오 3: 검색 및 필터링 테스트

```http
# 1. 키워드 검색 테스트
GET {{baseUrl}}/api/shared-apis/search?keyword=weather
GET {{baseUrl}}/api/shared-apis/search?keyword=api
GET {{baseUrl}}/api/shared-apis/search?keyword=data

# 2. 페이징 테스트
GET {{baseUrl}}/api/shared-apis?page=0&size=5
GET {{baseUrl}}/api/shared-apis?page=1&size=5
GET {{baseUrl}}/api/shared-apis?page=2&size=5

# 3. 빈 결과 테스트
GET {{baseUrl}}/api/shared-apis/search?keyword=nonexistentapi
```

### 🔍 에러 케이스 테스트

#### 1. 권한 없는 공유 취소 시도
```http
DELETE {{baseUrl}}/api/users/{{wrongUserId}}/shared-apis/unshare/{{sharedApiId}}
```
**예상 응답: 403 Forbidden**

#### 2. 존재하지 않는 API 공유 시도
```http
POST {{baseUrl}}/api/users/{{testUserId}}/shared-apis/share
?customApiId=non-existent-api&planType=FREE
```
**예상 응답: 404 Not Found**

#### 3. 이미 공유된 API 중복 공유 시도
```http
POST {{baseUrl}}/api/users/{{testUserId}}/shared-apis/share
?customApiId=already-shared-api&planType=FREE
```
**예상 응답: 400 Bad Request**

#### 4. 존재하지 않는 공유 API 저장 시도
```http
POST {{baseUrl}}/api/users/{{testUserId}}/shared-apis/save
?sharedApiId=non-existent-shared-api
```
**예상 응답: 404 Not Found**

### 📊 성능 테스트

```bash
# 공유 API 목록 조회 성능 테스트
ab -n 1000 -c 50 "{{baseUrl}}/api/shared-apis?page=0&size=20"

# 검색 성능 테스트  
ab -n 500 -c 25 "{{baseUrl}}/api/shared-apis/search?keyword=weather"

# 사용자별 공유 API 조회 성능 테스트
ab -n 1000 -c 50 "{{baseUrl}}/api/users/{{testUserId}}/shared-apis/my"
```

**성능 목표:**
- 목록 조회: 평균 응답시간 < 200ms
- 검색: 평균 응답시간 < 300ms  
- 개인 API 조회: 평균 응답시간 < 100ms

### 🧪 Postman 테스트 스크립트

**공유 API 응답 검증:**
```javascript
pm.test("Shared API response structure", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.success).to.be.true;
    pm.expect(jsonData.data).to.have.property("sharedApiId");
    pm.expect(jsonData.data).to.have.property("originalApiId");
    pm.expect(jsonData.data).to.have.property("creatorId");
    pm.expect(jsonData.data).to.have.property("apiName");
    pm.expect(jsonData.data).to.have.property("isActive");
});

pm.test("Shared API is active", function () {
    const sharedApi = pm.response.json().data;
    pm.expect(sharedApi.isActive).to.be.true;
    pm.expect(sharedApi.dataCount).to.be.a('number');
    pm.expect(sharedApi.dataCount).to.be.at.least(0);
});

// 공유 API ID를 변수로 저장
if (pm.response.code === 200) {
    const responseData = pm.response.json().data;
    pm.collectionVariables.set("sharedApiId", responseData.sharedApiId);
}
```

**제한 초과 에러 검증:**
```javascript
pm.test("Shared API limit exceeded error", function () {
    if (pm.response.code === 403) {
        const jsonData = pm.response.json();
        pm.expect(jsonData.success).to.be.false;
        pm.expect(jsonData.errorCode).to.eql("PLAN_LIMIT_EXCEEDED");
        pm.expect(jsonData.message).to.include("공유 API 제한을 초과했습니다");
        pm.expect(jsonData.message).to.include("PRO 플랜으로 업그레이드");
    }
});
```

---

## ✅ 최종 검증 체크리스트

### 필수 검증 항목

- [ ] **통합 정보 API 정상 응답** (모든 필드 포함)
- [ ] **FREE 플랜 사용자 제한 정확성** (5개 커스텀 API, 3개 공유 API)
- [ ] **PRO 플랜 사용자 제한 정확성** (50개 커스텀 API, 20개 공유 API)
- [ ] **제한 초과 시 정확한 오류 메시지**
- [ ] **업그레이드 권장 메시지 포함**
- [ ] **구독 취소 시 FREE 플랜 자동 전환**
- [ ] **취소 후 새 구독 ID 생성 확인**
- [ ] **Stripe 구독 실제 취소 확인**

### 성능 검증 항목

- [ ] **통합 정보 API 응답 시간 < 500ms**
- [ ] **제한 검증 로직 응답 시간 < 100ms**
- [ ] **동시 요청 100개 처리 가능**
- [ ] **메모리 누수 없음**

### 데이터 정합성 검증

- [ ] **Plan 엔티티 created_at/updated_at 필드 정상 동작**
- [ ] **DB 스키마와 엔티티 완전 일치**
- [ ] **JSON 응답 구조 일관성**

이제 모든 신규 기능을 포함한 완전한 테스트 가이드가 준비되었습니다! 🎉