# 🚀 BE-UserService

**Stripe 기반 구독 관리 및 사용량 제한 기능을 갖춘 고성능 사용자 서비스**

Auth0 인증, Stripe 결제, Redis 기반 Rate Limiting을 통합한 엔터프라이즈급 마이크로서비스입니다.

## 📋 목차

- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [아키텍처](#-아키텍처)
- [빠른 시작](#-빠른-시작)
- [Stripe 연동 설정](#-stripe-연동-설정)
- [API 문서](#-api-문서)
- [구독 플랜 및 제한](#-구독-플랜-및-제한)
- [사용량 제한 시스템](#-사용량-제한-시스템)
- [웹훅 처리](#-웹훅-처리)
- [모니터링 및 로깅](#-모니터링-및-로깅)
- [배포 가이드](#-배포-가이드)
- [기여하기](#-기여하기)

## ✨ 주요 기능

### 🔐 사용자 관리
- **Auth0 기반 인증 시스템** - JWT 토큰 기반 보안
- **사용자 CRUD 연산** - 생성, 조회, 수정, 삭제
- **BYOK (Bring Your Own Key)** - 개인 API 키 안전 관리
- **이벤트 기반 아키텍처** - Kafka를 통한 비동기 처리

### 💳 Stripe 기반 구독 관리
- **3단계 구독 플랜** - FREE, PRO
- **유연한 결제 주기** - 월간/연간 구독 지원
- **자동 구독 관리** - 생성, 변경, 취소, 갱신
- **실시간 웹훅 처리** - 결제 이벤트 즉시 반영
- **자동 Fallback** - 결제 실패 시 FREE 플랜으로 자동 전환

### ⚡ 고성능 사용량 제한
- **Redis 기반 Rate Limiting** - 분/시간/일별 제한
- **플랜별 API 제한** - 생성 가능한 API 개수 제한
- **실시간 사용량 추적** - 통계 및 분석 데이터 제공
- **인터셉터 기반 제어** - 모든 API 요청 사전 검증

### 📊 모니터링 & 분석
- **구조화된 로깅** - 모든 작업 추적 가능
- **성능 메트릭** - 응답 시간 및 처리량 모니터링
- **사용량 통계** - 사용자별 상세 분석 데이터
- **알림 시스템** - 제한 초과 및 오류 알림

## 🛠 기술 스택

### Backend
- **Java 17** - 최신 LTS 버전
- **Spring Boot 3.5.4** - 엔터프라이즈 프레임워크
- **Spring Security** - 보안 및 인증
- **Spring Data JPA** - ORM 및 데이터 액세스
- **Hibernate** - JPA 구현체

### Database & Cache
- **MySQL** - 운영 환경 메인 데이터베이스
- **H2** - 테스트 환경 인메모리 DB
- **Redis** - 캐싱 및 Rate Limiting

### External Services
- **Stripe** - 결제 및 구독 관리
- **Auth0** - 사용자 인증 및 권한 관리
- **AWS Secrets Manager** - 보안 키 관리
- **Apache Kafka** - 이벤트 스트리밍

### Build & Test
- **Gradle 8.14.3** - 빌드 도구
- **JUnit 5** - 단위 테스트
- **Mockito** - 모킹 프레임워크
- **TestContainers** - 통합 테스트
- **JaCoCo** - 테스트 커버리지

## 🏗 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client Apps   │    │   Auth0         │    │   Stripe        │
│   (Frontend)    │    │   (Authentication)   │   (Payment)     │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          │ HTTP/REST            │ JWT Tokens           │ Webhooks
          │                      │                      │
┌─────────▼──────────────────────▼──────────────────────▼───────┐
│                    API Gateway / Load Balancer                │
└─────────┬─────────────────────────────────────────────────────┘
          │
┌─────────▼─────────────────────────────────────────────────────┐
│                      BE-UserService                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐           │
│  │Controllers  │  │Interceptors │  │Event        │           │
│  │             │  │- Subscription│  │Publishers   │           │
│  │- User       │  │- RateLimit  │  │             │           │
│  │- Subscription│  │- Usage Track│  │- UserEvents │           │
│  │- Webhook    │  │             │  │- SubEvents  │           │
│  └─────┬───────┘  └─────┬───────┘  └─────┬───────┘           │
│        │                │                │                   │
│  ┌─────▼─────────────────▼────────────────▼───────┐           │
│  │                Services Layer                  │           │
│  │- UserService                                   │           │
│  │- SubscriptionManagementService                │           │
│  │- SubscriptionPlanManagementService            │           │
│  │- StripeCustomerService                        │           │
│  │- StripeSubscriptionService                    │           │
│  │- StripeWebhookService                         │           │
│  │- UserSecretsArnService                        │           │
│  └─────┬─────────────────────────────────────────┘           │
│        │                                                     │
│  ┌─────▼─────────────────────────────────────────┐           │
│  │              Repository Layer                  │           │
│  │- UserRepository                               │           │
│  │- UserSubscriptionRepository                  │           │
│  │- SubscriptionPlanRepository                  │           │
│  │- UserSecretsArnRepository                    │           │
│  └─────┬─────────────────────────────────────────┘           │
└────────┼─────────────────────────────────────────────────────┘
         │
┌────────▼─────────────────────────────────────────────────────┐
│                    Data & Cache Layer                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐           │
│  │   MySQL     │  │   Redis     │  │    AWS      │           │
│  │             │  │             │  │  Secrets    │           │
│  │- Users      │  │- Rate Limits│  │  Manager    │           │
│  │- Subscriptions│ │- Usage Data │  │             │           │
│  │- Plans      │  │- Cache      │  │- API Keys   │           │
│  │- Secrets    │  │             │  │- Secrets    │           │
│  └─────────────┘  └─────────────┘  └─────────────┘           │
└─────────────────────────────────────────────────────────────┘
```

### 주요 컴포넌트

#### 1. **API Layer (Controllers)**
- `UserController`: 사용자 관리 엔드포인트
- `SubscriptionController`: 구독 관리 엔드포인트  
- `WebhookController`: Stripe 웹훅 처리
- `HealthController`: 헬스체크 및 모니터링

#### 2. **Middleware Layer (Interceptors)**
- `SubscriptionInterceptor`: 구독 상태 및 권한 검증
- `RateLimitInterceptor`: API 호출 빈도 제한
- `ApiUsageTrackingInterceptor`: 사용량 추적 및 로깅

#### 3. **Business Logic Layer (Services)**
- `UserService`: 사용자 생명주기 관리
- `SubscriptionManagementService`: 구독 생명주기 관리
- `StripeIntegrationServices`: Stripe API 통합
- `EventPublisherService`: 비동기 이벤트 발행

#### 4. **Data Access Layer (Repositories)**
- JPA 기반 데이터 접근
- Redis 기반 캐시 및 세션 관리
- AWS Secrets Manager 연동

## 🚀 빠른 시작

### 1. 사전 요구사항

```bash
# Java 17 설치 확인
java -version

# Docker 설치 확인 (Redis용)
docker --version

# Gradle 설치 확인
./gradlew --version
```

### 2. 프로젝트 클론 및 의존성 설치

```bash
git clone <repository-url>
cd BE-UserService

# 의존성 설치 및 빌드
./gradlew build
```

### 3. Redis 시작

```bash
# Docker로 Redis 실행
docker run -d \
  --name redis-userservice \
  -p 6379:6379 \
  redis:latest

# Redis 연결 확인
redis-cli ping
# 응답: PONG
```

### 4. 환경 설정

`src/main/resources/application.yml` 파일 수정:

```yaml
spring:
  profiles:
    active: dev
  
server:
  port: 8082

# Stripe 설정 (필수)
stripe:
  api-key: ${STRIPE_API_KEY:sk_test_your_stripe_secret_key}
  publishable-key: ${STRIPE_PUBLISHABLE_KEY:pk_test_your_stripe_publishable_key}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET:whsec_your_webhook_secret}
  
  # Stripe 제품 ID
  products:
    free: ${STRIPE_PRODUCT_FREE:prod_free}
    pro: ${STRIPE_PRODUCT_PRO:prod_pro}  
    enterprise: ${STRIPE_PRODUCT_ENTERPRISE:prod_enterprise}
  
  # Stripe 가격 ID
  prices:
    pro-monthly: ${STRIPE_PRICE_PRO_MONTHLY:price_pro_monthly}
    pro-yearly: ${STRIPE_PRICE_PRO_YEARLY:price_pro_yearly}
    enterprise-monthly: ${STRIPE_PRICE_ENTERPRISE_MONTHLY:price_enterprise_monthly}
    enterprise-yearly: ${STRIPE_PRICE_ENTERPRISE_YEARLY:price_enterprise_yearly}

# Redis 설정
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
```

### 5. 애플리케이션 실행

```bash
# 개발 모드로 실행
./gradlew bootRun

# 또는 JAR 파일로 실행
./gradlew bootJar
java -jar build/libs/BE-UserService-*.jar
```

서버가 `http://localhost:8082`에서 실행됩니다.

### 6. 동작 확인

```bash
# 헬스체크
curl http://localhost:8082/api/health

# 응답:
{
  "success": true,
  "message": "UserService is running",
  "timestamp": "2024-01-01T12:00:00"
}
```

## 💳 Stripe 연동 설정

### ✅ 완료! Stripe 키만 설정하면 바로 사용 가능!

이 서비스는 **환경변수에 Stripe 키만 설정하면 바로 사용할 수 있도록** 완전히 구현되어 있습니다.

### 1. Stripe 계정 설정

1. **Stripe 계정 생성**: https://stripe.com
2. **개발자 도구 접속**: Dashboard → Developers
3. **API 키 획득**: 
   - 테스트 환경: `sk_test_...`, `pk_test_...`
   - 운영 환경: `sk_live_...`, `pk_live_...`

### 2. 제품 및 가격 생성

#### Stripe Dashboard에서 제품 생성:

```bash
# 또는 Stripe CLI로 생성
stripe products create \
  --name="Pro Plan" \
  --description="Professional features"

stripe prices create \
  --unit-amount=2999 \
  --currency=krw \
  --recurring[interval]=month \
  --product=prod_XXX
```

#### 필요한 제품 및 가격:
- **FREE**: 무료 플랜 (제품만 생성, 가격 없음)
- **PRO**: 
  - 월간: ₩2,999 (`price_pro_monthly`)
  - 연간: ₩29,990 (`price_pro_yearly`)
- **ENTERPRISE**: 
  - 월간: ₩9,999 (`price_enterprise_monthly`)
  - 연간: ₩99,990 (`price_enterprise_yearly`)

### 3. 웹훅 설정

1. **웹훅 엔드포인트 생성**: Dashboard → Developers → Webhooks
2. **엔드포인트 URL**: `https://your-domain.com/api/webhook/stripe`
3. **수신할 이벤트**:
   ```
   customer.subscription.created
   customer.subscription.updated
   customer.subscription.deleted
   invoice.payment_succeeded
   invoice.payment_failed
   payment_method.attached
   customer.updated
   ```

### 4. 환경변수 설정 (이것만 하면 끝!)

```bash
# .env 파일 생성
cat > .env << EOF
STRIPE_API_KEY=sk_test_your_secret_key
STRIPE_PUBLISHABLE_KEY=pk_test_your_publishable_key
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret

STRIPE_PRODUCT_FREE=prod_free_id
STRIPE_PRODUCT_PRO=prod_pro_id
STRIPE_PRODUCT_ENTERPRISE=prod_enterprise_id

STRIPE_PRICE_PRO_MONTHLY=price_pro_monthly_id
STRIPE_PRICE_PRO_YEARLY=price_pro_yearly_id
STRIPE_PRICE_ENTERPRISE_MONTHLY=price_enterprise_monthly_id
STRIPE_PRICE_ENTERPRISE_YEARLY=price_enterprise_yearly_id
EOF

# 환경변수 로드
source .env
```

### 5. 연동 테스트

```bash
# Stripe 연결 확인
curl -H "Authorization: Bearer $STRIPE_API_KEY" \
  https://api.stripe.com/v1/account

# 테스트 구독 생성
curl -X POST http://localhost:8082/api/subscription \
  -H "Content-Type: application/json" \
  -H "User-ID: test-user" \
  -d '{
    "planName": "PRO",
    "billingPeriod": "MONTHLY"
  }'
```

## 📚 API 문서

### 인증 헤더
모든 API 요청에는 사용자 식별을 위한 헤더가 필요합니다:

```bash
User-ID: {사용자의 고유 ID}
# 또는 향후 JWT 토큰 지원:
Authorization: Bearer {jwt_token}
```

### 주요 엔드포인트

#### 🔐 사용자 관리

```http
# 사용자 등록
POST /api/users
Content-Type: application/json

{
  "auth0Id": "auth0|123456789",
  "userEmail": "user@example.com"
}

# 사용자 조회
GET /api/users/auth0/{auth0Id}
GET /api/users/email/{email}
GET /api/users

# 개인 키 관리
POST /api/users/{userId}/secrets
GET /api/users/{userId}/secrets
```

#### 💳 구독 관리

```http
# 구독 플랜 조회
GET /api/subscription/plans

# 구독 생성
POST /api/subscription
{
  "planName": "PRO|ENTERPRISE",
  "billingPeriod": "MONTHLY|YEARLY"
}

# 현재 구독 조회
GET /api/subscription/current

# 구독 변경
PUT /api/subscription
{
  "stripeSubscriptionId": "sub_xxx",
  "newPlanName": "ENTERPRISE",
  "billingPeriod": "YEARLY"
}

# 구독 취소
DELETE /api/subscription
{
  "stripeSubscriptionId": "sub_xxx"
}
```

#### 📊 사용량 조회

```http
# 사용량 통계
GET /api/usage/stats

# 오늘의 사용량
GET /api/usage/today

# 플랜 기능 조회
GET /api/subscription/features
```

#### 🔔 웹훅

```http
# Stripe 웹훅 (내부용)
POST /api/webhook/stripe
Stripe-Signature: {signature}
```

### 응답 형식

모든 API는 통일된 응답 형식을 사용합니다:

```json
// 성공 응답
{
  "success": true,
  "data": { ... },
  "message": "요청이 성공적으로 처리되었습니다.",
  "timestamp": "2024-01-01T12:00:00"
}

// 실패 응답
{
  "success": false,
  "error": "SUBSCRIPTION_LIMIT_EXCEEDED",
  "message": "구독 플랜의 한도를 초과했습니다.",
  "timestamp": "2024-01-01T12:00:00"
}
```

## 📊 구독 플랜 및 제한

### 플랜 비교

| 기능 | FREE | PRO | ENTERPRISE |
|------|------|-----|------------|
| **월 요금** | ₩0 | ₩2,999 | ₩9,999 |
| **연 요금** | - | ₩29,990 | ₩99,990 |
| **API 생성 한도** | 5개 | 50개 | 무제한 |
| **분당 API 호출** | 10회 | 100회 | 1,000회 |
| **시간당 API 호출** | 100회 | 1,000회 | 10,000회 |
| **일일 API 호출** | 1,000회 | 10,000회 | 100,000회 |
| **API당 데이터 크기** | 3MB | 20MB | 무제한 |
| **우선 지원** | ❌ | ✅ | ✅ |
| **고급 분석** | ❌ | ✅ | ✅ |
| **팀 협업** | ❌ | ❌ | ✅ |

### 자동 제한 적용

구독 인터셉터가 모든 API 요청을 자동으로 검증합니다:

1. **사용자 인증 확인**
2. **구독 상태 검증** (만료 확인)
3. **Rate Limit 검사**
4. **API 사용량 한도 확인**
5. **사용량 기록**

### 제한 초과 시 동작

1. **Rate Limit 초과**: `429 Too Many Requests`
2. **API 한도 초과**: `403 Forbidden`
3. **구독 만료**: `402 Payment Required`
4. **인증 실패**: `401 Unauthorized`

## ⚡ 사용량 제한 시스템

### Redis 기반 분산 Rate Limiting

```redis
# 키 구조
rate_limit:minute:{userId}  # TTL: 1분
rate_limit:hour:{userId}    # TTL: 1시간  
rate_limit:day:{userId}     # TTL: 1일

api_count:{userId}          # TTL: 30일
usage:{userId}:{action}:{date}  # TTL: 90일
```

### 실시간 사용량 추적

모든 API 호출은 자동으로 기록되며, 플랜별 제한을 실시간으로 적용합니다.

### 멀티레벨 캐싱

```
Level 1: Redis (실시간 카운터)
Level 2: 로컬 캐시 (자주 조회되는 플랜 정보)
Level 3: 데이터베이스 (영구 저장)
```

## 🔔 웹훅 처리

### 지원하는 Stripe 이벤트

- `customer.subscription.created`: 구독 생성
- `customer.subscription.updated`: 구독 변경
- `customer.subscription.deleted`: 구독 취소
- `invoice.payment_succeeded`: 결제 성공
- `invoice.payment_failed`: 결제 실패
- `payment_method.attached`: 결제 수단 추가

### 웹훅 보안

1. **Stripe 서명 검증**: 모든 웹훅 요청의 서명 확인
2. **Idempotency**: 중복 이벤트 처리 방지
3. **에러 복구**: 실패한 웹훅 재처리 메커니즘
4. **로깅**: 모든 웹훅 이벤트 상세 기록

### 자동 구독 관리

- **결제 성공** → 구독 갱신
- **결제 실패** → 구독 일시정지
- **구독 취소** → FREE 플랜 자동 전환
- **구독 생성** → 플랜 활성화

## 📈 모니터링 및 로깅

### 구조화된 로깅

```java
// 사용자 작업 로깅
log.info("사용자 생성 완료 - userId: {}, email: {}", userId, email);

// 구독 관련 로깅  
log.info("구독 생성 - userId: {}, plan: {}, amount: {}", userId, planName, amount);

// Rate Limit 로깅
log.warn("Rate Limit 초과 - userId: {}, limit: {}, current: {}", userId, limit, current);

// Stripe 연동 로깅
log.info("Stripe 구독 생성 - subscriptionId: {}, customerId: {}", subId, customerId);
```

### 메트릭 수집

Micrometer를 통한 메트릭 수집으로 성능 모니터링이 가능합니다.

### 알림 설정

1. **Rate Limit 초과**: 5분 내 10회 이상
2. **결제 실패**: 즉시 알림
3. **시스템 오류**: 5xx 에러 5회 이상
4. **Stripe 연결 실패**: 즉시 알림

## 🧪 테스트

### 테스트 전략

```bash
# 단위 테스트 (90%+ 커버리지)
./gradlew test

# 통합 테스트
./gradlew integrationTest

# 성능 테스트
./gradlew performanceTest
```

### TDD 개발 과정

이 프로젝트는 **Test-Driven Development** 방식으로 개발되었습니다:

1. **Red**: 실패하는 테스트 작성
2. **Green**: 테스트를 통과하는 최소한의 코드 작성
3. **Refactor**: 코드 품질 개선

## 🔒 보안

### 보안 기능

1. **JWT 토큰 검증**: Auth0 기반 인증
2. **API 키 암호화**: AWS Secrets Manager 연동
3. **Rate Limiting**: DDoS 공격 방지
4. **입력 검증**: SQL Injection 및 XSS 방지
5. **HTTPS 강제**: 모든 통신 암호화

### 민감 정보 관리

```yaml
# 환경변수로 관리
stripe:
  api-key: ${STRIPE_API_KEY}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET}

aws:
  access-key: ${AWS_ACCESS_KEY}
  secret-key: ${AWS_SECRET_KEY}
```

## 🤝 기여하기

### 개발 워크플로우

1. **이슈 생성**: 기능 요청 또는 버그 리포트
2. **브랜치 생성**: `feature/feature-name` 또는 `bugfix/bug-name`
3. **TDD 개발**: 테스트 먼저 작성
4. **코드 리뷰**: Pull Request 생성
5. **배포**: 승인 후 main 브랜치 머지

### 코딩 컨벤션

```java
// 클래스명: PascalCase
public class UserService {
    
    // 메서드명: camelCase
    public User createUser(CreateUserRequest request) {
        
        // 변수명: camelCase
        String userId = generateUserId();
        
        // 상수명: UPPER_SNAKE_CASE
        private static final String DEFAULT_PLAN = "FREE";
    }
}
```

### 커밋 메시지 규칙

```
feat: 새로운 기능 추가
fix: 버그 수정
docs: 문서 수정
style: 코드 스타일 변경
refactor: 코드 리팩토링
test: 테스트 코드 추가/수정
chore: 빌드 설정 변경
```

---

## 📞 지원

- **테스트 가이드**: [TESTING_GUIDE.md](./TESTING_GUIDE.md)
- **이슈 리포트**: GitHub Issues
- **기능 요청**: GitHub Discussions

---

## 🎯 핵심 특징

### ✅ 바로 사용 가능
**Stripe 환경변수만 설정하면 완전한 구독 관리 시스템이 바로 작동합니다.**

### 🚀 엔터프라이즈급 성능
- Redis 기반 고성능 Rate Limiting
- 분산 환경 지원
- 자동 스케일링 대응

### 🔒 보안 최우선
- Auth0 기반 인증
- 암호화된 키 관리
- 웹훅 서명 검증

### 📊 완벽한 모니터링
- 구조화된 로깅
- 실시간 메트릭
- 상세한 사용량 분석

---

**🎉 BE-UserService와 함께 강력한 구독 기반 서비스를 구축하세요!**

*환경변수에 Stripe 키만 설정하면 바로 사용할 수 있는 완전한 구독 관리 시스템입니다.*