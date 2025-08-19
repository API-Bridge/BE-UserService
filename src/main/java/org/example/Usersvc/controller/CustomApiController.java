package org.example.Usersvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.domain.CustomApi;
import org.example.Usersvc.service.CustomApiService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/custom-apis")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "CustomApi", description = "커스텀 API 관리")
public class CustomApiController {

    private final CustomApiService customApiService;

    @GetMapping("/my")
    @Operation(summary = "내 커스텀 API 목록", description = "사용자가 생성한 커스텀 API 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<CustomApi>>> getMyCustomApis(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId) {
        
        List<CustomApi> customApis = customApiService.getCustomApisByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(customApis));
    }

    @GetMapping
    @Operation(summary = "커스텀 API 목록 조회 (페이징)", description = "사용자의 커스텀 API 목록을 페이징으로 조회합니다.")
    public ResponseEntity<ApiResponse<Page<CustomApi>>> getCustomApis(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<CustomApi> customApis = customApiService.getCustomApisByUserId(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(customApis));
    }

    @GetMapping("/{customApiId}")
    @Operation(summary = "커스텀 API 상세 조회", description = "특정 커스텀 API의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<CustomApi>> getCustomApi(
            @Parameter(description = "사용자 ID", required = true) @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "커스텀 API ID", required = true) @PathVariable String customApiId) {
        
        CustomApi customApi = customApiService.getCustomApi(userId, customApiId);
        return ResponseEntity.ok(ApiResponse.success(customApi));
    }

    @GetMapping("/search")
    @Operation(summary = "커스텀 API 검색", description = "키워드로 커스텀 API를 검색합니다.")
    public ResponseEntity<ApiResponse<List<CustomApi>>> searchCustomApis(
            @Parameter(description = "검색 키워드", required = true) @RequestParam String keyword) {
        
        List<CustomApi> customApis = customApiService.searchCustomApis(keyword);
        return ResponseEntity.ok(ApiResponse.success(customApis));
    }
}