package com.lostfound.capstonebackend.domain.lostitem;

import com.lostfound.capstonebackend.common.exception.GlobalExceptionHandler;
import com.lostfound.capstonebackend.common.util.JwtUtils;
import com.lostfound.capstonebackend.config.JwtAuthenticationFilter;
import com.lostfound.capstonebackend.config.SecurityConfig;
import com.lostfound.capstonebackend.config.TestSecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LostItemStatisticsController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class, LostItemStatisticsControllerTest.MockConfig.class})
class LostItemStatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LostItemStatisticsService statisticsService;

    @Autowired
    private JwtUtils jwtUtils;

    @AfterEach
    void tearDown() {
        reset(statisticsService, jwtUtils);
    }

    @Test
    @DisplayName("카테고리별 통계")
    void getCategoryStatistics() throws Exception {
        given(statisticsService.getCategoryStatistics()).willReturn(Map.of("지갑", 5L));

        mockMvc.perform(get("/api/lost-items/statistics/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.지갑").value(5));
    }

    @Test
    @DisplayName("상태별 통계")
    void getStatusStatistics() throws Exception {
        given(statisticsService.getStatusStatistics()).willReturn(Map.of("FOUND", 3L));

        mockMvc.perform(get("/api/lost-items/statistics/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.FOUND").value(3));
    }

    @Test
    @DisplayName("데이터소스 통계")
    void getDataSourceStatistics() throws Exception {
        given(statisticsService.getDataSourceStatistics()).willReturn(Map.of("USER", 10L));

        mockMvc.perform(get("/api/lost-items/statistics/datasource"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.USER").value(10));
    }

    @Test
    @DisplayName("인기 카테고리")
    void getTopCategories() throws Exception {
        given(statisticsService.getTopCategories(5)).willReturn(Map.of("지갑", 5L));

        mockMvc.perform(get("/api/lost-items/statistics/top-categories")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.지갑").value(5));
    }

    @Test
    @DisplayName("월별 통계")
    void getMonthlyStatistics() throws Exception {
        given(statisticsService.getMonthlyStatistics()).willReturn(Map.of("202401", 12L));

        mockMvc.perform(get("/api/lost-items/statistics/monthly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data['202401']").value(12));
    }

    @Test
    @DisplayName("기간별 등록 수")
    void getCountByDateRange() throws Exception {
        given(statisticsService.getCountByDateRange(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(7L);

        mockMvc.perform(get("/api/lost-items/statistics/count-by-range")
                        .param("startDate", "2024-01-01T00:00:00")
                        .param("endDate", "2024-01-07T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(7));
    }

    @Test
    @DisplayName("종합 통계")
    void getOverallStatistics() throws Exception {
        given(statisticsService.getOverallStatistics()).willReturn(Map.of(
                "totalItems", 20L,
                "monthlyStats", Map.of("202401", 5L)
        ));

        mockMvc.perform(get("/api/lost-items/statistics/overall"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(20));
    }

    @TestConfiguration
    static class MockConfig {

        @Bean
        @Primary
        LostItemStatisticsService statisticsService() {
            return mock(LostItemStatisticsService.class);
        }

        @Bean
        @Primary
        JwtUtils jwtUtils() {
            return mock(JwtUtils.class);
        }
    }
}
