# 🧪 상세 테스트 시나리오

## 📋 테스트 데이터 준비

### 기본 테스트 사용자
```json
{
  "user-001": {"auth0Id": "auth0|6894dba797884cb2154bc2729", "email": "testuser1@example.com"},
  "user-002": {"auth0Id": "auth0|test_user_2", "email": "testuser2@example.com"},
  "user-003": {"auth0Id": "auth0|test_admin", "email": "admin@example.com"},
  "user-004": {"auth0Id": "auth0|test_developer", "email": "developer@example.com"},
  "user-005": {"auth0Id": "auth0|test_premium", "email": "premium@example.com"}
}
```

---

## 🔧 1. 사용자 관리 상세 테스트

### 1.1 사용자 생성 테스트 케이스

#### ✅ 정상 케이스
```bash
# TC-USER-001: 유효한 사용자 생성
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "auth0Id": "auth0|new_user_123",
    "userEmail": "newuser@example.com"
  }'

# 검증포인트:
# - 응답 코드: 201 Created
# - userId가 UUID 형식으로 생성됨
# - createdAt이 현재 시간으로 설정됨
# - 기본 무료 구독이 자동 생성됨
```

#### ❌ 오류 케이스
```bash
# TC-USER-002: Auth0 ID 누락
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "userEmail": "newuser@example.com"
  }'
# 기대결과: 400 Bad Request, "Auth0 ID는 필수입니다."

# TC-USER-003: 이메일 누락
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "auth0Id": "auth0|new_user_123"
  }'
# 기대결과: 400 Bad Request, "이메일은 필수입니다."

# TC-USER-004: 잘못된 이메일 형식
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "auth0Id": "auth0|new_user_123",
    "userEmail": "invalid-email"
  }'
# 기대결과: 400 Bad Request, "올바른 이메일 형식이 아닙니다."

# TC-USER-005: 중복 Auth0 ID
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "auth0Id": "auth0|6894dba797884cb2154bc2729",
    "userEmail": "duplicate@example.com"
  }'
# 기대결과: 400 Bad Request, 중복 사용자 오류
```

### 1.2 사용자 조회 테스트 케이스

```bash
# TC-USER-006: 유효한 사용자 조회
curl -X GET "http://localhost:8080/api/users/user-001"
# 기대결과: 200 OK, 사용자 정보 반환

# TC-USER-007: 존재하지 않는 사용자 조회
curl -X GET "http://localhost:8080/api/users/non-existent-user"
# 기대결과: 404 Not Found, "사용자를 찾을 수 없습니다."

# TC-USER-008: 잘못된 UUID 형식
curl -X GET "http://localhost:8080/api/users/invalid-uuid"
# 기대결과: 400 Bad Request, "올바른 UUID 형식이 아닙니다."
```

---

## 🔑 2. 개인키 관리 (BYOK) 상세 테스트

### 2.1 개인키 등록 테스트

#### ✅ 정상 케이스
```bash
# TC-SECRET-001: OpenAI API 키 등록
curl -X POST "http://localhost:8080/api/users/user-002/secrets" \
  -H "Content-Type: application/json" \
  -d '{
    "secretName": "openai-api-key",
    "secretValue": "sk-1234567890abcdef1234567890abcdef1234567890abcdef",
    "description": "OpenAI GPT API Key"
  }'

# TC-SECRET-002: Claude API 키 등록
curl -X POST "http://localhost:8080/api/users/user-003/secrets" \
  -H "Content-Type: application/json" \
  -d '{
    "secretName": "claude-api-key",
    "secretValue": "sk-ant-api03-abcdef1234567890",
    "description": "Anthropic Claude API Key"
  }'
```

#### ❌ 오류 케이스
```bash
# TC-SECRET-003: 시크릿 이름 누락
curl -X POST "http://localhost:8080/api/users/user-002/secrets" \
  -H "Content-Type: application/json" \
  -d '{
    "secretValue": "sk-1234567890abcdef",
    "description": "Test API Key"
  }'

# TC-SECRET-004: 시크릿 값 누락
curl -X POST "http://localhost:8080/api/users/user-002/secrets" \
  -H "Content-Type: application/json" \
  -d '{
    "secretName": "test-key",
    "description": "Test API Key"
  }'

# TC-SECRET-005: 존재하지 않는 사용자
curl -X POST "http://localhost:8080/api/users/non-existent/secrets" \
  -H "Content-Type: application/json" \
  -d '{
    "secretName": "test-key",
    "secretValue": "sk-1234567890abcdef"
  }'
```

