# UserService 테스트 가이드

본 문서는 BE-UserService의 모든 기능을 테스트하는 방법을 단계별로 설명합니다.

## 🚀 빠른 시작

### 1. 환경 설정

#### 필수 의존성
- Java 17+
- Gradle 8.14.3
- Docker (Redis용)
- Stripe 계정 (테스트 키)

#### 환경변수 설정
```bash
# application.yml 또는 환경변수로 설정
STRIPE_API_KEY=sk_test_your_stripe_secret_key
STRIPE_PUBLISHABLE_KEY=pk_test_your_stripe_publishable_key
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret

# Stripe 제품 및 가격 ID (Stripe 대시보드에서 생성)
STRIPE_PRODUCT_FREE=prod_free_plan_id
STRIPE_PRODUCT_PRO=prod_pro_plan_id
STRIPE_PRODUCT_ENTERPRISE=prod_enterprise_plan_id

STRIPE_PRICE_PRO_MONTHLY=price_pro_monthly_id
STRIPE_PRICE_PRO_YEARLY=price_pro_yearly_id
STRIPE_PRICE_ENTERPRISE_MONTHLY=price_enterprise_monthly_id
STRIPE_PRICE_ENTERPRISE_YEARLY=price_enterprise_yearly_id
```

### 2. 서비스 시작
```bash
# Redis 시작 (Docker)
docker run -d -p 6379:6379 redis:latest

# 애플리케이션 시작
./gradlew bootRun
```

서버는 `http://localhost:8082`에서 실행됩니다.

## 📋 기능별 테스트 가이드

### 1. 헬스체크 테스트

```bash
# 서버 상태 확인
curl -X GET http://localhost:8082/api/health
```

**예상 응답:**
```json
{
  "success": true,
  "message": "UserService is running",
  "timestamp": "2024-01-01T12:00:00"
}
```

### 2. 사용자 관리 테스트

#### 2.1 사용자 등록
```bash
curl -X POST http://localhost:8082/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "auth0Id": "auth0|test123456",
    "userEmail": "test@example.com"
  }'
```

#### 2.2 사용자 조회
```bash
# Auth0 ID로 조회
curl -X GET "http://localhost:8082/api/users/auth0/auth0|test123456"

# 이메일로 조회
curl -X GET "http://localhost:8082/api/users/email/test@example.com"

# 전체 사용자 조회
curl -X GET http://localhost:8082/api/users
```

#### 2.3 개인 키(BYOK) 관리
```bash
# 개인 키 등록
curl -X POST http://localhost:8082/api/users/{userId}/secrets \
  -H "Content-Type: application/json" \
  -H "User-ID: {userId}" \
  -d '{
    "secretName": "my-api-key",
    "secretValue": "sk-test-123456789",
    "description": "My OpenAI API Key"
  }'

# 개인 키 조회
curl -X GET http://localhost:8082/api/users/{userId}/secrets \
  -H "User-ID: {userId}"
```

### 3. 구독 관리 테스트

#### 3.1 구독 플랜 조회
```bash
# 사용 가능한 모든 플랜 조회
curl -X GET http://localhost:8082/api/subscription/plans
```

**예상 응답:**
```json
{
  "success": true,
  "data": [
    {
      "planName": "FREE",
      "maxApiCount": 5,
      "rateLimitPerMinute": 10,
      "rateLimitPerHour": 100,
      "rateLimitPerDay": 1000,
      "monthlyPrice": 0
    },
    {
      "planName": "PRO",
      "maxApiCount": 50,
      "rateLimitPerMinute": 100,
      "rateLimitPerHour": 1000,
      "rateLimitPerDay": 10000,
      "monthlyPrice": 2999
    }
  ]
}
```

#### 3.2 구독 생성
```bash
# 유료 구독 생성
curl -X POST http://localhost:8082/api/subscription \
  -H "Content-Type: application/json" \
  -H "User-ID: {userId}" \
  -d '{
    "planName": "PRO",
    "billingPeriod": "MONTHLY"
  }'

# 무료 구독 생성
curl -X POST http://localhost:8082/api/subscription \
  -H "Content-Type: application/json" \
  -H "User-ID: {userId}" \
  -d '{
    "planName": "FREE",
    "billingPeriod": "MONTHLY"
  }'
```

#### 3.3 현재 구독 조회
```bash
curl -X GET http://localhost:8082/api/subscription/current \
  -H "User-ID: {userId}"
```

#### 3.4 구독 변경
```bash
curl -X PUT http://localhost:8082/api/subscription \
  -H "Content-Type: application/json" \
  -H "User-ID: {userId}" \
  -d '{
    "stripeSubscriptionId": "sub_stripe_subscription_id",
    "newPlanName": "ENTERPRISE",
    "billingPeriod": "YEARLY"
  }'
```

#### 3.5 구독 취소
```bash
curl -X DELETE http://localhost:8082/api/subscription \
  -H "User-ID: {userId}" \
  -d '{
    "stripeSubscriptionId": "sub_stripe_subscription_id"
  }'
```

### 4. API 사용량 제한 테스트

#### 4.1 Rate Limiting 테스트
```bash
# 여러 번 빠르게 요청하여 Rate Limit 테스트
for i in {1..15}; do
  curl -X POST http://localhost:8082/api/test \
    -H "User-ID: {userId}" \
    -H "Content-Type: application/json"
  echo "Request $i completed"
done
```

