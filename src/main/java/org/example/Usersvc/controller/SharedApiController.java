package org.example.Usersvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.Usersvc.domain.PlanType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.domain.SharedApi;
import org.example.Usersvc.domain.UserSavedApi;
import org.example.Usersvc.service.SharedApiService;
import org.example.Usersvc.service.UserSavedApiService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/shared-apis")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SharedApi", description = "공유 API 관리")
public class SharedApiController {

    private final SharedApiService sharedApiService;
    private final UserSavedApiService userSavedApiService;

    @PostMapping("/share")
    @Operation(summary = "API 공유 게시", description = "커스텀 API를 공유 게시판에 게시합니다.")
    public ResponseEntity<ApiResponse<SharedApi>> shareApi(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId,
            @Parameter(description = "Custom API ID", required = true) @RequestParam String customApiId,
            @Parameter(description = "플랜 타입", required = true, schema = @Schema(allowableValues = {"FREE", "PRO"})) @RequestParam PlanType planType,
            @Parameter(description = "공유할 API 이름") @RequestParam(required = false) String apiName,
            @Parameter(description = "공유할 API 설명") @RequestParam(required = false) String apiDescription) {
        
        try {
            SharedApi sharedApi = sharedApiService.shareApi(userId, customApiId, planType, apiName, apiDescription);
            return ResponseEntity.ok(ApiResponse.success(sharedApi));
            
        } catch (IllegalArgumentException e) {
            log.warn("API 공유 실패 - userId: {}, customApiId: {}, error: {}", userId, customApiId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "SHARE_API_ERROR"));
                    
        } catch (Exception e) {
            log.error("API 공유 중 예상치 못한 오류 발생 - userId: {}, customApiId: {}", userId, customApiId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("API 공유 처리 중 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
        }
    }

    @DeleteMapping("/unshare/{sharedApiId}")
    @Operation(summary = "API 공유 취소", description = "공유된 API를 취소합니다.")
    public ResponseEntity<ApiResponse<Void>> unshareApiBySharedId(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId,
            @Parameter(description = "공유 API ID", required = true) @PathVariable String sharedApiId) {
        
        try {
            sharedApiService.unshareApiBySharedId(userId, sharedApiId);
            return ResponseEntity.ok(ApiResponse.success());
            
        } catch (IllegalArgumentException e) {
            log.warn("API 공유 취소 실패 - userId: {}, sharedApiId: {}, error: {}", userId, sharedApiId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "UNSHARE_API_ERROR"));
                    
        } catch (Exception e) {
            log.error("API 공유 취소 중 예상치 못한 오류 발생 - userId: {}, sharedApiId: {}", userId, sharedApiId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("API 공유 취소 처리 중 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
        }
    }




    @GetMapping("/my")
    @Operation(summary = "내가 공유한 API 목록", description = "사용자가 공유한 API 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<SharedApi>>> getMySharedApis(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId) {
        
        List<SharedApi> sharedApis = sharedApiService.getSharedApisByCreator(userId);
        return ResponseEntity.ok(ApiResponse.success(sharedApis));
    }

    @PostMapping("/save")
    @Operation(summary = "공유 API 저장", description = "공유된 API를 내 계정으로 저장합니다.")
    public ResponseEntity<ApiResponse<UserSavedApi>> saveSharedApi(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId,
            @Parameter(description = "공유 API ID", required = true) @RequestParam String sharedApiId) {
        
        try {
            UserSavedApi savedApi = userSavedApiService.saveSharedApi(userId, sharedApiId);
            return ResponseEntity.ok(ApiResponse.success(savedApi));
            
        } catch (IllegalArgumentException e) {
            log.warn("공유 API 저장 실패 - userId: {}, sharedApiId: {}, error: {}", userId, sharedApiId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "SAVE_API_ERROR"));
                    
        } catch (Exception e) {
            log.error("공유 API 저장 중 예상치 못한 오류 발생 - userId: {}, sharedApiId: {}", userId, sharedApiId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("공유 API 저장 처리 중 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
        }
    }

    @GetMapping("/saved")
    @Operation(summary = "저장된 API 목록", description = "사용자가 저장한 공유 API 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<Page<UserSavedApi>>> getSavedApis(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<UserSavedApi> savedApis = userSavedApiService.getUserSavedApis(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(savedApis));
    }

    @DeleteMapping("/saved/{userApiId}")
    @Operation(summary = "저장된 API 삭제", description = "저장된 공유 API를 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteSavedApi(
            @Parameter(description = "사용자 ID", required = true) @PathVariable String userId,
            @Parameter(description = "사용자 API ID", required = true) @PathVariable String userApiId) {
        
        try {
            userSavedApiService.deleteSavedApi(userId, userApiId);
            return ResponseEntity.ok(ApiResponse.success());
            
        } catch (IllegalArgumentException e) {
            log.warn("저장된 API 삭제 실패 - userId: {}, userApiId: {}, error: {}", userId, userApiId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "DELETE_SAVED_API_ERROR"));
                    
        } catch (Exception e) {
            log.error("저장된 API 삭제 중 예상치 못한 오류 발생 - userId: {}, userApiId: {}", userId, userApiId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("저장된 API 삭제 처리 중 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
        }
    }
}