package org.example.Usersvc.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 간단한 테스트 컨트롤러 - Thymeleaf 동작 확인용
 */
@Controller
@RequestMapping("/simple")
@Slf4j
public class SimpleTestController {

    @GetMapping("/test")
    public String simpleTest(Model model) {
        log.info("Simple test page accessed");
        model.addAttribute("message", "Hello, Thymeleaf!");
        return "simple-test";
    }
    
    @GetMapping("/api")
    @ResponseBody
    public String apiTest() {
        log.info("Simple API test accessed");
        return "API is working!";
    }
}