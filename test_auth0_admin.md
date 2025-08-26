# Auth0 관리자 권한 테스트 가이드

## 1. Auth0 토큰 발급 테스트

### 1.1 Auth0 Management API에서 테스트 토큰 생성
```bash
# Auth0 Management API 토큰 발급
curl -X POST https://api-bridge.us.auth0.com/oauth/token \
  -H 'Content-Type: application/json' \
  -d '{
    "client_id": "YOUR_CLIENT_ID",
    "client_secret": "YOUR_CLIENT_SECRET",
    "audience": "https://api.api-bridge.com",
    "grant_type": "client_credentials"
  }'
```

### 1.2 수동 JWT 토큰 생성 (개발용)
JWT.io에서 다음 페이로드로 테스트 토큰 생성:

```json
{
  "sub": "auth0|testadmin001",
  "email": "admin@api-bridge.com",
  "iss": "https://api-bridge.us.auth0.com/",
  "aud": "https://api.api-bridge.com",
  "https://yourapp.com/roles": ["user", "admin"],
  "https://yourapp.com/permissions": ["read:profile", "read:all_users", "write:all_users"],
  "exp": 9999999999
}
```

## 2. API 테스트 시나리오

### 2.1 일반 사용자 API (권한 불필요)
```bash
# 사용자 정보 조회 (일반 사용자 가능)
curl -X GET "http://localhost:8080/gateway/users/user-001" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 2.2 관리자 전용 API 테스트
```bash
# 1. 전체 사용자 목록 조회 (관리자만 가능)
curl -X GET "http://localhost:8080/gateway/users/admin/users" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"

# 2. 사용자 상세 정보 조회 (관리자만 가능)  
curl -X GET "http://localhost:8080/gateway/users/admin/users/user-001" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"

# 3. 시스템 통계 조회 (관리자만 가능)
curl -X GET "http://localhost:8080/gateway/users/admin/statistics" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"

# 4. API 관리 기능 (관리자만 가능)
curl -X GET "http://localhost:8080/gateway/apimgmt/admin/apis" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"

# 5. 모니터링 데이터 (관리자만 가능)
curl -X GET "http://localhost:8080/gateway/monitoring/metrics" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

### 2.3 권한 없는 접근 테스트 (403 Forbidden 예상)
```bash
# 일반 사용자 토큰으로 관리자 API 접근
curl -X GET "http://localhost:8080/gateway/users/admin/users" \
  -H "Authorization: Bearer USER_ONLY_JWT_TOKEN"
```

## 3. 로컬 개발 환경 테스트

### 3.1 TestJwtConfig 활용 (개발용)
`application-test.yml`에 다음 설정 추가:

```yaml
test:
  jwt:
    enabled: true
    admin-user-id: "test-admin-001"
    admin-email: "admin@api-bridge.com"
```

### 3.2 통합 테스트 클래스 작성
```java
@SpringBootTest
@TestPropertySource(properties = {
    "test.jwt.enabled=true"
})
class AdminAuthorizationIntegrationTest {
    
    @Test
    void testAdminOnlyEndpoint() {
        // 관리자 토큰으로 테스트
        String adminToken = createTestAdminToken();
        
        webTestClient.get()
            .uri("/api/admin/users")
            .header("Authorization", "Bearer " + adminToken)
            .exchange()
            .expectStatus().isOk();
    }
    
    @Test
    void testAccessDeniedForNonAdmin() {
        // 일반 사용자 토큰으로 테스트
        String userToken = createTestUserToken();
        
        webTestClient.get()
            .uri("/api/admin/users")
            .header("Authorization", "Bearer " + userToken)
            .exchange()
            .expectStatus().isForbidden();
    }
}
```

## 4. 실제 브라우저 테스트

### 4.1 Auth0 Universal Login 사용
1. `http://localhost:8080/oauth2/authorization/auth0` 접속
2. Auth0 로그인 화면에서 관리자 계정으로 로그인
3. 로그인 후 JWT 토큰 확인
4. 관리자 API 호출 테스트

### 4.2 JWT 토큰 디코딩 확인
브라우저 개발자 도구에서:
```javascript
// localStorage나 sessionStorage에서 토큰 추출
const token = localStorage.getItem('access_token');

// JWT 디코딩 (jwt-decode 라이브러리 필요)
const decoded = jwt_decode(token);
console.log('Roles:', decoded['https://yourapp.com/roles']);
console.log('Permissions:', decoded['https://yourapp.com/permissions']);
```

## 5. 테스트 데이터 설정

### 5.1 테스트 관리자 계정 생성
데이터베이스에 테스트 관리자 추가:

```sql
-- 테스트 관리자 계정
INSERT INTO user (user_id, auth0_id, user_email, created_at) VALUES 
('admin-test-001', 'auth0|admintest001', 'admin@api-bridge.com', NOW());

-- 관리자 PRO 구독 (테스트용)
INSERT INTO subscription (subscription_id, user_id, plan_id, status, plan_payment_date) VALUES 
('sub-admin-test', 'admin-test-001', 2, 'ACTIVE', NOW());
```

