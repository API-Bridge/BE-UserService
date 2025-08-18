# 🔓 권한 검증 없는 테스트 모드 가이드

## 📋 개요

403 에러 해결을 위해 모든 권한 검증을 일시적으로 비활성화했습니다.  
이제 JWT 토큰만 있으면 모든 API에 접근할 수 있습니다.

## ✅ 적용된 변경사항

### 1. API Gateway
- ✅ 모든 경로 `permitAll()` (이미 설정됨)
- ✅ UserInfoHeader 필터 활성화

### 2. User Service  
- ✅ TrustedGatewayConfig에서 모든 경로 `permitAll()`
- ✅ UserController의 모든 `@PreAuthorize` 어노테이션 비활성화

## 🧪 테스트 방법

### 1. 기본 테스트 (권한 없이)
```bash
# 유효한 JWT 토큰으로 요청
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/gateway/users/api/users

# 응답: 200 OK (이제 403 에러가 나지 않아야 함)
```

### 2. 헤더 전달 확인
```bash
# User Service에서 전달받은 헤더 확인
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     -H "X-Debug: true" \
     http://localhost:8080/gateway/users/api/users

# User Service 로그에서 다음 헤더들이 전달되는지 확인:
# - X-User-Id
# - X-User-Email  
# - X-User-Authorities
# - X-Gateway-Verified
```

### 3. 신뢰 기반 모드 확인
```bash
# User Service가 trusted-gateway 프로파일로 실행되는지 확인
# 로그에서 다음 메시지 확인:
# "Active profiles: trusted-gateway"
```

## 🔧 현재 인증 흐름

```
클라이언트 (JWT 토큰) 
    ↓
API Gateway (JWT 검증 + 헤더 추가)
    ↓  
User Service (헤더 신뢰, 권한 검증 없음)
    ↓
응답 반환
```

## 🚨 중요 사항

### ⚠️ 보안 주의사항
- **개발/테스트 환경에서만 사용**
- 프로덕션에서는 반드시 권한 검증 활성화 필요
- 외부에서 User Service 직접 접근 차단 필수

### 🔄 원래 권한 검증으로 복구 방법

권한 검증을 다시 활성화하려면:

1. **UserController 복구**:
```java
// 주석 제거
@PreAuthorize("hasRole('USER')")
```

2. **TrustedGatewayConfig 복구**:
```java
.requestMatchers("/api/**").authenticated()
.requestMatchers("/admin/**").hasRole("ADMIN")
```

3. **프로파일 변경**:
```bash
# 레거시 JWT 검증 모드로 복구
SPRING_PROFILES_ACTIVE=legacy-jwt
```

## 📊 예상 테스트 결과

### ✅ 성공 시나리오
- 모든 API 엔드포인트에서 200 OK 응답
- User Service 로그에서 헤더 수신 확인
- JWT 토큰 정보가 헤더로 올바르게 전달

### ❌ 여전히 문제가 있다면
1. **JWT 토큰 자체가 유효하지 않음**
   - Auth0에서 토큰 재발급 필요
   - audience, issuer 설정 확인

2. **서비스 간 네트워크 문제**  
   - API Gateway → User Service 연결 확인
   - 포트 및 서비스 디스커버리 확인

3. **설정 충돌**
   - 기존 레거시 설정과 충돌 가능성
   - 애플리케이션 재시작 필요

## 🎯 다음 단계

1. **테스트 성공 시**: 점진적으로 권한 검증 재활성화
2. **테스트 실패 시**: JWT 토큰 및 네트워크 설정 점검
3. **안정화 후**: 프로덕션에 맞는 보안 정책 재구성

---
*현재 상태: 모든 권한 검증 비활성화 (테스트 모드)*
