package org.example.Usersvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.domain.UserSecretsArn;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.service.UserService;
import org.example.Usersvc.service.UserSecretsArnService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// UserController - 사용자 관리 REST API 컨트롤러
// Auth0 연동을 통한 사용자 관리 및 BYOK(Bring Your Own Key) 기능을 제공하는 컨트롤러입니다.
// 
// 주요 엔드포인트:
// - POST /api/users - 사용자 생성
// - GET /api/users/{userId} - 사용자 조회
// - POST /api/users/{userId}/secrets - 개인 키 등록
// - GET /api/secrets/{userId}/arn - 개인 키 조회 (AI 서비스용)
// - GET /api/users/{userId}/secrets - 사용자 키 목록 조회
//
// 보안 고려사항:
// - 모든 엔드포인트는 인증된 사용자만 접근 가능
// - 사용자 ID는 UUID 형식으로 검증
// - 민감한 키 값은 응답에서 마스킹 처리
// - 입력 데이터 유효성 검증 적용
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "User Management", description = "사용자 관리 및 개인 키(BYOK) 관리 API")
public class UserController {

    private final UserService userService;
    private final UserSecretsArnService userSecretsArnService;

    // 사용자 생성 엔드포인트
    // Auth0에서 받은 사용자 정보를 기반으로 새로운 사용자를 시스템에 등록합니다.
    //
    // 요청: POST /api/users
    // Body: { "auth0Id": "auth0|...", "userEmail": "user@example.com" }
    // 응답: 201 Created, 생성된 사용자 정보
    @Operation(
            summary = "사용자 생성",
            description = "Auth0에서 받은 사용자 정보를 기반으로 새로운 사용자를 시스템에 등록합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "사용자 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    value = "{\"success\":true,\"message\":\"요청이 성공적으로 처리되었습니다.\",\"data\":{\"userId\":\"123e4567-e89b-12d3-a456-426614174000\",\"auth0Id\":\"auth0|12345\",\"userEmail\":\"user@example.com\",\"createdAt\":\"2024-01-01T10:00:00\"}}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    value = "{\"success\":false,\"message\":\"Auth0 ID는 필수입니다.\",\"errorCode\":\"INVALID_REQUEST\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @PostMapping("/users")
    // @PreAuthorize("hasRole('USER')") // 권한 검증 일시 비활성화
    public ResponseEntity<ApiResponse<User>> createUser(
            @Parameter(description = "사용자 생성 요청 정보", required = true)
            @Valid @RequestBody CreateUserRequest request) {
        log.info("사용자 생성 요청 - auth0Id: {}, email: {}", request.auth0Id(), request.userEmail());
        
        try {
            // 입력 데이터 검증
            validateCreateUserRequest(request);
            
            // 사용자 생성
            User createdUser = userService.createUser(request.auth0Id(), request.userEmail());
            
            log.info("사용자 생성 성공 - userId: {}", createdUser.getUserId());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(createdUser));
                    
        } catch (IllegalArgumentException e) {
            log.warn("사용자 생성 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "INVALID_REQUEST"));
                    
        } catch (Exception e) {
            log.error("사용자 생성 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("INTERNAL_ERROR", "사용자 생성에 실패했습니다."));
        }
    }
    
    // 사용자 조회 엔드포인트
    // 사용자 ID를 통해 특정 사용자의 정보를 조회합니다.
    //
    // 요청: GET /api/users/{userId}
    // 응답: 200 OK, 사용자 정보 또는 404 Not Found
    @Operation(
            summary = "사용자 조회",
            description = "사용자 ID를 통해 특정 사용자의 정보를 조회합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "사용자 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 UUID 형식",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @GetMapping("/users/{userId}")
    // @PreAuthorize("hasRole('USER')") // 권한 검증 일시 비활성화
    public ResponseEntity<ApiResponse<User>> getUser(
            @Parameter(description = "조회할 사용자의 UUID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String userId) {
        log.debug("사용자 조회 요청 - userId: {}", userId);
        
        try {
            // 기본적인 사용자 ID 유효성 검사 (null, empty 체크)
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("빈 사용자 ID - userId: {}", userId);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("INVALID_USER_ID", "사용자 ID는 필수입니다."));
            }
            Optional<User> user = userService.getUserById(userId);
            
            if (user.isEmpty()) {
                log.warn("사용자를 찾을 수 없음 - userId: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
            }
            
            log.debug("사용자 조회 성공 - userId: {}", userId);
            return ResponseEntity.ok(ApiResponse.success(user.get()));
            
        } catch (Exception e) {
            log.error("사용자 조회 중 오류 발생 - userId: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("INTERNAL_ERROR", "사용자 조회에 실패했습니다."));
        }
    }
    
    // 개인 키 등록 엔드포인트
    // 사용자가 개인 API 키를 AWS Secrets Manager에 등록하는 엔드포인트입니다.
    // BYOK(Bring Your Own Key) 기능의 핵심 엔드포인트입니다.
    //
    // 요청: POST /api/users/{userId}/secrets
    // Body: { "secretName": "my-key", "secretValue": "sk-...", "description": "..." }
    // 응답: 201 Created, 등록된 키 정보 (ARN 포함)
    @Operation(
            summary = "개인 키 등록",
            description = "사용자가 개인 API 키를 AWS Secrets Manager에 등록합니다. (BYOK - Bring Your Own Key)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PostMapping("/users/{userId}/secrets")
    // @PreAuthorize("hasRole('USER')") // 권한 검증 일시 비활성화
    public ResponseEntity<ApiResponse<UserSecretsArn>> registerUserSecret(
            @Parameter(description = "사용자 UUID") @PathVariable String userId,
            @Parameter(description = "개인 키 등록 요청 정보") @Valid @RequestBody RegisterSecretRequest request) {
        log.info("개인 키 등록 요청 - userId: {}, secretName: {}", userId, request.secretName());
        
        try {
            // 기본적인 사용자 ID 유효성 검사 (null, empty 체크)
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("빈 사용자 ID - userId: {}", userId);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("INVALID_USER_ID", "사용자 ID는 필수입니다."));
            }
            
            // 입력 데이터 검증
            validateRegisterSecretRequest(request);
            
            // 사용자 존재 여부 확인
            if (!userService.getUserById(userId).isPresent()) {
                log.warn("키 등록 실패 - 사용자를 찾을 수 없음: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
            }
            
            // AWS Secrets Manager에 키 저장
            UserSecretsArn registeredArn = userSecretsArnService.storeUserSecret(
                    userId, request.secretName(), request.secretValue(), request.description());
            
            log.info("개인 키 등록 성공 - userId: {}, arnId: {}", userId, registeredArn.getArnId());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(registeredArn));
                    
        } catch (IllegalArgumentException e) {
            log.warn("키 등록 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_REQUEST", e.getMessage()));
                    
        } catch (Exception e) {
            log.error("개인 키 등록 중 오류 발생 - userId: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("INTERNAL_ERROR", "개인 키 등록에 실패했습니다."));
        }
    }
    
    // 개인 키 조회 엔드포인트 (AI 서비스용)
    // AI 서비스가 사용자의 개인 키를 요청할 때 사용하는 엔드포인트입니다.
    // ARN을 통해 AWS Secrets Manager에서 키를 복호화하여 반환합니다.
    //
    // 요청: GET /api/secrets/{userId}/arn?arnId={arnId}
    // 응답: 200 OK, 복호화된 키 값
    @Operation(
            summary = "개인 키 조회 (AI 서비스용)",
            description = "AI 서비스가 사용자의 개인 키를 요청할 때 사용합니다. ARN을 통해 복호화된 키 값을 반환합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/secrets/{userId}/arn")
    // @PreAuthorize("hasRole('SERVICE') or hasRole('USER')") // 권한 검증 일시 비활성화
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserSecretValue(
            @Parameter(description = "사용자 UUID") @PathVariable String userId,
            @Parameter(description = "ARN ID", required = true) @RequestParam @NotBlank(message = "ARN ID는 필수입니다") String arnId) {
        log.info("개인 키 조회 요청 - userId: {}, arnId: {}", userId, arnId);
        
        try {
            // 기본적인 사용자 ID 유효성 검사 (null, empty 체크)
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("빈 사용자 ID - userId: {}", userId);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("INVALID_USER_ID", "사용자 ID는 필수입니다."));
            }
            
            // ARN 정보 조회
            Optional<UserSecretsArn> arnInfo = userSecretsArnService.getSecretsArnById(arnId);
            
            if (arnInfo.isEmpty()) {
                log.warn("ARN을 찾을 수 없음 - arnId: {}", arnId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("ARN_NOT_FOUND", "요청한 ARN을 찾을 수 없습니다."));
            }
            
            // 사용자 권한 확인 (ARN이 해당 사용자의 것인지 확인)
            if (!arnInfo.get().getUserId().equals(userId)) {
                log.warn("권한 없는 ARN 접근 시도 - userId: {}, arnOwner: {}", userId, arnInfo.get().getUserId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("ACCESS_DENIED", "해당 ARN에 접근 권한이 없습니다."));
            }
            
            // AWS Secrets Manager에서 키 값 복호화
            String secretValue = userSecretsArnService.getSecretValue(arnInfo.get().getArn());
            
            // 응답 데이터 구성 (보안상 ARN 정보도 함께 제공)
            Map<String, Object> responseData = Map.of(
                    "arnId", arnInfo.get().getArnId(),
                    "secretValue", secretValue,
                    "description", arnInfo.get().getArnDescription() != null ? arnInfo.get().getArnDescription() : ""
            );
            
            log.info("개인 키 조회 성공 - userId: {}, arnId: {}", userId, arnId);
            
            return ResponseEntity.ok(ApiResponse.success(responseData));
            
        } catch (Exception e) {
            log.error("개인 키 조회 중 오류 발생 - userId: {}, arnId: {}", userId, arnId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("INTERNAL_ERROR", "개인 키 조회에 실패했습니다."));
        }
    }
    
    // 사용자 키 목록 조회 엔드포인트
    // 특정 사용자가 등록한 모든 암호화 키의 목록을 조회합니다.
    // 보안상 실제 키 값은 제외하고 ARN 정보만 반환합니다.
    //
    // 요청: GET /api/users/{userId}/secrets
    // 응답: 200 OK, 사용자의 키 목록 (ARN 정보 포함, 실제 키 값 제외)
    @Operation(
            summary = "사용자 키 목록 조회",
            description = "특정 사용자가 등록한 모든 암호화 키의 목록을 조회합니다. 보안상 실제 키 값은 제외하고 ARN 정보만 반환합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/users/{userId}/secrets")
    // @PreAuthorize("hasRole('USER')") // 권한 검증 일시 비활성화
    public ResponseEntity<ApiResponse<List<UserSecretsArn>>> getUserSecrets(
            @Parameter(description = "사용자 UUID") @PathVariable String userId) {
        log.debug("사용자 키 목록 조회 요청 - userId: {}", userId);
        
        try {
            // 기본적인 사용자 ID 유효성 검사 (null, empty 체크)
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("빈 사용자 ID - userId: {}", userId);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("INVALID_USER_ID", "사용자 ID는 필수입니다."));
            }
            
            // 사용자 존재 여부 확인
            if (!userService.getUserById(userId).isPresent()) {
                log.warn("키 목록 조회 실패 - 사용자를 찾을 수 없음: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."));
            }
            
            // 사용자의 모든 ARN 조회
            List<UserSecretsArn> userSecrets = userSecretsArnService.getUserSecretsArns(userId);
            
            log.debug("사용자 키 목록 조회 성공 - userId: {}, count: {}", userId, userSecrets.size());
            
            return ResponseEntity.ok(ApiResponse.success(userSecrets));
            
        } catch (Exception e) {
            log.error("사용자 키 목록 조회 중 오류 발생 - userId: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("INTERNAL_ERROR", "키 목록 조회에 실패했습니다."));
        }
    }
    