**FREE 플랜 제한 초과 시 예상 응답:**
```json
{
  "success": false,
  "message": "API 호출 제한을 초과했습니다. 잠시 후 다시 시도해주세요.",
  "timestamp": "2024-01-01T12:00:00"
}
```

#### 4.2 API 생성 한도 테스트
```bash
# FREE 플랜 사용자가 6개 이상의 API 생성 시도
for i in {1..7}; do
  curl -X POST http://localhost:8082/api/custom \
    -H "User-ID: {userId}" \
    -H "Content-Type: application/json" \
    -d '{
      "apiName": "test-api-'$i'",
      "description": "Test API '$i'"
    }'
done
```

### 5. Stripe 웹훅 테스트

#### 5.1 웹훅 엔드포인트 확인
```bash
curl -X POST http://localhost:8082/api/webhook/stripe \
  -H "Content-Type: application/json" \
  -H "Stripe-Signature: test-signature" \
  -d '{
    "type": "customer.subscription.created",
    "data": {
      "object": {
        "id": "sub_test123",
        "customer": "cus_test123",
        "status": "active"
      }
    }
  }'
```

### 6. 사용량 통계 조회

```bash
# 사용자별 사용량 통계
curl -X GET http://localhost:8082/api/usage/stats \
  -H "User-ID: {userId}"

# 오늘의 API 호출 수
curl -X GET http://localhost:8082/api/usage/today \
  -H "User-ID: {userId}"
```

## 🧪 자동화된 테스트 실행

### 단위 테스트
```bash
# 전체 테스트 실행
./gradlew test

# 특정 서비스 테스트
./gradlew test --tests "*UserServiceTest"
./gradlew test --tests "*SubscriptionManagementServiceTest"
./gradlew test --tests "*StripeWebhookServiceTest"

# 인터셉터 테스트
./gradlew test --tests "*SubscriptionInterceptorTest"
```

### 통합 테스트
```bash
# 컨트롤러 통합 테스트
./gradlew test --tests "*ControllerTest"

# 데이터베이스 통합 테스트
./gradlew test --tests "*RepositoryTest"
```

### 테스트 커버리지 확인
```bash
./gradlew jacocoTestReport
# 결과: build/reports/jacoco/test/html/index.html
```

## 📊 모니터링 및 로그

### 로그 확인
```bash
# 애플리케이션 로그
tail -f logs/application.log

# Stripe 관련 로그 확인
grep "Stripe" logs/application.log

# Rate Limit 로그 확인
grep "Rate Limit" logs/application.log
```

### Redis 데이터 확인
```bash
# Redis 연결
redis-cli

# Rate Limit 카운터 확인
KEYS rate_limit:*

# API 사용량 확인
KEYS usage:*

# 특정 사용자의 API 카운트 확인
GET api_count:user-12345
```

## 🚨 트러블슈팅

### 일반적인 문제들

#### 1. Stripe 연결 실패
```bash
# Stripe API 키 확인
curl -H "Authorization: Bearer sk_test_your_key" \
  https://api.stripe.com/v1/account
```

#### 2. Redis 연결 실패
```bash
# Redis 서버 상태 확인
redis-cli ping
# 응답: PONG
```

#### 3. 데이터베이스 연결 실패
```bash
# H2 콘솔 접속 (개발 환경)
http://localhost:8082/h2-console
# JDBC URL: jdbc:h2:mem:testdb
# Username: sa
# Password: (비어있음)
```

#### 4. 구독 상태 불일치
```bash
# 사용자의 현재 구독 상태 확인
curl -X GET http://localhost:8082/api/subscription/current \
  -H "User-ID: {userId}"

# Stripe에서 구독 상태 확인
curl -H "Authorization: Bearer sk_test_your_key" \
  https://api.stripe.com/v1/subscriptions/{subscription_id}
```

## 📈 성능 테스트

### 부하 테스트 (Apache Bench 사용)
```bash
# 동시 10명 사용자, 총 100개 요청
ab -n 100 -c 10 -H "User-ID: test-user" \
  http://localhost:8082/api/health

# Rate Limit 성능 테스트
ab -n 50 -c 5 -H "User-ID: test-user" \
  http://localhost:8082/api/test
```

### JMeter 테스트 시나리오
1. 사용자 등록 → 구독 생성 → API 호출 → 구독 변경 → 구독 취소
2. 다양한 플랜별 Rate Limit 테스트
3. 동시 다중 사용자 시나리오

## ✅ 테스트 체크리스트

### 기본 기능
- [ ] 서버 시작 및 헬스체크
- [ ] 사용자 등록/조회/수정/삭제
- [ ] BYOK 등록/조회

### 구독 관리
- [ ] 무료 플랜 자동 할당
- [ ] 유료 구독 생성
- [ ] 구독 플랜 변경
- [ ] 구독 취소 및 무료 플랜 복귀

### Stripe 연동
- [ ] 고객 생성
- [ ] 구독 생성/수정/취소
- [ ] 웹훅 이벤트 처리
- [ ] 결제 성공/실패 처리

### 사용량 제한
- [ ] Rate Limiting (분/시간/일별)
- [ ] API 생성 한도 제한
- [ ] 플랜별 기능 제한
- [ ] 만료된 구독 차단

### 보안 및 인증
- [ ] 사용자 ID 헤더 검증
- [ ] 인증되지 않은 요청 차단
- [ ] 개인 키 암호화 저장

이 가이드를 따라 테스트하면 UserService의 모든 기능이 정상적으로 작동하는지 확인할 수 있습니다.