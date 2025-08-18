package org.example.Usersvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/shared-apis")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SharedApi", description = "공유 API 관리")
public class SharedApiController {

    private final SharedApiService sharedApiService;
    private final UserSavedApiService userSavedApiService;

    @PostMapping("/share")
    @Operation(summary = "API 공유 게시", description = "커스텀 API를 공유 게시판에 게시합니다.")
    public ResponseEntity<ApiResponse<SharedApi>> shareApi(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "Custom API ID", required = true) @RequestParam String customApiId,
            @Parameter(description = "플랜 타입", required = true) @RequestParam String planType) {
        
        SharedApi sharedApi = sharedApiService.shareApi(userId, customApiId, planType);
        return ResponseEntity.ok(ApiResponse.success(sharedApi));
    }

    @DeleteMapping("/unshare")
    @Operation(summary = "API 공유 취소", description = "공유된 API를 취소합니다.")
    public ResponseEntity<ApiResponse<Void>> unshareApi(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "Custom API ID", required = true) @RequestParam String customApiId) {
        
        sharedApiService.unshareApi(userId, customApiId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping
    @Operation(summary = "공유 API 목록 조회", description = "활성화된 공유 API 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<Page<SharedApi>>> getSharedApis(
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<SharedApi> sharedApis = sharedApiService.getActiveSharedApis(pageable);
        return ResponseEntity.ok(ApiResponse.success(sharedApis));
    }

    @GetMapping("/search")
    @Operation(summary = "공유 API 검색", description = "키워드로 공유 API를 검색합니다.")
    public ResponseEntity<ApiResponse<List<SharedApi>>> searchSharedApis(
            @Parameter(description = "검색 키워드", required = true) @RequestParam String keyword) {
        
        List<SharedApi> sharedApis = sharedApiService.searchSharedApis(keyword);
        return ResponseEntity.ok(ApiResponse.success(sharedApis));
    }

    @GetMapping("/my")
    @Operation(summary = "내가 공유한 API 목록", description = "사용자가 공유한 API 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<SharedApi>>> getMySharedApis(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        List<SharedApi> sharedApis = sharedApiService.getSharedApisByCreator(userId);
        return ResponseEntity.ok(ApiResponse.success(sharedApis));
    }

    @PostMapping("/save")
    @Operation(summary = "공유 API 저장", description = "공유된 API를 내 계정으로 저장합니다.")
    public ResponseEntity<ApiResponse<UserSavedApi>> saveSharedApi(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "공유 API ID", required = true) @RequestParam String sharedApiId) {
        
        UserSavedApi savedApi = userSavedApiService.saveSharedApi(userId, sharedApiId);
        return ResponseEntity.ok(ApiResponse.success(savedApi));
    }

    @GetMapping("/saved")
    @Operation(summary = "저장된 API 목록", description = "사용자가 저장한 공유 API 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<Page<UserSavedApi>>> getSavedApis(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<UserSavedApi> savedApis = userSavedApiService.getUserSavedApis(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(savedApis));
    }

    @DeleteMapping("/saved/{userApiId}")
    @Operation(summary = "저장된 API 삭제", description = "저장된 공유 API를 삭제합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteSavedApi(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "사용자 API ID", required = true) @PathVariable String userApiId) {
        
        userSavedApiService.deleteSavedApi(userId, userApiId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}