### 5.2 Auth0에서 테스트 사용자 역할 설정
Auth0 Dashboard → Users → 특정 사용자 → 다음 Metadata 추가:

```json
// app_metadata
{
  "roles": ["user", "admin"],
  "permissions": ["read:profile", "read:all_users", "write:all_users", "manage:system"]
}
```

## 6. 자동화된 테스트 스크립트

### 6.1 Bash 테스트 스크립트
```bash
#!/bin/bash
# test_admin_auth.sh

API_BASE="http://localhost:8080"
ADMIN_TOKEN="eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9..." # 실제 토큰 입력
USER_TOKEN="eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9..."  # 일반 사용자 토큰

echo "🧪 관리자 권한 테스트 시작..."

# 1. 관리자 API 성공 테스트
echo "✅ 관리자 전용 API 테스트 (200 OK 예상)"
curl -s -w "\n상태코드: %{http_code}\n" \
  -X GET "${API_BASE}/gateway/users/admin/users" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}"

# 2. 권한 없는 접근 테스트  
echo -e "\n❌ 권한 없는 접근 테스트 (403 Forbidden 예상)"
curl -s -w "\n상태코드: %{http_code}\n" \
  -X GET "${API_BASE}/gateway/users/admin/users" \
  -H "Authorization: Bearer ${USER_TOKEN}"

# 3. 토큰 없는 접근 테스트
echo -e "\n🚫 토큰 없는 접근 테스트 (401 Unauthorized 예상)" 
curl -s -w "\n상태코드: %{http_code}\n" \
  -X GET "${API_BASE}/gateway/users/admin/users"

echo -e "\n🎯 테스트 완료!"
```

### 6.2 PowerShell 테스트 스크립트 (Windows)
```powershell
# test_admin_auth.ps1
$API_BASE = "http://localhost:8080"
$ADMIN_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9..." # 실제 토큰 입력
$USER_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9..."  # 일반 사용자 토큰

Write-Host "🧪 관리자 권한 테스트 시작..." -ForegroundColor Green

# 관리자 API 테스트
$headers = @{ "Authorization" = "Bearer $ADMIN_TOKEN" }
try {
    $response = Invoke-RestMethod -Uri "$API_BASE/gateway/users/admin/users" -Headers $headers
    Write-Host "✅ 관리자 API 성공: $($response.success)" -ForegroundColor Green
} catch {
    Write-Host "❌ 관리자 API 실패: $($_.Exception.Message)" -ForegroundColor Red
}

# 권한 없는 접근 테스트
$userHeaders = @{ "Authorization" = "Bearer $USER_TOKEN" }  
try {
    $response = Invoke-RestMethod -Uri "$API_BASE/gateway/users/admin/users" -Headers $userHeaders
    Write-Host "⚠️  예상하지 못한 성공" -ForegroundColor Yellow
} catch {
    Write-Host "✅ 권한 없는 접근 차단됨: $($_.Exception.Response.StatusCode)" -ForegroundColor Green
}
```

## 7. 프런트엔드 연동 테스트

### 7.1 JavaScript 클라이언트 테스트
```javascript
// auth-test.js
class AdminAuthTester {
    constructor(apiBase = 'http://localhost:8080') {
        this.apiBase = apiBase;
        this.token = localStorage.getItem('access_token');
    }
    
    async testAdminAccess() {
        const response = await fetch(`${this.apiBase}/gateway/users/admin/users`, {
            headers: {
                'Authorization': `Bearer ${this.token}`,
                'Content-Type': 'application/json'
            }
        });
        
        console.log('관리자 API 응답:', response.status);
        if (response.ok) {
            const data = await response.json();
            console.log('데이터:', data);
        }
    }
    
    checkUserRoles() {
        if (!this.token) {
            console.log('토큰이 없습니다');
            return;
        }
        
        const decoded = this.decodeJWT(this.token);
        const roles = decoded['https://yourapp.com/roles'] || [];
        const permissions = decoded['https://yourapp.com/permissions'] || [];
        
        console.log('사용자 역할:', roles);
        console.log('사용자 권한:', permissions);
        console.log('관리자 여부:', roles.includes('admin'));
    }
    
    decodeJWT(token) {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        return JSON.parse(jsonPayload);
    }
}

// 사용법
const tester = new AdminAuthTester();
tester.checkUserRoles();
tester.testAdminAccess();
```

## 8. 문제 해결 가이드

### 8.1 일반적인 오류와 해결책

**403 Forbidden (예상된 동작)**
- 일반 사용자가 관리자 API 접근 시 정상
- AdminRoleFilter가 올바르게 작동 중

**401 Unauthorized**
- JWT 토큰이 없거나 만료됨
- Auth0 설정이나 토큰 발급 확인 필요

**500 Internal Server Error**
- AdminService나 관련 의존성 누락
- 로그 확인 후 누락된 서비스 구현 필요

### 8.2 로그 모니터링
```bash
# UserService 로그 모니터링
tail -f logs/user-service.log | grep -i "admin"

# API Gateway 로그 모니터링  
tail -f logs/api-gateway.log | grep -i "AdminRoleFilter"
```

이제 Auth0 Actions 설정 후 위의 방법들로 단계별 테스트할 수 있습니다!