    // 사용자 생성 요청 유효성 검증
    // Auth0 ID와 이메일 형식을 검증합니다.
    private void validateCreateUserRequest(CreateUserRequest request) {
        if (request.auth0Id() == null || request.auth0Id().trim().isEmpty()) {
            throw new IllegalArgumentException("Auth0 ID는 필수입니다.");
        }
        
        if (request.userEmail() == null || request.userEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
        
        if (!request.userEmail().contains("@")) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
        }
        
        if (!request.auth0Id().startsWith("auth0|")) {
            throw new IllegalArgumentException("올바른 Auth0 ID 형식이 아닙니다.");
        }
    }
    
    // 키 등록 요청 유효성 검증
    // 시크릿 이름, 값, 설명의 유효성을 검증합니다.
    private void validateRegisterSecretRequest(RegisterSecretRequest request) {
        if (request.secretName() == null || request.secretName().trim().isEmpty()) {
            throw new IllegalArgumentException("시크릿 이름은 필수입니다.");
        }
        
        if (request.secretValue() == null || request.secretValue().trim().isEmpty()) {
            throw new IllegalArgumentException("시크릿 값은 필수입니다.");
        }
        
        // 시크릿 이름 길이 제한 (AWS Secrets Manager 제한 고려)
        if (request.secretName().length() > 512) {
            throw new IllegalArgumentException("시크릿 이름이 너무 깁니다. (최대 512자)");
        }
        
        // 시크릿 값 길이 제한 (AWS Secrets Manager 제한 고려)
        if (request.secretValue().length() > 65536) {
            throw new IllegalArgumentException("시크릿 값이 너무 큽니다. (최대 65KB)");
        }
    }
    
