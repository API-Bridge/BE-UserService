# 🚀 BE-UserService

**Auth0 인증, Stripe 결제, 사용량 제한 기능을 갖춘 엔터프라이즈급 사용자 서비스**

Spring Boot 3.4.3 기반 마이크로서비스로 JWT 인증, 구독 관리, API 사용량 제한, 커스텀 API 공유 기능을 제공합니다.

## 📋 목차

- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [빠른 시작](#-빠른-시작)
- [API 문서](#-api-문서)
- [인증 방식](#-인증-방식)
- [환경 설정](#-환경-설정)
- [구독 플랜 시스템](#-구독-플랜-시스템)
- [배포 가이드](#-배포-가이드)
- [모니터링](#-모니터링)

## ✨ 주요 기능

### 🔐 사용자 관리
- **Auth0 JWT 토큰 인증** - 다양한 OAuth 제공자 지원
- **사용자 CRUD 연산** - 생성, 조회, 구독 정보 관리
- **BYOK (Bring Your Own Key)** - AWS Secrets Manager 기반 개인 키 관리
- **플랜별 사용량 제한** - FREE/PRO/ENTERPRISE 플랜별 제한

### 💳 Stripe 구독 관리
- **결제 세션 생성** - Stripe Checkout 연동
- **구독 상태 관리** - 활성/취소/갱신 처리
- **웹훅 처리** - 결제 완료 후 자동 구독 업데이트
- **다중 플랜 지원** - 월간/연간 결제 옵션

### 📊 커스텀 API 및 공유 시스템
- **커스텀 API 관리** - 사용자별 API 생성 및 관리
- **공유 API 마켓플레이스** - API 공유 및 저장 기능
- **사용량 통계** - 분/시간/일/월별 API 호출 통계
- **플랜별 제한** - API 개수, 호출량 제한

## 🛠 기술 스택

### Backend
- **Spring Boot 3.4.3** - Java 17 (Amazon Corretto)
- **Spring Security + OAuth2** - JWT 토큰 기반 인증
- **Spring Data JPA** - 데이터베이스 ORM
- **MySQL 8.0** - 메인 데이터베이스
- **Redis** - 캐싱 및 세션 관리

### 외부 서비스
- **Auth0** - 인증 및 사용자 관리
- **Stripe** - 결제 및 구독 관리
- **Apache Kafka** - 이벤트 기반 메시징
- **AWS Secrets Manager** - 키 관리

### 모니터링
- **Micrometer + Prometheus** - 메트릭 수집
- **Spring Boot Actuator** - 헬스 체크
- **Structured Logging** - JSON 로깅 및 보안 감사

## 🚀 빠른 시작

### 1. 필수 요구사항
- Java 17 (Amazon Corretto 권장)
- MySQL 8.0
- Redis (선택사항)
- Docker & Docker Compose (선택사항)

### 2. 로컬 환경 설정

```bash
# 레포지토리 클론
git clone https://github.com/your-org/BE-UserService.git
cd BE-UserService

# 환경변수 설정
cp .env.example .env
# .env 파일의 값들을 실제 환경에 맞게 수정

# MySQL 데이터베이스 생성
mysql -u root -p
CREATE DATABASE userservice;
```

### 3. 애플리케이션 실행

**개발 환경:**
```bash
./gradlew bootRun
```

**Docker Compose 사용:**
```bash
docker-compose up -d  # 외부 의존성 (MySQL, Redis, Kafka) 시작
./gradlew bootRun     # 애플리케이션 실행
```

### 4. 접속 확인
- **애플리케이션**: http://localhost:8081
- **Swagger UI**: http://localhost:8081/swagger-ui/index.html
- **헬스 체크**: http://localhost:8081/api/health

## 📚 API 문서

### 🎯 주요 엔드포인트 그룹

#### 👤 User Management
- `POST /api/users` - 사용자 생성
- `GET /api/users/{userId}` - 사용자 조회
- `GET /api/users/info` - 통합 사용자 정보 (X-User-Id 헤더 필요)
- `POST /api/users/{userId}/secrets` - 개인 키 등록 (BYOK)
- `GET /api/users/{userId}/secrets` - 개인 키 목록
- `GET /api/secrets/{userId}/arn` - 개인 키 조회 (AI 서비스용)

#### 💰 Subscription & Usage
- `POST /api/subscription/checkout` - Stripe 결제 세션 생성
- `POST /api/subscription/update-after-payment` - 결제 완료 후 구독 업데이트
- `GET /api/subscription/products` - Stripe 상품 정보
- `GET /api/users/{userId}/subscription` - 사용자 구독 정보
- `POST /api/users/{userId}/subscription/cancel` - 구독 취소
- `GET /api/users/{userId}/usage` - API 사용량 조회

#### 🎛 Plan Management
- `GET /api/plan/limits` - 플랜 제한사항 조회 (X-User-Id 헤더 필요)
- `GET /api/plan/features` - 플랜 기능 조회
- `GET /api/plan/usage-stats` - 사용량 통계
- `GET /api/plan/limits-status` - 제한 상태 조회

#### 🔧 Custom API Management
- `GET /api/custom-apis/my` - 내 커스텀 API 목록 (X-User-Id 헤더 필요)
- `GET /api/custom-apis` - 커스텀 API 목록 (페이징)
- `GET /api/custom-apis/{customApiId}` - 커스텀 API 상세
- `GET /api/custom-apis/search` - 커스텀 API 검색

#### 🌐 Shared API Management
- `GET /api/shared-apis` - 공유 API 목록 (공개)
- `GET /api/shared-apis/search` - 공유 API 검색
- `POST /api/users/{userId}/shared-apis/share` - API 공유 게시
- `POST /api/users/{userId}/shared-apis/save` - 공유 API 저장
- `GET /api/users/{userId}/shared-apis/saved` - 저장된 API 목록
- `DELETE /api/users/{userId}/shared-apis/unshare` - API 공유 취소

#### ⚕️ Health & Admin
- `GET /api/health` - 서비스 상태 확인
- `GET /actuator/health` - Spring Actuator 헬스 체크
- `GET /actuator/prometheus` - Prometheus 메트릭
- `POST /api/admin/migrate/free-subscriptions` - 무료 구독 자동 생성

#### 🎣 Webhook & Test
- `POST /webhook` - Stripe 웹훅 처리
- `GET /users/test-headers` - 헤더 테스트
- `GET /test/api/**` - 통합 테스트 API들

## 🔐 인증 방식

### 1. JWT Bearer Token (운영 환경)
```bash
curl -H "Authorization: Bearer <JWT_TOKEN>" \
     http://localhost:8081/api/users/123
```

### 2. X-User-Id Header (Gateway 연동)
```bash
curl -H "X-User-Id: user-123" \
     http://localhost:8081/api/users/info
```

### 3. 공개 엔드포인트 (인증 불필요)
- `/api/health`
- `/api/shared-apis`
- `/api/subscription/products`
- `/actuator/health`

## ⚙️ 환경 설정

### 환경 변수 (.env 파일)

```env
# Spring Configuration
SPRING_PROFILES_ACTIVE=dev
DDL_AUTO=update

# Database
DB_URL=jdbc:mysql://localhost:3306/userservice
DB_USERNAME=root
DB_PASSWORD=your_password

# Auth0
AUTH0_ISSUER_URI=https://your-domain.auth0.com/
AUTH0_AUDIENCE=https://api.your-domain.com
AUTH0_CLIENT_ID=your_client_id

# Stripe
STRIPE_SECRET_KEY=sk_test_...
STRIPE_PUBLIC_KEY=pk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_FREE_PRODUCT_ID=prod_...
STRIPE_PRO_PRODUCT_ID=prod_...

# Redis (Optional)
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka (Optional)
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

### 프로파일별 설정

#### Development (`dev`)
- MySQL 데이터베이스 사용
- 상세한 디버그 로깅
- H2 콘솔 활성화
- 개발 도구 자동 재시작

#### Production (`prod`)
- MySQL 최적화된 커넥션 풀
- 로깅 레벨 최소화
- Redis 캐싱 활성화
- 성능 메트릭 수집

#### Staging (`staging`)
- 프로덕션과 유사하지만 더 관대한 설정
- 디버그 로깅 부분 활성화

## 💼 구독 플랜 시스템

### 플랜 비교

| 기능 | FREE | PRO |
|------|------|-----|
| 커스텀 API 개수 | 5개 | 무제한 |
| 공유 API 개수 | 3개 | 무제한 |
| API 호출 (월간) | 30,000회 | 무제한 |
| 분당 호출 제한 | 10회 | 100회 |
| 시간당 호출 제한 | 100회 | 10,000회 |
| 일일 호출 제한 | 1,000회 | 100,000회 |
| 개인 키 관리 | ✅ | ✅ |
| 우선 지원 | ❌ | ✅ |

### 구독 플로우

1. **결제 세션 생성**: `POST /api/subscription/checkout`
2. **Stripe 결제 페이지로 리다이렉트**
3. **결제 완료 후 웹훅 처리**: `POST /webhook`
4. **구독 활성화**: `POST /api/subscription/update-after-payment`

## 📦 배포 가이드

### Docker 배포

```bash
# 이미지 빌드
docker build -t userservice:latest .

# 컨테이너 실행
docker run -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:mysql://mysql:3306/userservice \
  -e DB_USERNAME=userservice \
  -e DB_PASSWORD=password \
  userservice:latest
```

### Kubernetes 배포 (Helm)

```bash
# Helm 차트 설치
helm install userservice ./helm/userservice \
  --set image.tag=latest \
  --set env.DB_USERNAME=userservice \
  --set-string secrets.database.enabled=true
```

### 환경별 배포 스크립트

```bash
# 개발 환경
./gradlew bootRun --args='--spring.profiles.active=dev'

# 스테이징 환경
./gradlew bootJar
java -jar build/libs/*.jar --spring.profiles.active=staging

# 프로덕션 환경
docker-compose -f docker-compose.prod.yml up -d
```

## 📊 모니터링

### 메트릭 엔드포인트
- **Prometheus**: `/actuator/prometheus`
- **헬스 체크**: `/actuator/health`
- **애플리케이션 정보**: `/actuator/info`

### 주요 메트릭
- `userservice_users_created_total` - 생성된 사용자 수
- `userservice_api_calls_total` - API 호출 횟수
- `userservice_subscriptions_active` - 활성 구독 수
- `userservice_plan_usage_ratio` - 플랜별 사용률

### 로그 모니터링
- **구조화된 JSON 로깅**
- **보안 감사 로그** - 인증, 권한 부여 이벤트
- **사용자 활동 로그** - API 키 등록, 구독 변경
- **성능 로그** - 응답 시간, 처리량

## 🤝 기여하기

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 라이선스

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🆘 지원

- **이슈 신고**: [GitHub Issues](https://github.com/your-org/BE-UserService/issues)
- **개발팀 연락**: dev@yourcompany.com
- **API 문서**: http://localhost:8081/swagger-ui/index.html