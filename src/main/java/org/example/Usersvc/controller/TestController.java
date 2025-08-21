package org.example.Usersvc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 테스트 페이지 컨트롤러
 */
@RestController
public class TestController {

    @GetMapping(value = "/tosspay-v2-widget", produces = "text/html;charset=UTF-8")
    public String tossPayV2WidgetPage() {
        return """
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>🎉 TossPay V2 결제위젯</title>
    <script src="https://js.tosspayments.com/v2/payment"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; 
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
        }
        .container { 
            max-width: 1000px; 
            margin: 0 auto; 
            background: white; 
            border-radius: 20px; 
            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
            overflow: hidden;
        }
        .header {
            background: linear-gradient(45deg, #1e40af, #3b82f6);
            color: white;
            padding: 30px;
            text-align: center;
        }
        .header h1 { font-size: 2.5rem; margin-bottom: 10px; }
        .header p { opacity: 0.9; font-size: 1.1rem; }
        .content { display: flex; gap: 20px; padding: 30px; }
        .left-panel { flex: 1; }
        .right-panel { flex: 1; }
        .form-group { 
            margin-bottom: 20px; 
            display: flex; 
            flex-direction: column;
        }
        .form-group label { 
            margin-bottom: 8px; 
            font-weight: 600; 
            color: #374151;
            font-size: 0.95rem;
        }
        .form-group input, .form-group select { 
            padding: 12px 16px; 
            border: 2px solid #e5e7eb; 
            border-radius: 8px; 
            font-size: 1rem;
            transition: all 0.3s ease;
        }
        .form-group input:focus, .form-group select:focus { 
            outline: none; 
            border-color: #3b82f6; 
            box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
        }
        .payment-widget {
            min-height: 300px;
            border: 2px dashed #d1d5db;
            border-radius: 12px;
            padding: 20px;
            background: #f9fafb;
        }
        .btn {
            background: linear-gradient(45deg, #1e40af, #3b82f6);
            color: white;
            border: none;
            padding: 14px 28px;
            border-radius: 8px;
            font-size: 1.1rem;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s ease;
            width: 100%;
            margin-top: 10px;
        }
        .btn:hover { 
            transform: translateY(-2px);
            box-shadow: 0 4px 15px rgba(59, 130, 246, 0.4);
        }
        .btn:disabled {
            background: #9ca3af;
            cursor: not-allowed;
            transform: none;
            box-shadow: none;
        }
        .status {
            padding: 15px;
            border-radius: 8px;
            margin-top: 15px;
            font-weight: 500;
        }
        .status.loading { background: #fef3c7; color: #92400e; }
        .status.success { background: #d1fae5; color: #065f46; }
        .status.error { background: #fee2e2; color: #991b1b; }
        .price-display {
            font-size: 2rem;
            font-weight: 700;
            color: #1e40af;
            text-align: center;
            margin: 20px 0;
        }
        @media (max-width: 768px) {
            .content { flex-direction: column; padding: 20px; }
            .header h1 { font-size: 2rem; }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🎉 TossPay V2 결제위젯</h1>
            <p>최신 결제위젯으로 간편하고 안전한 결제를 경험하세요</p>
        </div>
        
        <div class="content">
            <div class="left-panel">
                <h2>📝 결제 정보</h2>
                
                <div class="form-group">
                    <label for="userId">👤 사용자 ID</label>
                    <input type="text" id="userId" value="widget-user-001" placeholder="사용자 ID를 입력하세요">
                </div>
                
                <div class="form-group">
                    <label for="planType">📋 구독 플랜</label>
                    <select id="planType" onchange="updatePrice()">
                        <option value="PRO">PRO 플랜 - 모든 기능 무제한</option>
                    </select>
                </div>
                
                <div class="price-display" id="priceDisplay">
                    ₩9,900
                </div>
                
                <button class="btn" id="loadWidgetBtn" onclick="loadPaymentWidget()">
                    🎨 결제위젯 불러오기
                </button>
            </div>
            
            <div class="right-panel">
                <h2>💳 결제위젯</h2>
                <div id="payment-method" class="payment-widget">
                    <div style="text-align: center; color: #6b7280; margin-top: 100px;">
                        <p>👆 왼쪽에서 "결제위젯 불러오기" 버튼을 눌러주세요</p>
                    </div>
                </div>
                
                <button class="btn" id="paymentBtn" onclick="requestPayment()" disabled>
                    🚀 결제하기
                </button>
                
                <div id="status" class="status" style="display: none;"></div>
            </div>
        </div>
    </div>

    <script>
        const CLIENT_KEY = 'test_ck_ORzdMaqN3wKEX17JQBNb35AkYXQG';
        let paymentWidget = null;
        
        function updatePrice() {
            const planType = document.getElementById('planType').value;
            const priceDisplay = document.getElementById('priceDisplay');
            
            switch(planType) {
                case 'PRO':
                    priceDisplay.textContent = '₩9,900';
                    break;
                default:
                    priceDisplay.textContent = '₩9,900';
            }
        }
        
        async function loadPaymentWidget() {
            const userId = document.getElementById('userId').value;
            const planType = document.getElementById('planType').value;
            
            if (!userId) {
                alert('사용자 ID를 입력해주세요.');
                return;
            }
            
            try {
                showStatus('loading', '🔄 결제위젯 초기화 중...');
                document.getElementById('loadWidgetBtn').disabled = true;
                
                // 백엔드에서 결제 정보 생성
                const response = await fetch('/api/tosspay/widget/prepare', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ userId: userId, planType: planType })
                });
                
                const data = await response.json();
                
                if (data.success) {
                    const paymentData = data.data;
                    
                    // V2 결제위젯 초기화
                    paymentWidget = TossPayments(CLIENT_KEY);
                    
                    // 결제위젯 렌더링
                    await paymentWidget.renderPaymentMethods({
                        selector: '#payment-method',
                        variantKey: 'DEFAULT',
                        paymentKey: paymentData.paymentKey || paymentData.orderId
                    }, {
                        amount: {
                            currency: 'KRW',
                            value: paymentData.amount
                        }
                    });
                    
                    // 전역 변수에 결제 데이터 저장
                    window.paymentData = paymentData;
                    
                    showStatus('success', '✅ 결제위젯이 성공적으로 로드되었습니다!');
                    document.getElementById('paymentBtn').disabled = false;
                    
                } else {
                    showStatus('error', '❌ 결제위젯 로드 실패: ' + data.message);
                    document.getElementById('loadWidgetBtn').disabled = false;
                }
                
            } catch (error) {
                console.error('결제위젯 로드 오류:', error);
                showStatus('error', '❌ 결제위젯 로드 오류: ' + error.message);
                document.getElementById('loadWidgetBtn').disabled = false;
            }
        }
        
        async function requestPayment() {
            if (!paymentWidget || !window.paymentData) {
                alert('먼저 결제위젯을 로드해주세요.');
                return;
            }
            
            try {
                showStatus('loading', '🚀 결제 처리 중...');
                document.getElementById('paymentBtn').disabled = true;
                
                const paymentData = window.paymentData;
                
                // V2 결제 요청
                await paymentWidget.requestPayment({
                    orderId: paymentData.orderId,
                    orderName: paymentData.orderName,
                    successUrl: paymentData.successUrl,
                    failUrl: paymentData.failUrl,
                    customerEmail: paymentData.customerEmail,
                    customerName: paymentData.customerName
                });
                
            } catch (error) {
                console.error('결제 요청 오류:', error);
                showStatus('error', '❌ 결제 요청 오류: ' + error.message);
                document.getElementById('paymentBtn').disabled = false;
            }
        }
        
        function showStatus(type, message) {
            const statusEl = document.getElementById('status');
            statusEl.className = 'status ' + type;
            statusEl.textContent = message;
            statusEl.style.display = 'block';
            
            if (type === 'success' || type === 'error') {
                setTimeout(() => {
                    statusEl.style.display = 'none';
                }, 5000);
            }
        }
        
        // 페이지 로드 시 초기화
        window.addEventListener('load', function() {
            updatePrice();
        });
    </script>
</body>
</html>
                """;
    }

