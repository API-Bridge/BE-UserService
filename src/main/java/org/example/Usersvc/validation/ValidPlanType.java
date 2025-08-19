package org.example.Usersvc.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * PlanType 유효성 검증 어노테이션
 * 
 * 문자열이 유효한 PlanType (FREE, PRO)으로 변환될 수 있는지 검증합니다.
 * 
 * 사용법:
 * ```java
 * @ValidPlanType
 * @RequestParam String planType
 * ```
 */
@Documented
@Constraint(validatedBy = PlanTypeValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPlanType {
    
    String message() default "유효하지 않은 플랜 타입입니다. 지원 플랜: FREE, PRO";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}