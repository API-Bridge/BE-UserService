# 💳 Stripe 구독 결제 플로우 완벽 가이드

## 🔄 현재 결제 플로우 구조

### 1. 결제 세션 생성 → 2. Stripe 결제 창 → 3. 웹훅으로 결과 처리

---

## 🚀 1. 결제 창 열기 - 전체 플로우

### 1.1 **결제 세션 생성 API 호출**
```bash
curl -X POST "http://localhost:8080/api/subscription/checkout" \
  -H "Content-Type: application/json" \
  -d '{
    "priceId": "price_1234567890",
    "successUrl": "http://localhost:3000/success?session_id={CHECKOUT_SESSION_ID}",
    "cancelUrl": "http://localhost:3000/cancel"
  }'
```

**응답:**
```json
{
  "success": true,
  "data": {
    "checkoutUrl": "https://checkout.stripe.com/c/pay/cs_test_...",
    "sessionId": "cs_test_1234567890abcdef",
    "priceId": "price_1234567890"
  }
}
```

### 1.2 **브라우저에서 결제 창 열기**
```javascript
// Frontend (React/Vue/Angular)
fetch('/api/subscription/checkout', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    priceId: 'price_pro_monthly',
    successUrl: window.location.origin + '/success?session_id={CHECKOUT_SESSION_ID}',
    cancelUrl: window.location.origin + '/cancel'
  })
})
.then(response => response.json())
.then(data => {
  if (data.success) {
    // Stripe Checkout 페이지로 리다이렉트
    window.location.href = data.data.checkoutUrl;
  }
});
```

### 1.3 **실제 결제 창 URL 예시**
```
https://checkout.stripe.com/c/pay/cs_test_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0#fidkdWxOYHwnPyd1blpxYHZxWjA0S3I2VVBrSWZXVFE8VHN8d3dhNDZzPX1AXXd3b0h1NjBBZUltN0p%2FblJNS0xkSFZwT2htM31VaG1rPU9VcUBUMTR3TTVCZEhgPTVhfGBkQGZnQkAzdGF3YEhyZnE8U2pRPQ%3D%3D
```

이 URL로 이동하면 **Stripe가 호스팅하는 안전한 결제 페이지**가 열립니다.

---

## 💾 2. 결제 데이터 저장 위치

### 2.1 **Stripe 측 저장 데이터**
```json
{
  "customer": {
    "id": "cus_1234567890",
    "email": "user@example.com",
    "created": 1640995200,
    "subscriptions": [...],
    "payment_methods": [...]
  },
  "subscription": {
    "id": "sub_1234567890",
    "customer": "cus_1234567890",
    "status": "active",
    "current_period_start": 1640995200,
    "current_period_end": 1643673600,
    "plan": {
      "id": "price_pro_monthly",
      "amount": 2900,
      "currency": "usd"
    }
  }
}
```

### 2.2 **우리 DB에 저장되는 데이터**

#### **`subscription` 테이블**
```sql
-- 결제 성공 후 웹훅을 통해 업데이트됨
UPDATE subscription 
SET 
  plan_id = 2,  -- Pro 플랜
  plan_payment_date = CURRENT_TIMESTAMP,
  plan_update_date = CURRENT_TIMESTAMP,
  is_active = true
WHERE user_id = 'user-001';
```

#### **데이터 저장 예시**
| subscription_id | user_id | plan_id | plan_payment_date | is_active |
|----------------|---------|---------|-------------------|-----------|
| sub-user-001 | user-001 | 2 | 2024-01-01 10:00:00 | true |

---

## 🔗 3. 웹훅으로 결제 결과 처리

### ⚠️ **현재 누락된 부분: 웹훅 컨트롤러**

현재 `StripeWebhookService`는 있지만, 웹훅을 받는 **컨트롤러가 누락**되어 있습니다!

### 3.1 **필요한 웹훅 컨트롤러 추가**

```java
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {
    
    private final StripeWebhookService webhookService;
    
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;
    
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        
        try {
            // Stripe 시그니처 검증
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);
            
            // 웹훅 처리
            webhookService.handleWebhook(event);
            
            return ResponseEntity.ok("success");
            
        } catch (SignatureVerificationException e) {
            log.error("Stripe 시그니처 검증 실패: {}", e.getMessage());
            return ResponseEntity.status(400).body("Invalid signature");
        } catch (Exception e) {
            log.error("웹훅 처리 중 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Webhook processing failed");
        }
    }
}
```

### 3.2 **Stripe 대시보드 웹훅 설정**

**Stripe 대시보드에서 설정해야 할 웹훅 URL:**
```
https://your-domain.com/api/webhooks/stripe
```

