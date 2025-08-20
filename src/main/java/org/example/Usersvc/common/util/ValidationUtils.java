package org.example.Usersvc.common.util;

import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

import static org.example.Usersvc.common.util.ValidationConstants.*;

/**
 * 공통 검증 유틸리티 클래스
 * 
 * 중복되는 검증 로직을 통합하여 코드 품질을 향상시킵니다.
 */
@Slf4j
public final class ValidationUtils {

    private static final Pattern AUTH0_ID_PATTERN_COMPILED = Pattern.compile(AUTH0_ID_PATTERN);
    private static final Pattern EMAIL_PATTERN_COMPILED = Pattern.compile(EMAIL_PATTERN);

    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 사용자 ID 검증
     * 
     * @param userId 검증할 사용자 ID
     * @return 유효하지 않은 경우 에러 응답, 유효한 경우 null
     */
    public static ResponseEntity<ApiResponse<Void>> validateUserIdAndReturnError(String userId) {
        if (!StringUtils.hasText(userId)) {
            log.warn("빈 사용자 ID 요청");
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(USER_ID_REQUIRED_MESSAGE, INVALID_USER_ID));
        }
        return null;
    }

    /**
     * 사용자 ID 유효성 검사
     * 
     * @param userId 검증할 사용자 ID
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public static boolean isValidUserId(String userId) {
        return StringUtils.hasText(userId);
    }

    /**
     * 이메일 주소 검증
     * 
     * @param email 검증할 이메일 주소
     * @return 유효하지 않은 경우 에러 응답, 유효한 경우 null
     */
    public static ResponseEntity<ApiResponse<Void>> validateEmailAndReturnError(String email) {
        if (!StringUtils.hasText(email)) {
            log.warn("빈 이메일 주소");
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(EMAIL_REQUIRED_MESSAGE, INVALID_EMAIL));
        }
        
        if (!EMAIL_PATTERN_COMPILED.matcher(email).matches()) {
            log.warn("잘못된 이메일 형식: {}", email);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(EMAIL_INVALID_FORMAT_MESSAGE, INVALID_EMAIL));
        }
        
        return null;
    }

    /**
     * 이메일 주소 유효성 검사
     * 
     * @param email 검증할 이메일 주소
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public static boolean isValidEmail(String email) {
        return StringUtils.hasText(email) && EMAIL_PATTERN_COMPILED.matcher(email).matches();
    }

    /**
     * Auth0 ID 검증
     * 
     * @param auth0Id 검증할 Auth0 ID
     * @return 유효하지 않은 경우 에러 응답, 유효한 경우 null
     */
    public static ResponseEntity<ApiResponse<Void>> validateAuth0IdAndReturnError(String auth0Id) {
        if (!StringUtils.hasText(auth0Id)) {
            log.warn("빈 Auth0 ID");
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(AUTH0_ID_REQUIRED_MESSAGE, INVALID_AUTH0_ID));
        }
        
        if (!isValidAuth0IdFormat(auth0Id)) {
            log.warn("잘못된 Auth0 ID 형식: {}", auth0Id);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(AUTH0_ID_INVALID_FORMAT_MESSAGE, INVALID_AUTH0_ID));
        }
        
        return null;
    }

    /**
     * Auth0 ID 형식 유효성 검사
     * 
     * 지원하는 형식:
     * - auth0|{identifier} (Auth0 네이티브 사용자)
     * - google-oauth2|{identifier} (구글 OAuth)
     * - github|{identifier} (깃허브 OAuth)
     * - 기타 OAuth 제공자
     * 
     * @param auth0Id 검증할 Auth0 ID
     * @return 유효한 형식이면 true, 그렇지 않으면 false
     */
    public static boolean isValidAuth0IdFormat(String auth0Id) {
        if (!StringUtils.hasText(auth0Id)) {
            return false;
        }
        
        return AUTH0_ID_PATTERN_COMPILED.matcher(auth0Id).matches();
    }

    /**
     * 시크릿 이름 검증
     * 
     * @param secretName 검증할 시크릿 이름
     * @return 유효하지 않은 경우 에러 응답, 유효한 경우 null
     */
    public static ResponseEntity<ApiResponse<Void>> validateSecretNameAndReturnError(String secretName) {
        if (!StringUtils.hasText(secretName)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(SECRET_NAME_REQUIRED_MESSAGE, INVALID_REQUEST));
        }
        
        if (secretName.length() > MAX_SECRET_NAME_LENGTH) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(SECRET_NAME_TOO_LONG_MESSAGE, INVALID_REQUEST));
        }
        
        return null;
    }

    /**
     * 시크릿 값 검증
     * 
     * @param secretValue 검증할 시크릿 값
     * @return 유효하지 않은 경우 에러 응답, 유효한 경우 null
     */
    public static ResponseEntity<ApiResponse<Void>> validateSecretValueAndReturnError(String secretValue) {
        if (!StringUtils.hasText(secretValue)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(SECRET_VALUE_REQUIRED_MESSAGE, INVALID_REQUEST));
        }
        
        if (secretValue.length() > MAX_SECRET_VALUE_LENGTH) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(SECRET_VALUE_TOO_LARGE_MESSAGE, INVALID_REQUEST));
        }
        
        return null;
    }

    /**
     * Stripe 키가 테스트 모드인지 확인
     * 
     * @param stripeSecretKey 검증할 Stripe 시크릿 키
     * @return 테스트 모드이거나 더미 키인 경우 true
     */
    public static boolean isStripeTestMode(String stripeSecretKey) {
        if (!StringUtils.hasText(stripeSecretKey)) {
            return true;
        }
        
        return stripeSecretKey.contains(STRIPE_MOCK_KEY) ||
               stripeSecretKey.equals(STRIPE_DUMMY_KEY) ||
               stripeSecretKey.startsWith("sk_test_your_test_key");
    }

    /**
     * 페이지 크기 검증 및 보정
     * 
     * @param size 요청된 페이지 크기
     * @return 유효한 범위 내의 페이지 크기
     */
    public static int validateAndCorrectPageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    /**
     * 페이지 번호 검증 및 보정
     * 
     * @param page 요청된 페이지 번호
     * @return 0 이상의 페이지 번호
     */
    public static int validateAndCorrectPageNumber(int page) {
        return Math.max(page, 0);
    }

    /**
     * 안전한 로깅을 위한 Stripe 키 마스킹
     * 
     * @param stripeKey Stripe 키
     * @return 마스킹된 키 또는 상태 메시지
     */
    public static String maskStripeKeyForLogging(String stripeKey) {
        return StringUtils.hasText(stripeKey) ? CONFIGURED_STATUS : NOT_CONFIGURED_STATUS;
    }
}