### 2.2 개인키 조회 테스트

```bash
# TC-SECRET-006: 유효한 키 조회
curl -X GET "http://localhost:8080/api/secrets/user-002/arn?arnId=arn-001"

# TC-SECRET-007: 존재하지 않는 ARN
curl -X GET "http://localhost:8080/api/secrets/user-002/arn?arnId=non-existent"

# TC-SECRET-008: 다른 사용자의 키 접근 시도
curl -X GET "http://localhost:8080/api/secrets/user-001/arn?arnId=arn-001"
# user-002의 키를 user-001이 조회 시도

# TC-SECRET-009: 사용자 키 목록 조회
curl -X GET "http://localhost:8080/api/users/user-002/secrets"
```

---

## 💳 3. 구독 관리 상세 테스트

### 3.1 구독 조회 및 관리

```bash
# TC-SUB-001: 기본 무료 구독 확인
curl -X GET "http://localhost:8080/api/users/user-001/subscription"
# 기대결과: Free 플랜, isActive: true

# TC-SUB-002: Pro 플랜 변경
curl -X PUT "http://localhost:8080/api/users/user-001/subscription?planName=Pro"
# 검증: 플랜 변경 후 구독 정보 다시 조회하여 확인

# TC-SUB-003: 잘못된 플랜명으로 변경 시도
curl -X PUT "http://localhost:8080/api/users/user-001/subscription?planName=InvalidPlan"
# 기대결과: 400 Bad Request
```

### 3.2 사용량 조회 테스트

```bash
# TC-USAGE-001: Free 플랜 사용량 조회
curl -X GET "http://localhost:8080/api/users/user-001/usage"
# 검증포인트:
# - rateLimitPerMinute: 10
# - rateLimitPerHour: 100
# - rateLimitPerDay: 1000

# TC-USAGE-002: Pro 플랜 사용량 조회
curl -X GET "http://localhost:8080/api/users/user-002/usage"
# 검증포인트:
# - rateLimitPerMinute: 200
# - rateLimitPerHour: 5000
# - rateLimitPerDay: 50000
```

### 3.3 Stripe 결제 테스트

```bash
# TC-STRIPE-001: 유효한 결제 세션 생성
curl -X POST "http://localhost:8080/api/subscription/checkout" \
  -H "Content-Type: application/json" \
  -d '{
    "priceId": "price_1234567890",
    "successUrl": "http://localhost:3000/success",
    "cancelUrl": "http://localhost:3000/cancel"
  }'

# TC-STRIPE-002: Price ID 누락
curl -X POST "http://localhost:8080/api/subscription/checkout" \
  -H "Content-Type: application/json" \
  -d '{
    "successUrl": "http://localhost:3000/success"
  }'

# TC-STRIPE-003: 프로덕트 정보 조회
curl -X GET "http://localhost:8080/api/subscription/products"
```

---

## 🔄 4. 공유 API 기능 상세 테스트

### 4.1 API 공유 테스트

```bash
# TC-SHARE-001: 유효한 API 공유
curl -X POST "http://localhost:8080/api/shared-apis/share" \
  -H "X-User-Id: user-003" \
  -H "Content-Type: application/json" \
  -d 'customApiId=api-001&planName=FREE'

# TC-SHARE-002: 존재하지 않는 API 공유 시도
curl -X POST "http://localhost:8080/api/shared-apis/share" \
  -H "X-User-Id: user-003" \
  -H "Content-Type: application/json" \
  -d 'customApiId=non-existent&planName=FREE'

# TC-SHARE-003: 다른 사용자의 API 공유 시도
curl -X POST "http://localhost:8080/api/shared-apis/share" \
  -H "X-User-Id: user-001" \
  -H "Content-Type: application/json" \
  -d 'customApiId=api-001&planName=FREE'
# api-001은 user-003 소유

# TC-SHARE-004: 이미 공유된 API 재공유 시도
curl -X POST "http://localhost:8080/api/shared-apis/share" \
  -H "X-User-Id: user-003" \
  -H "Content-Type: application/json" \
  -d 'customApiId=api-001&planName=FREE'
# 동일한 API를 두 번 공유 시도
```