    @GetMapping(value = "/tosspay-success", produces = "text/html;charset=UTF-8")
    public String tossPaySuccessPage() {
        return """
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>🎉 결제 성공</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; 
            background: linear-gradient(135deg, #10b981 0%, #059669 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 20px;
        }
        .container { 
            max-width: 600px; 
            background: white; 
            border-radius: 20px; 
            box-shadow: 0 20px 40px rgba(0,0,0,0.1);
            padding: 40px;
            text-align: center;
        }
        .success-icon {
            width: 80px;
            height: 80px;
            background: #10b981;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0 auto 30px;
            animation: bounce 1s ease-in-out;
        }
        .success-icon::before {
            content: "✓";
            color: white;
            font-size: 2.5rem;
            font-weight: bold;
        }
        @keyframes bounce {
            0%, 20%, 50%, 80%, 100% { transform: translateY(0); }
            40% { transform: translateY(-30px); }
            60% { transform: translateY(-15px); }
        }
        h1 { 
            color: #1f2937; 
            margin-bottom: 20px; 
            font-size: 2.5rem;
        }
        .subtitle {
            color: #6b7280;
            font-size: 1.2rem;
            margin-bottom: 30px;
            line-height: 1.6;
        }
        .payment-info {
            background: #f9fafb;
            border-radius: 12px;
            padding: 20px;
            margin: 30px 0;
            text-align: left;
        }
        .info-row {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 10px 0;
            border-bottom: 1px solid #e5e7eb;
        }
        .info-row:last-child { border-bottom: none; }
        .info-label { 
            font-weight: 600; 
            color: #374151;
        }
        .info-value { 
            color: #10b981;
            font-weight: 600;
        }
        .btn {
            background: linear-gradient(45deg, #10b981, #059669);
            color: white;
            border: none;
            padding: 15px 30px;
            border-radius: 10px;
            font-size: 1.1rem;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s ease;
            margin: 10px;
            text-decoration: none;
            display: inline-block;
        }
        .btn:hover { 
            transform: translateY(-2px);
            box-shadow: 0 4px 15px rgba(16, 185, 129, 0.4);
        }
        .status {
            padding: 15px;
            border-radius: 8px;
            margin: 20px 0;
            font-weight: 500;
        }
        .status.loading { background: #fef3c7; color: #92400e; }
        .status.success { background: #d1fae5; color: #065f46; }
        .status.error { background: #fee2e2; color: #991b1b; }
    </style>
</head>
<body>
    <div class="container">
        <div class="success-icon"></div>
        
        <h1>🎉 결제 완료!</h1>
        <p class="subtitle">
            TossPay를 통한 결제가 성공적으로 완료되었습니다.<br>
            잠시만 기다려주시면 결제 승인 처리를 완료해드리겠습니다.
        </p>

        <div class="payment-info">
            <div class="info-row">
                <span class="info-label">📋 주문번호</span>
                <span class="info-value" id="orderId">로딩중...</span>
            </div>
            <div class="info-row">
                <span class="info-label">💳 결제키</span>
                <span class="info-value" id="paymentKey">로딩중...</span>
            </div>
            <div class="info-row">
                <span class="info-label">💰 결제금액</span>
                <span class="info-value" id="amount">로딩중...</span>
            </div>
            <div class="info-row">
                <span class="info-label">✅ 상태</span>
                <span class="info-value" id="paymentStatus">승인 처리 중...</span>
            </div>
        </div>

        <div id="status" class="status loading">
            🔄 TossPay 결제 승인 API를 호출하고 있습니다...
        </div>

        <div id="actions" style="display: none;">
            <a href="/tosspay-v2-widget" class="btn">🔄 새로운 결제</a>
            <a href="/" class="btn">🏠 홈으로 이동</a>
        </div>
    </div>

    <script>
        // URL 파라미터에서 결제 정보 추출
        const urlParams = new URLSearchParams(window.location.search);
        const paymentKey = urlParams.get('paymentKey');
        const orderId = urlParams.get('orderId');
        const amount = urlParams.get('amount');

        // 화면에 결제 정보 표시
        document.getElementById('paymentKey').textContent = paymentKey || 'N/A';
        document.getElementById('orderId').textContent = orderId || 'N/A';
        document.getElementById('amount').textContent = amount ? '₩' + Number(amount).toLocaleString() : 'N/A';

        // 자동으로 결제 승인 API 호출
        async function confirmPayment() {
            if (!paymentKey || !orderId || !amount) {
                showStatus('error', '❌ 결제 정보가 누락되었습니다.');
                return;
            }

            try {
                showStatus('loading', '🚀 TossPay 승인 API 호출 중...');

                const response = await fetch('/api/tosspay/confirm', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        paymentKey: paymentKey,
                        orderId: orderId,
                        amount: parseInt(amount)
                    })
                });

                const data = await response.json();

                if (data.success) {
                    document.getElementById('paymentStatus').textContent = '승인 완료 ✅';
                    showStatus('success', '✅ 결제 승인이 완료되었습니다! 구독이 활성화되었습니다.');
                    document.getElementById('actions').style.display = 'block';
                } else {
                    showStatus('error', '❌ 결제 승인 실패: ' + data.message);
                    document.getElementById('paymentStatus').textContent = '승인 실패 ❌';
                }

            } catch (error) {
                console.error('결제 승인 오류:', error);
                showStatus('error', '❌ 결제 승인 처리 중 오류가 발생했습니다: ' + error.message);
                document.getElementById('paymentStatus').textContent = '승인 오류 ❌';
            }
        }

        function showStatus(type, message) {
            const statusEl = document.getElementById('status');
            statusEl.className = 'status ' + type;
            statusEl.textContent = message;
        }

        // 페이지 로드 시 자동 승인 처리
        window.addEventListener('load', function() {
            setTimeout(confirmPayment, 1000); // 1초 후 자동 승인
        });
    </script>
</body>
</html>
                """;
    }