**수신해야 할 이벤트들:**
- `customer.subscription.created` - 구독 생성
- `customer.subscription.updated` - 구독 변경  
- `customer.subscription.deleted` - 구독 취소
- `invoice.payment_succeeded` - 결제 성공
- `invoice.payment_failed` - 결제 실패

---

## 🧪 4. 전체 결제 테스트 시나리오

### 4.1 **테스트용 HTML 페이지 생성**

```html
<!-- src/main/resources/static/payment-test.html -->
<!DOCTYPE html>
<html>
<head>
    <title>Stripe 결제 테스트</title>
    <script src="https://js.stripe.com/v3/"></script>
</head>
<body>
    <h1>구독 플랜 선택</h1>
    
    <div>
        <h3>Pro 플랜 - $29/월</h3>
        <button onclick="checkout('price_pro_monthly')">Pro 플랜 결제</button>
    </div>
    
    <div>
        <h3>Enterprise 플랜 - $99/월</h3>
        <button onclick="checkout('price_enterprise_monthly')">Enterprise 플랜 결제</button>
    </div>

    <script>
        async function checkout(priceId) {
            try {
                const response = await fetch('/api/subscription/checkout', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        priceId: priceId,
                        successUrl: window.location.origin + '/payment-success.html?session_id={CHECKOUT_SESSION_ID}',
                        cancelUrl: window.location.origin + '/payment-cancel.html'
                    })
                });
                
                const data = await response.json();
                
                if (data.success) {
                    // Stripe Checkout으로 리다이렉트
                    window.location.href = data.data.checkoutUrl;
                } else {
                    alert('결제 세션 생성 실패: ' + data.message);
                }
            } catch (error) {
                alert('오류 발생: ' + error.message);
            }
        }
    </script>
</body>
</html>
```

### 4.2 **성공/실패 페이지**

```html
<!-- src/main/resources/static/payment-success.html -->
<!DOCTYPE html>
<html>
<head><title>결제 성공</title></head>
<body>
    <h1>🎉 결제가 완료되었습니다!</h1>
    <p>구독이 활성화되었습니다. 잠시 후 계정에 반영됩니다.</p>
    <button onclick="window.location.href='/payment-test.html'">돌아가기</button>
    
    <script>
        // URL에서 session_id 추출하여 결제 확인
        const urlParams = new URLSearchParams(window.location.search);
        const sessionId = urlParams.get('session_id');
        
        if (sessionId) {
            console.log('결제 세션 ID:', sessionId);
            // 추가적인 결제 확인 로직
        }
    </script>
</body>
</html>
```

### 4.3 **실제 테스트 순서**

1. **애플리케이션 실행**
   ```bash
   gradlew.bat bootRun
   ```

2. **테스트 페이지 접속**
   ```
   http://localhost:8080/payment-test.html
   ```

3. **Pro 플랜 결제 버튼 클릭**
   - 자동으로 Stripe Checkout 페이지로 이동
   - 테스트 카드 정보 입력:
     ```
     카드번호: 4242 4242 4242 4242
     만료일: 12/34
     CVC: 123
     ```

4. **결제 완료 후**
   - 성공 페이지로 리다이렉트
   - 웹훅을 통해 DB 업데이트 (웹훅 컨트롤러 추가 후)

---

## 🔧 5. 즉시 테스트 가능한 방법

### 5.1 **API로 결제 URL 받기**
```bash
curl -X POST "http://localhost:8080/api/subscription/checkout" \
  -H "Content-Type: application/json" \
  -d '{
    "priceId": "price_1234567890",
    "successUrl": "http://localhost:8080/success",
    "cancelUrl": "http://localhost:8080/cancel"
  }'
```

### 5.2 **응답에서 checkoutUrl 복사하여 브라우저에서 열기**
```json
{
  "success": true,
  "data": {
    "checkoutUrl": "https://checkout.stripe.com/c/pay/cs_test_...",
    "sessionId": "cs_test_...",
    "priceId": "price_1234567890"
  }
}
```

### 5.3 **Stripe 테스트 카드로 결제 진행**

---

## ⚠️ 현재 시스템에서 누락된 부분

1. **✅ 이미 구현됨:**
   - Stripe Checkout 세션 생성
   - 결제 창 URL 반환
   - 웹훅 처리 로직 (`StripeWebhookService`)

2. **❌ 누락됨 (추가 필요):**
   - 웹훅 컨트롤러 (`StripeWebhookController`)
   - 프론트엔드 테스트 페이지
   - Stripe 대시보드 웹훅 URL 설정

3. **🔧 설정 필요:**
   - Stripe API 키 (테스트/프로덕션)
   - 웹훅 시크릿 키
   - 실제 Price ID들

---

**결론: 현재 결제 창은 API 호출로 URL을 받아서 브라우저에서 열 수 있고, 결제 데이터는 웹훅을 통해 우리 DB에 저장됩니다!** 💎