package org.example.Usersvc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 결제 테스트용 페이지 컨트롤러
 */
@Controller
public class PaymentTestController {

    /**
     * 결제 테스트 페이지
     */
    @GetMapping("/payment-test")
    public String paymentTestPage() {
        return "payment-test";
    }
    
    /**
     * 루트 페이지에서도 테스트 페이지로 이동
     */
    @GetMapping("/")
    public String homePage() {
        return "redirect:/payment-test";
    }
}