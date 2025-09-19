package com.lostfound.capstonebackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Swagger/OpenAPI 3.0 설정 클래스
 * API 문서화와 테스트 환경을 제공합니다.
 *
 * @author Capstone Team
 * @version 1.0
 * @since 2025-09-19
 */
@Configuration
@Profile("dev")
@Slf4j
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * OpenAPI 3.0 설정을 위한 Bean 등록
     * JWT 인증을 포함한 API 문서를 생성합니다.
     *
     * @return OpenAPI 설정 객체
     */
    @Bean
    public OpenAPI openAPI() {
        log.info("Configuring OpenAPI documentation");

        // API 기본 정보 설정
        Info info = new Info()
                .title("Lost & Found System API")
                .description("""
                        서울시 분실물 통합 관리 시스템 Backend API
                        
                        ## 주요 기능
                        - 사용자 인증 (JWT 기반)
                        - 분실물/습득물 등록 및 검색
                        - 자동 매칭 시스템
                        - 실시간 알림
                        
                        ## 인증 방법
                        1. POST /api/auth/login으로 로그인
                        2. 응답으로 받은 accessToken을 복사
                        3. 우측 상단 'Authorize' 버튼 클릭
                        4. "Bearer {accessToken}" 형식으로 입력
                        
                        ## 테스트 계정
                        - Email: test@example.com
                        - Password: test123!
                        """)
                .version("v1.0.0")
                .contact(new Contact()
                        .name("Capstone Team 2025")
                        .email("capstone@example.com")
                        .url("https://github.com/capstone-team-2025"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));

        // 서버 정보 설정
        Server localServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("Local Development Server");

        Server prodServer = new Server()
                .url("https://api.lostfound.example.com")
                .description("Production Server");

        // JWT 보안 스키마 설정
        String jwtSchemeName = "bearerAuth";
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(jwtSchemeName);

        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT 토큰을 입력하세요. 'Bearer ' 접두사는 자동으로 추가됩니다."));

        OpenAPI openAPI = new OpenAPI()
                .info(info)
                .servers(List.of(localServer, prodServer))
                .addSecurityItem(securityRequirement)
                .components(components);

        log.info("OpenAPI documentation configured successfully");
        return openAPI;
    }
}
