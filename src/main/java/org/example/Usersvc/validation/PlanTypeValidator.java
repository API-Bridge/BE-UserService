package org.example.Usersvc.validation;

import org.example.Usersvc.domain.PlanType;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * PlanType enum 유효성 검증기
 * 
 * @ValidPlanType 어노테이션과 함께 사용되어 
 * 문자열이 유효한 PlanType으로 변환될 수 있는지 검증합니다.
 */
public class PlanTypeValidator implements ConstraintValidator<ValidPlanType, String> {

    @Override
    public void initialize(ValidPlanType constraintAnnotation) {
        // 초기화 로직이 필요한 경우 여기에 구현
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null 값은 @NotNull로 별도 검증
        }
        
        try {
            PlanType.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            // 커스텀 에러 메시지 설정
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "지원하지 않는 플랜입니다: " + value + ". 지원 플랜: FREE, PRO"
            ).addConstraintViolation();
            return false;
        }
    }
}