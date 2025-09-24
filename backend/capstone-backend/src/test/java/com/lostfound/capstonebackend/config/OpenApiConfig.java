package com.lostfound.capstonebackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(OpenAPI) 설정.
 * - Bearer(JWT) 인증 스키마 등록
 * - Swagger UI에서 Authorize 버튼으로 토큰 입력 가능
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI api() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Lost & Found API")
                        .description("캡스톤 분실물 관리 백엔드 API 문서")
                        .version("v1"))
                // 전역으로 Bearer 인증 요구 (로그인/회원가입만 예외 처리할 수 있음)
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(
                                securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }
}