### 4.2 공유 API 조회 테스트

```bash
# TC-SHARE-005: 공유 API 목록 조회 (페이징)
curl -X GET "http://localhost:8080/api/shared-apis?page=0&size=10"

# TC-SHARE-006: 페이지 범위 초과
curl -X GET "http://localhost:8080/api/shared-apis?page=999&size=10"

# TC-SHARE-007: 특정 사용자의 공유 API 조회
curl -X GET "http://localhost:8080/api/shared-apis/creator/user-003"

# TC-SHARE-008: 존재하지 않는 사용자의 공유 API 조회
curl -X GET "http://localhost:8080/api/shared-apis/creator/non-existent"

# TC-SHARE-009: API 검색 기능
curl -X GET "http://localhost:8080/api/shared-apis/search?keyword=weather"
curl -X GET "http://localhost:8080/api/shared-apis/search?keyword=news"
curl -X GET "http://localhost:8080/api/shared-apis/search?keyword=nonexistent"
```

### 4.3 API 저장 및 관리 테스트

```bash
# TC-SAVE-001: 공유 API 저장
curl -X POST "http://localhost:8080/api/shared-apis/shared-001/save" \
  -H "X-User-Id: user-001"

# TC-SAVE-002: 이미 저장된 API 재저장 시도
curl -X POST "http://localhost:8080/api/shared-apis/shared-001/save" \
  -H "X-User-Id: user-001"

# TC-SAVE-003: 존재하지 않는 공유 API 저장 시도
curl -X POST "http://localhost:8080/api/shared-apis/non-existent/save" \
  -H "X-User-Id: user-001"

# TC-SAVE-004: 사용자 저장 API 목록 조회
curl -X GET "http://localhost:8080/api/shared-apis/saved" \
  -H "X-User-Id: user-001"

# TC-SAVE-005: 저장된 API 삭제
curl -X DELETE "http://localhost:8080/api/shared-apis/saved/saved-001" \
  -H "X-User-Id: user-001"

# TC-SAVE-006: 다른 사용자의 저장 API 삭제 시도
curl -X DELETE "http://localhost:8080/api/shared-apis/saved/saved-002" \
  -H "X-User-Id: user-001"
# saved-002는 user-002 소유
```

### 4.4 공유 취소 테스트

```bash
# TC-UNSHARE-001: 유효한 공유 취소
curl -X DELETE "http://localhost:8080/api/shared-apis/unshare" \
  -H "X-User-Id: user-003" \
  -d 'customApiId=api-001'

# TC-UNSHARE-002: 다른 사용자의 공유 취소 시도
curl -X DELETE "http://localhost:8080/api/shared-apis/unshare" \
  -H "X-User-Id: user-001" \
  -d 'customApiId=api-001'

# TC-UNSHARE-003: 공유되지 않은 API 취소 시도
curl -X DELETE "http://localhost:8080/api/shared-apis/unshare" \
  -H "X-User-Id: user-003" \
  -d 'customApiId=api-005'
```

---

## 🔄 5. 통합 시나리오 테스트

### 5.1 신규 사용자 온보딩 플로우
```bash
# Step 1: 사용자 생성
NEW_USER=$(curl -s -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "auth0Id": "auth0|integration_test_001",
    "userEmail": "integration@example.com"
  }' | jq -r '.data.userId')

# Step 2: 구독 정보 확인 (기본 Free 플랜)
curl -X GET "http://localhost:8080/api/users/$NEW_USER/subscription"

# Step 3: 사용량 확인
curl -X GET "http://localhost:8080/api/users/$NEW_USER/usage"

# Step 4: 개인키 등록
curl -X POST "http://localhost:8080/api/users/$NEW_USER/secrets" \
  -H "Content-Type: application/json" \
  -d '{
    "secretName": "integration-test-key",
    "secretValue": "sk-integration123456789",
    "description": "Integration test API key"
  }'

# Step 5: 키 목록 확인
curl -X GET "http://localhost:8080/api/users/$NEW_USER/secrets"
```

