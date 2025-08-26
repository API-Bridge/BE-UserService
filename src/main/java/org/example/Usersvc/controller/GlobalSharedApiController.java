/* 공유 기능 비활성화
package org.example.Usersvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.common.response.ApiResponse;
import org.example.Usersvc.domain.SharedApi;
import org.example.Usersvc.service.SharedApiService;
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
@Tag(name = "GlobalSharedApi", description = "글로벌 공유 API 조회")
public class GlobalSharedApiController {

    private final SharedApiService sharedApiService;

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
}
*/