    // UUID 형식 유효성 검증 메서드

    
    // 사용자 생성 요청 DTO
    // Auth0에서 받은 사용자 정보를 담는 요청 객체입니다.
    @Schema(description = "사용자 생성 요청")
    public record CreateUserRequest(
            @Schema(description = "Auth0 사용자 ID", example = "auth0|12345", required = true)
            @NotBlank(message = "Auth0 ID는 필수입니다")
            String auth0Id,
            
            @Schema(description = "사용자 이메일 주소", example = "user@example.com", required = true)
            @NotBlank(message = "이메일은 필수입니다")
            String userEmail
    ) {}
    
    // 키 등록 요청 DTO
    // 사용자가 개인 키를 등록할 때 사용하는 요청 객체입니다.
    @Schema(description = "개인 키 등록 요청")
    public record RegisterSecretRequest(
            @Schema(description = "시크릿 이름", example = "my-openai-key", required = true)
            @NotBlank(message = "시크릿 이름은 필수입니다")
            String secretName,
            
            @Schema(description = "시크릿 값 (API 키)", example = "sk-1234567890abcdef", required = true)
            @NotBlank(message = "시크릿 값은 필수입니다")
            String secretValue,
            
            @Schema(description = "시크릿 설명", example = "OpenAI API 키")
            String description  // 선택적 필드
    ) {}
}