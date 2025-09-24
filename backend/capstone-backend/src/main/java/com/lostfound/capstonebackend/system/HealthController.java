package com.lostfound.capstonebackend.system;

import com.lostfound.capstonebackend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 애플리케이션의 상태를 확인하기 위한 헬스 체크(Health Check) API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "System", description = "시스템 관리 API")
public class HealthController {

    /**
     * 시스템이 정상적으로 실행 중인지 확인하는 헬스 체크 엔드포인트입니다.
     * "Lost & Found API is healthy" 메시지를 포함한 성공 응답을 반환합니다.
     * @return 시스템 상태 메시지를 담은 ApiResponse
     */
    @GetMapping("/health")
    @Operation(summary = "API 헬스 체크", description = "API 서버가 정상적으로 동작하는지 확인합니다.")
    public ApiResponse<String> health() {
        return ApiResponse.ok("Lost & Found API is healthy");
    }
}
