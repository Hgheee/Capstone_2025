package com.lostfound.capstonebackend.common.exception;

import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.lostfound.capstonebackend.common.util.JwtUtils;
import com.lostfound.capstonebackend.config.JwtAuthenticationFilter;
import com.lostfound.capstonebackend.config.SecurityConfig;
import com.lostfound.capstonebackend.config.TestSecurityConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.validation.Valid;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class, GlobalExceptionHandlerTest.TestController.class, GlobalExceptionHandlerTest.MockConfig.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("BusinessException 처리")
    void handleBusinessException() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.ACCESS_DENIED.getCode()));
    }

    @Test
    @DisplayName("검증 실패 처리")
    void handleMethodArgumentNotValid() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
    }

    @Test
    @DisplayName("JSON 파싱 오류 처리")
    void handleNotReadable() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
    }

    @Test
    @DisplayName("파라미터 타입 불일치 처리")
    void handleTypeMismatch() throws Exception {
        mockMvc.perform(get("/test/type-mismatch")
                        .param("count", "not-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
    }

    @RestController
    @Validated
    static class TestController {

        @GetMapping("/test/business")
        void triggerBusiness() {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        @PostMapping("/test/validate")
        void validate(@RequestBody @Valid SampleRequest request) {
        }

        @GetMapping("/test/type-mismatch")
        void typeMismatch(@RequestParam int count) {
        }
    }

    record SampleRequest(@NotBlank(message = "name은 필수입니다.") String name) {}

    @TestConfiguration
    static class MockConfig {

        @Bean
        @Primary
        JwtUtils jwtUtils() {
            return mock(JwtUtils.class);
        }
    }
}
