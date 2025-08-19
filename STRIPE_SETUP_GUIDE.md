# 🔑 Stripe 연동 설정 가이드

## 1. Stripe 대시보드 설정

### Step 1: Stripe 계정 로그인
1. https://dashboard.stripe.com 에 로그인
2. **테스트 환경**인지 확인 (왼쪽 상단에 "TEST DATA" 표시)

### Step 2: API 키 가져오기
1. 왼쪽 사이드바에서 **"Developers"** → **"API keys"** 클릭
2. **Publishable key** (pk_test_로 시작) 복사
3. **Secret key** (sk_test_로 시작) 복사
4. **Reveal** 버튼을 눌러 실제 키 값 확인

### Step 3: 제품(Product) 생성
1. 왼쪽 사이드바에서 **"Products"** 클릭
2. **"+ Add product"** 버튼 클릭

#### Free 플랜 생성:
- **Name**: Free Plan
- **Description**: 무료 플랜 - 기본 기능 제공
- **Pricing**: One-time payment / $0
- 생성 후 **Product ID** (prod_로 시작) 복사

#### Pro 플랜 생성:
- **Name**: Pro Plan  
- **Description**: 프로 플랜 - 고급 기능 제공
- **Pricing**: Recurring / Monthly / $22
- 생성 후 **Product ID** (prod_로 시작) 복사
- **Price ID** (price_로 시작) 복사

### Step 4: Webhook 설정 (선택사항)
1. 왼쪽 사이드바에서 **"Developers"** → **"Webhooks"** 클릭
2. **"+ Add endpoint"** 클릭
3. **Endpoint URL**: `http://localhost:8081/api/webhooks/stripe`
4. **Events to send**: 
   - `customer.created`
   - `customer.subscription.created` 
   - `customer.subscription.updated`
   - `customer.subscription.deleted`
   - `invoice.payment_succeeded`
   - `invoice.payment_failed`

## 2. 환경변수 업데이트

아래 템플릿을 사용하여 실제 Stripe 값으로 업데이트하세요:

```powershell
# 실제 Stripe API 키로 교체하세요
$env:STRIPE_PUBLIC_KEY = "pk_test_실제_퍼블릭_키"
$env:STRIPE_SECRET_KEY = "sk_test_실제_시크릿_키"
$env:STRIPE_WEBHOOK_SECRET = "whsec_실제_웹훅_시크릿"

# 실제 Product ID로 교체하세요  
$env:STRIPE_FREE_PRODUCT_ID = "prod_실제_프리_제품_ID"
$env:STRIPE_PRO_PRODUCT_ID = "prod_실제_프로_제품_ID"

# 실제 Price ID로 교체하세요
$env:STRIPE_PRO_MONTHLY_PRICE_ID = "price_실제_프로_월간_가격_ID"
```

## 3. 테스트 카드 번호

Stripe 테스트 환경에서 사용할 수 있는 카드 번호:

- **성공**: 4242 4242 4242 4242
- **거절**: 4000 0000 0000 0002  
- **인증 필요**: 4000 0025 0000 3155
- **만료일**: 미래의 아무 날짜 (예: 12/25)
- **CVC**: 아무 3자리 숫자 (예: 123)

## 4. 확인 방법

1. UserService 재시작 후 다음 URL에서 테스트:
   - 대시보드: http://localhost:8081/test/dashboard
   - 플랜 조회: http://localhost:8081/test/api/plans

2. 로그에서 "Stripe API key initialized" 메시지 확인
3. 고객 생성 테스트 수행

## 주의사항 ⚠️

- **절대 실제(라이브) API 키를 테스트 환경에 사용하지 마세요**
- **SECRET KEY는 절대 클라이언트 코드에 노출하지 마세요**
- **git에 실제 키를 커밋하지 마세요**