### 5.2 API 공유 생태계 플로우
```bash
# Step 1: 사용자 A가 API 공유
curl -X POST "http://localhost:8080/api/shared-apis/share" \
  -H "X-User-Id: user-003" \
  -d 'customApiId=api-002&planName=FREE'

# Step 2: 공유 목록에서 확인
curl -X GET "http://localhost:8080/api/shared-apis?page=0&size=10"

# Step 3: 사용자 B가 검색으로 발견
curl -X GET "http://localhost:8080/api/shared-apis/search?keyword=news"

# Step 4: 사용자 B가 저장
curl -X POST "http://localhost:8080/api/shared-apis/shared-002/save" \
  -H "X-User-Id: user-001"

# Step 5: 사용자 B의 저장 목록 확인
curl -X GET "http://localhost:8080/api/shared-apis/saved" \
  -H "X-User-Id: user-001"

# Step 6: 사용자 A가 공유 취소
curl -X DELETE "http://localhost:8080/api/shared-apis/unshare" \
  -H "X-User-Id: user-003" \
  -d 'customApiId=api-002'

# Step 7: 공유 목록에서 제거됐는지 확인
curl -X GET "http://localhost:8080/api/shared-apis?page=0&size=10"
```

### 5.3 구독 업그레이드 플로우
```bash
# Step 1: 현재 Free 플랜 확인
curl -X GET "http://localhost:8080/api/users/user-001/subscription"

# Step 2: 현재 사용량 한도 확인
curl -X GET "http://localhost:8080/api/users/user-001/usage"

# Step 3: Stripe 결제 세션 생성
curl -X POST "http://localhost:8080/api/subscription/checkout" \
  -H "Content-Type: application/json" \
  -d '{
    "priceId": "price_pro_monthly",
    "successUrl": "http://localhost:3000/success",
    "cancelUrl": "http://localhost:3000/cancel"
  }'

# Step 4: Pro 플랜으로 변경 (결제 완료 후)
curl -X PUT "http://localhost:8080/api/users/user-001/subscription?planName=Pro"

# Step 5: 변경된 구독 정보 확인
curl -X GET "http://localhost:8080/api/users/user-001/subscription"

# Step 6: 증가된 사용량 한도 확인
curl -X GET "http://localhost:8080/api/users/user-001/usage"
```

---

## 📊 6. 성능 테스트 시나리오

### 6.1 부하 테스트
```bash
# 동시 사용자 구독 조회 (Apache Bench)
ab -n 1000 -c 50 -H "X-User-Id: user-001" \
   http://localhost:8080/api/users/user-001/subscription

# 공유 API 목록 조회 부하 테스트
ab -n 500 -c 25 \
   http://localhost:8080/api/shared-apis?page=0&size=20
```

### 6.2 스트레스 테스트
```bash
# 연속적인 사용자 생성 테스트
for i in {1..100}; do
  curl -X POST "http://localhost:8080/api/users" \
    -H "Content-Type: application/json" \
    -d "{
      \"auth0Id\": \"auth0|stress_test_$i\",
      \"userEmail\": \"stress$i@example.com\"
    }" &
done
wait
```

---

## ⚡ 7. 자동화 테스트 스크립트

### Bash 스크립트 예시
```bash
#!/bin/bash
# integration-test.sh

BASE_URL="http://localhost:8080"
TOTAL_TESTS=0
PASSED_TESTS=0

function test_api() {
    local name="$1"
    local expected_code="$2"
    local curl_command="$3"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo "Testing: $name"
    
    response_code=$(eval "$curl_command" -w "%{http_code}" -o /dev/null -s)
    
    if [ "$response_code" -eq "$expected_code" ]; then
        echo "✅ PASS: $name (got $response_code)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo "❌ FAIL: $name (expected $expected_code, got $response_code)"
    fi
}

# 테스트 실행
test_api "Health Check" 200 "curl -X GET $BASE_URL/health"
test_api "User Creation" 201 "curl -X POST $BASE_URL/api/users -H 'Content-Type: application/json' -d '{\"auth0Id\":\"auth0|test123\",\"userEmail\":\"test@example.com\"}'"

echo ""
echo "Test Results: $PASSED_TESTS/$TOTAL_TESTS passed"
```

---

이 상세한 테스트 시나리오를 통해 모든 기능을 체계적으로 검증할 수 있습니다! 🎯