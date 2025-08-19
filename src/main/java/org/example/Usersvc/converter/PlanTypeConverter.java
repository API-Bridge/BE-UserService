package org.example.Usersvc.converter;

import org.example.Usersvc.domain.PlanType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * String을 PlanType으로 변환하는 Spring Converter
 * 
 * 이 컨버터는 HTTP 요청 파라미터로 받은 문자열을 
 * 자동으로 PlanType enum으로 변환합니다.
 * 
 * 등록 후 @RequestParam PlanType planType 형태로 직접 사용 가능합니다.
 */
@Component
public class PlanTypeConverter implements Converter<String, PlanType> {

    @Override
    public PlanType convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("플랜 타입은 비어있을 수 없습니다.");
        }
        
        try {
            return PlanType.fromString(source.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 플랜 타입: " + source + 
                                             ". 지원 플랜: FREE, PRO");
        }
    }
}