    /**
     * TossPay V2 Modal Widget Page
     */
    @GetMapping(value = "/tosspay-v2-modal", produces = "text/html;charset=UTF-8")
    public String tossPayV2ModalPage() {
        return """
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>토스페이먼츠 V2 모달 결제</title>
    <script src="https://js.tosspayments.com/v2/payment"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        
        .container {
            background: white;
            padding: 2rem;
            border-radius: 20px;
            box-shadow: 0 20px 40px rgba(0,0,0,0.1);
            max-width: 600px;
            width: 90%;
        }
        
        .header {
            text-align: center;
            margin-bottom: 2rem;
            padding-bottom: 1rem;
            border-bottom: 2px solid #f0f0f0;
        }
        
        .header h1 {
            color: #333;
            font-size: 2rem;
            margin-bottom: 0.5rem;
        }
        
        .header p {
            color: #666;
            font-size: 1.1rem;
        }
        
        .config-section {
            margin-bottom: 2rem;
            padding: 1.5rem;
            background: #f8f9ff;
            border-radius: 12px;
        }
        
        .config-section h3 {
            color: #4a5568;
            margin-bottom: 1rem;
            display: flex;
            align-items: center;
        }
        
        .config-section h3::before {
            content: '⚙️';
            margin-right: 0.5rem;
        }
        
        .form-group {
            margin-bottom: 1rem;
        }
        
        label {
            display: block;
            margin-bottom: 0.5rem;
            color: #4a5568;
            font-weight: 600;
        }
        
        input, select {
            width: 100%;
            padding: 12px;
            border: 2px solid #e2e8f0;
            border-radius: 8px;
            font-size: 1rem;
            transition: border-color 0.2s;
        }
        
        input:focus, select:focus {
            outline: none;
            border-color: #3182ce;
        }
        
        .price-display {
            text-align: center;
            font-size: 1.5rem;
            color: #2d3748;
            font-weight: bold;
            margin-top: 1rem;
            padding: 1rem;
            background: white;
            border-radius: 8px;
        }
        
        .btn {
            width: 100%;
            padding: 15px 24px;
            font-size: 1.2rem;
            font-weight: 600;
            border: none;
            border-radius: 12px;
            cursor: pointer;
            transition: all 0.3s;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 0.5rem;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            box-shadow: 0 8px 16px rgba(102, 126, 234, 0.3);
        }
        
        .btn:hover:not(:disabled) {
            transform: translateY(-3px);
            box-shadow: 0 12px 24px rgba(102, 126, 234, 0.4);
        }
        
        .btn:active {
            transform: translateY(-1px);
        }
        
        .btn:disabled {
            opacity: 0.6;
            cursor: not-allowed;
            transform: none;
        }
        
        .status {
            padding: 1rem;
            border-radius: 8px;
            margin: 1rem 0;
            font-weight: 600;
            text-align: center;
            display: none;
        }
        
        .status.loading {
            background: #e6fffa;
            color: #234e52;
            border: 2px solid #81e6d9;
        }
        
        .status.success {
            background: #f0fff4;
            color: #22543d;
            border: 2px solid #9ae6b4;
        }
        
        .status.error {
            background: #fed7d7;
            color: #742a2a;
            border: 2px solid #fc8181;
        }
        
        /* Modal Styles */
        .modal {
            display: none;
            position: fixed;
            z-index: 10000;
            left: 0;
            top: 0;
            width: 100%;
            height: 100%;
            background-color: rgba(0, 0, 0, 0.6);
            backdrop-filter: blur(8px);
            animation: fadeIn 0.4s ease;
        }
        
        .modal-content {
            background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
            margin: 3% auto;
            padding: 2.5rem;
            border-radius: 24px;
            width: 90%;
            max-width: 650px;
            max-height: 90vh;
            overflow-y: auto;
            position: relative;
            animation: slideUp 0.4s ease;
            box-shadow: 0 32px 64px rgba(0,0,0,0.24);
            border: 1px solid rgba(255,255,255,0.2);
        }
        
        .modal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 2rem;
            padding-bottom: 1.5rem;
            border-bottom: 2px solid #e2e8f0;
        }
        
        .modal-header h2 {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            background-clip: text;
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            font-size: 2rem;
            font-weight: 800;
        }
        
        .close-modal {
            background: linear-gradient(135deg, #f7fafc, #edf2f7);
            border: 2px solid #e2e8f0;
            font-size: 1.5rem;
            cursor: pointer;
            color: #4a5568;
            padding: 0;
            width: 44px;
            height: 44px;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 50%;
            transition: all 0.3s;
            font-weight: bold;
        }
        
        .close-modal:hover {
            background: linear-gradient(135deg, #fed7d7, #feb2b2);
            border-color: #fc8181;
            color: #742a2a;
            transform: scale(1.1);
        }
        
        #payment-method {
            margin: 2rem 0;
            min-height: 350px;
            padding: 2rem;
            border: 3px solid #e2e8f0;
            border-radius: 16px;
            background: linear-gradient(135deg, #ffffff, #f7fafc);
            position: relative;
            overflow: hidden;
        }
        
        .payment-loading {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            min-height: 350px;
            color: #4a5568;
            font-size: 1.2rem;
        }
        
        .spinner {
            width: 60px;
            height: 60px;
            border: 5px solid #e2e8f0;
            border-top: 5px solid #667eea;
            border-radius: 50%;
            animation: spin 1.2s linear infinite;
            margin-bottom: 1.5rem;
            box-shadow: 0 8px 16px rgba(102, 126, 234, 0.2);
        }
        
        .modal-footer {
            display: flex;
            gap: 1rem;
            margin-top: 2rem;
            padding-top: 1.5rem;
            border-top: 2px solid #e2e8f0;
        }
        
        .btn-modal {
            flex: 1;
            padding: 12px 24px;
            font-size: 1.1rem;
            font-weight: 600;
            border: none;
            border-radius: 12px;
            cursor: pointer;
            transition: all 0.3s;
        }
        
        .btn-confirm {
            background: linear-gradient(135deg, #48bb78, #38a169);
            color: white;
            box-shadow: 0 4px 12px rgba(72, 187, 120, 0.4);
        }
        
        .btn-confirm:hover:not(:disabled) {
            transform: translateY(-2px);
            box-shadow: 0 8px 20px rgba(72, 187, 120, 0.5);
        }
        
        .btn-cancel {
            background: linear-gradient(135deg, #e2e8f0, #cbd5e0);
            color: #4a5568;
            border: 2px solid #cbd5e0;
        }
        
        .btn-cancel:hover {
            background: linear-gradient(135deg, #cbd5e0, #a0aec0);
            transform: translateY(-2px);
        }
        
        @keyframes fadeIn {
            from { opacity: 0; }
            to { opacity: 1; }
        }
        
        @keyframes slideUp {
            from { transform: translateY(60px); opacity: 0; }
            to { transform: translateY(0); opacity: 1; }
        }
        
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
        
        @media (max-width: 768px) {
            .container { padding: 1rem; width: 95%; }
            .modal-content { 
                margin: 5% auto; 
                width: 95%; 
                padding: 1.5rem;
                max-height: 95vh; 
            }
            .modal-footer { flex-direction: column; }
            .modal-header h2 { font-size: 1.6rem; }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🎯 토스페이먼츠 V2 모달 결제</h1>
            <p>모달 창에서 편리하게 결제하세요</p>
        </div>
        
        <div class="config-section">
            <h3>결제 설정</h3>
            <div class="form-group">
                <label for="userId">사용자 ID</label>
                <input type="text" id="userId" value="testUser123" placeholder="사용자 ID를 입력하세요">
            </div>
            
            <div class="form-group">
                <label for="planType">구독 플랜</label>
                <select id="planType" onchange="updatePrice()">
                    <option value="PRO">PRO 플랜</option>
                </select>
            </div>
            
            <div class="price-display" id="priceDisplay">
                💰 월 29,000원
            </div>
        </div>
        
        <button class="btn" onclick="openPaymentModal()">
            🚀 결제 시작하기
        </button>
        
        <div id="status" class="status"></div>
    </div>

    <!-- Payment Modal -->
    <div id="paymentModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>💳 결제 진행</h2>
                <button class="close-modal" onclick="closePaymentModal()">&times;</button>
            </div>
            
            <div id="modalStatus" class="status"></div>
            
            <div id="payment-method">
                <div class="payment-loading">
                    <div class="spinner"></div>
                    <div>결제위젯을 로딩 중입니다...</div>
                    <div style="font-size: 0.9rem; margin-top: 0.5rem; color: #718096;">잠시만 기다려주세요</div>
                </div>
            </div>
            
            <div class="modal-footer">
                <button class="btn-modal btn-confirm" id="paymentBtn" onclick="requestPayment()" disabled>
                    💎 결제 승인
                </button>
                <button class="btn-modal btn-cancel" onclick="closePaymentModal()">
                    ❌ 취소
                </button>
            </div>
        </div>
    </div>

    <script>
        const CLIENT_KEY = 'test_ck_ORzdMaqN3wKEX17JQBNb35AkYXQG';
        let paymentWidget = null;
        let isModalOpen = false;
        
        function updatePrice() {
            const planType = document.getElementById('planType').value;
            const priceDisplay = document.getElementById('priceDisplay');
            
            switch(planType) {
                case 'PRO':
                    priceDisplay.textContent = '💰 월 29,000원';
                    break;
                default:
                    priceDisplay.textContent = '💰 무료';
            }
        }
        
        function openPaymentModal() {
            const userId = document.getElementById('userId').value.trim();
            
            if (!userId) {
                alert('사용자 ID를 입력해주세요.');
                return;
            }
            
            isModalOpen = true;
            const modal = document.getElementById('paymentModal');
            modal.style.display = 'block';
            document.body.style.overflow = 'hidden';
            
            // 결제위젯 로드
            setTimeout(() => {
                loadPaymentWidget();
            }, 300);
        }
        
        function closePaymentModal() {
            if (!isModalOpen) return;
            
            isModalOpen = false;
            const modal = document.getElementById('paymentModal');
            modal.style.display = 'none';
            document.body.style.overflow = 'auto';
            
            // 결제위젯 정리
            if (paymentWidget) {
                try {
                    paymentWidget = null;
                } catch (e) {
                    console.log('Widget cleanup:', e);
                }
            }
            
            // 상태 초기화
            document.getElementById('paymentBtn').disabled = true;
            document.getElementById('payment-method').innerHTML = `
                <div class="payment-loading">
                    <div class="spinner"></div>
                    <div>결제위젯을 로딩 중입니다...</div>
                    <div style="font-size: 0.9rem; margin-top: 0.5rem; color: #718096;">잠시만 기다려주세요</div>
                </div>
            `;
            hideModalStatus();
        }
        
        async function loadPaymentWidget() {
            if (!isModalOpen) return;
            
            const userId = document.getElementById('userId').value.trim();
            const planType = document.getElementById('planType').value;
            
            try {
                showModalStatus('loading', '🔄 결제위젯 초기화 중...');
                
                // 백엔드에서 결제 정보 생성
                const response = await fetch('/api/tosspay/widget/prepare', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ userId: userId, planType: planType })
                });
                
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }
                
                const data = await response.json();
                
                if (data.success && isModalOpen) {
                    const paymentData = data.data;
                    
                    // V2 결제위젯 초기화
                    paymentWidget = TossPayments(CLIENT_KEY);
                    
                    // 결제위젯 렌더링
                    await paymentWidget.renderPaymentMethods({
                        selector: '#payment-method',
                        variantKey: 'DEFAULT',
                        paymentKey: paymentData.paymentKey || paymentData.orderId
                    }, {
                        amount: {
                            currency: 'KRW',
                            value: paymentData.amount
                        }
                    });
                    
                    // 전역 변수에 결제 데이터 저장
                    window.paymentData = paymentData;
                    
                    if (isModalOpen) {
                        showModalStatus('success', '✅ 결제위젯이 준비되었습니다!');
                        document.getElementById('paymentBtn').disabled = false;
                    }
                    
                } else {
                    if (isModalOpen) {
                        showModalStatus('error', '❌ 결제위젯 로드 실패: ' + (data.message || '알 수 없는 오류'));
                    }
                }
                
            } catch (error) {
                console.error('결제위젯 로드 오류:', error);
                if (isModalOpen) {
                    showModalStatus('error', '❌ 결제위젯 로드 오류: ' + error.message);
                }
            }
        }
        
        async function requestPayment() {
            if (!paymentWidget || !window.paymentData) {
                alert('결제위젯이 준비되지 않았습니다.');
                return;
            }
            
            try {
                showModalStatus('loading', '🚀 결제를 처리하는 중입니다...');
                document.getElementById('paymentBtn').disabled = true;
                
                const paymentData = window.paymentData;
                
                // V2 결제 요청
                await paymentWidget.requestPayment({
                    orderId: paymentData.orderId,
                    orderName: paymentData.orderName,
                    successUrl: paymentData.successUrl,
                    failUrl: paymentData.failUrl,
                    customerEmail: paymentData.customerEmail,
                    customerName: paymentData.customerName
                });
                
            } catch (error) {
                console.error('결제 요청 오류:', error);
                showModalStatus('error', '❌ 결제 요청 실패: ' + error.message);
                document.getElementById('paymentBtn').disabled = false;
            }
        }
        
        function showStatus(type, message) {
            const statusEl = document.getElementById('status');
            statusEl.className = 'status ' + type;
            statusEl.textContent = message;
            statusEl.style.display = 'block';
            
            if (type === 'success' || type === 'error') {
                setTimeout(() => {
                    statusEl.style.display = 'none';
                }, 5000);
            }
        }
        
        function showModalStatus(type, message) {
            const statusEl = document.getElementById('modalStatus');
            statusEl.className = 'status ' + type;
            statusEl.textContent = message;
            statusEl.style.display = 'block';
            
            if (type === 'success') {
                setTimeout(() => {
                    statusEl.style.display = 'none';
                }, 3000);
            }
        }
        
        function hideModalStatus() {
            document.getElementById('modalStatus').style.display = 'none';
        }
        
        // 모달 외부 클릭 시 닫기
        window.onclick = function(event) {
            const modal = document.getElementById('paymentModal');
            if (event.target === modal) {
                closePaymentModal();
            }
        }
        
        // ESC 키로 모달 닫기
        document.addEventListener('keydown', function(event) {
            if (event.key === 'Escape' && isModalOpen) {
                closePaymentModal();
            }
        });
        
        // 페이지 로드 시 초기화
        window.addEventListener('load', function() {
            updatePrice();
        });
        
        // 페이지 언로드 시 정리
        window.addEventListener('beforeunload', function() {
            if (paymentWidget) {
                paymentWidget = null;
            }
        });
    </script>
</body>
</html>
                """;
    }
}