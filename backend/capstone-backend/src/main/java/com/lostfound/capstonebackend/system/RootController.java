package com.lostfound.capstonebackend.system;

import com.lostfound.capstonebackend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 애플리케이션의 루트 경로("/") 요청을 처리하는 컨트롤러입니다.
 */
@RestController
@Hidden // Swagger 문서에 표시되지 않도록 설정
public class RootController {

    /**
     * 루트 URL에 대한 GET 요청을 처리합니다.
     * 서버가 실행 중임을 알리는 간단한 상태 메시지를 반환합니다.
     * @return 서버 상태 메시지를 담은 ApiResponse
     */
    @GetMapping("/")
    public ApiResponse<String> root() {
        return ApiResponse.ok("Lost & Found Backend API v1.0 - Server is running");
    }
}
