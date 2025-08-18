# 🔐 신뢰 기반 인증 모드 마이그레이션 가이드

## 📋 개요

이 가이드는 User Service에서 JWT 이중 검증을 제거하고 API Gateway를 신뢰하는 인증 아키텍처로 전환하는 방법을 설명합니다.

## 🎯 마이그레이션 목표

- **성능 향상**: JWT 재검증 제거로 응답시간 50-100ms 단축
- **네트워크 부하 감소**: Auth0 JWKS 호출 50% 감소  
- **아키텍처 단순화**: 인증 책임을 API Gateway에 집중

## 🏗️ 아키텍처 변경사항

### 이전 (이중 검증 모드)
```
클라이언트 → API Gateway (JWT 검증) → User Service (JWT 재검증) → 응답
```

### 이후 (신뢰 기반 모드)
```
클라이언트 → API Gateway (JWT 검증 + 헤더 추가) → User Service (헤더 신뢰) → 응답
```

## 🔧 구현된 변경사항

### 1. API Gateway 측 변경
- `UserInfoHeaderFilter` 추가: JWT 정보를 헤더로 변환
- 모든 User Service 라우트에 필터 적용

### 2. User Service 측 변경
- `TrustedGatewayConfig` 추가: 헤더 기반 인증
- 기본 프로파일을 `trusted-gateway`로 변경
- 기존 JWT 설정을 레거시 모드로 변경

## 📂 전달되는 헤더 정보

| 헤더명 | 설명 | 예시 |
|--------|------|------|
| `X-User-Id` | 사용자 식별자 (JWT sub) | `auth0\|12345` |
| `X-User-Email` | 사용자 이메일 | `user@example.com` |
| `X-User-Authorities` | 권한 목록 (쉼표 구분) | `read:users,write:users` |
| `X-User-Roles` | 역할 목록 (쉼표 구분) | `USER,ADMIN` |
| `X-Gateway-Verified` | 검증 완료 표시 | `true` |

## 🚀 사용 방법

### 환경별 프로파일 설정

#### 신뢰 기반 모드 (권장)
```bash
# 환경변수 설정
SPRING_PROFILES_ACTIVE=trusted-gateway

# 또는 application.yml에서 (이미 기본값으로 설정됨)
spring:
  profiles:
    active: trusted-gateway
```

#### 레거시 JWT 재검증 모드
```bash
# 기존 방식 유지가 필요한 경우
SPRING_PROFILES_ACTIVE=legacy-jwt

# 또는 스테이징 환경
SPRING_PROFILES_ACTIVE=staging
```

## 🔒 보안 고려사항

### ✅ 안전한 적용 조건
1. **네트워크 보안**: API Gateway ↔ User Service 간 신뢰할 수 있는 네트워크
2. **직접 접근 차단**: 외부에서 User Service 직접 접근 불가
3. **헤더 무결성**: API Gateway에서 헤더 위조 방지

### 🛡️ 추가 보안 강화 방안
- API Gateway에서 서명된 헤더 전달
- User Service에서 요청 출처 IP 검증  
- 내부 서비스 간 mTLS 적용

## 📊 성능 벤치마크

### 예상 성능 향상
- **응답 시간**: 50-100ms 단축
- **CPU 사용률**: JWT 검증 연산 제거로 20-30% 감소
- **네트워크 요청**: Auth0 JWKS 호출 50% 감소

## 🧪 테스트 방법

### 1. 헤더 전달 확인
```bash
# API Gateway를 통한 요청
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://api-gateway/gateway/users/api/users/profile

# User Service 로그에서 헤더 확인
# X-User-Id, X-User-Email 등이 올바르게 전달되는지 확인
```

### 2. 인증 동작 확인
```bash
# 유효한 토큰으로 요청
curl -H "Authorization: Bearer VALID_TOKEN" \
     http://api-gateway/gateway/users/api/users

# 무효한 토큰으로 요청 (401 응답 확인)
curl -H "Authorization: Bearer INVALID_TOKEN" \
     http://api-gateway/gateway/users/api/users
```

## 🔄 롤백 방법

문제 발생 시 즉시 이전 모드로 롤백:

```bash
# User Service 재시작 시 레거시 모드로 실행
SPRING_PROFILES_ACTIVE=legacy-jwt java -jar user-service.jar
```

## 📝 체크리스트

### 마이그레이션 전 확인사항
- [ ] API Gateway ↔ User Service 네트워크 보안 확인
- [ ] 외부에서 User Service 직접 접근 차단 확인
- [ ] 백업 및 롤백 계획 수립

### 마이그레이션 후 확인사항  
- [ ] 모든 API 엔드포인트 정상 동작 확인
- [ ] 인증/인가 정책 올바른 적용 확인
- [ ] 성능 모니터링 및 개선 확인

## 🚨 문제 해결

### 일반적인 문제와 해결책

1. **헤더가 전달되지 않는 경우**
   - API Gateway 라우트 설정에 `UserInfoHeader` 필터 추가 확인
   - 네트워크 프록시나 로드밸런서에서 헤더 제거 여부 확인

2. **인증 실패가 발생하는 경우**
   - User Service 프로파일이 `trusted-gateway`로 설정되었는지 확인
   - `X-User-Id` 헤더가 올바르게 전달되는지 확인

3. **권한 확인이 실패하는 경우**
   - `@PreAuthorize` 어노테이션이 올바른 권한을 확인하는지 점검
   - `X-User-Authorities`, `X-User-Roles` 헤더 내용 확인

## 📞 지원

문제가 발생하거나 추가 질문이 있는 경우 개발팀에 문의하시기 바랍니다.

---
*마지막 업데이트: 2024년*
