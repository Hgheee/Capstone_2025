package com.lostfound.capstonebackend.domain.lostitem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.capstonebackend.common.exception.BusinessException;
import com.lostfound.capstonebackend.common.exception.ErrorCode;
import com.lostfound.capstonebackend.common.exception.GlobalExceptionHandler;
import com.lostfound.capstonebackend.common.util.JwtUtils;
import com.lostfound.capstonebackend.config.JwtAuthenticationFilter;
import com.lostfound.capstonebackend.config.SecurityConfig;
import com.lostfound.capstonebackend.config.TestSecurityConfig;
import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemRequest;
import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = LostItemController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class, LostItemControllerTest.MockConfig.class})
class LostItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LostItemService lostItemService;

    @Autowired
    private JwtUtils jwtUtils;

    @AfterEach
    void tearDown() {
        reset(lostItemService, jwtUtils);
    }

    private LostItemResponse sampleResponse(Long id, String title) {
        return new LostItemResponse(
                id,
                title,
                "설명",
                "카테고리",
                "서울",
                "강남구",  // region 필드 추가
                LocalDate.now(),
                LostItem.Status.FOUND.name(),
                null,
                LostItem.DataSource.USER.name(),
                "빨강",
                "보관소",
                null,
                "홍길동",
                0,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "FOUND",  // itemType
                null,     // latitude
                null      // longitude
        );
    }

    @Test
    @DisplayName("분실물 목록 조회")
    void getAllItems() throws Exception {
        // Given
        Page<LostItemResponse> page = new PageImpl<>(List.of(sampleResponse(1L, "지갑")));
        given(lostItemService.findAll(any(Pageable.class))).willReturn(page);

        // When & Then
        mockMvc.perform(get("/api/lost-items")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("지갑"));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(lostItemService).findAll(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    @DisplayName("분실물 상세 조회")
    void getItemById() throws Exception {
        // Given
        given(lostItemService.findById(1L)).willReturn(sampleResponse(1L, "휴대폰"));

        // When & Then
        mockMvc.perform(get("/api/lost-items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("휴대폰"));
    }

    @Test
    @DisplayName("분실물 등록")
    void createItem() throws Exception {
        // Given
        LostItemRequest request = new LostItemRequest("지갑", "설명", "카테고리", "서울역", LocalDate.now(), "검정", "보관", null);
        given(lostItemService.create(any(LostItemRequest.class), eq("user@example.com")))
                .willReturn(sampleResponse(10L, "지갑"));

        // When & Then
        mockMvc.perform(post("/api/lost-items")
                        .with(SecurityMockMvcRequestPostProcessors.user("user@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10L));
    }

    @Test
    @DisplayName("분실물 등록 실패 - 권한 없음")
    void createItemFailure() throws Exception {
        // Given
        LostItemRequest request = new LostItemRequest("지갑", "설명", "카테고리", "서울역", LocalDate.now(), "검정", "보관", null);
        given(lostItemService.create(any(LostItemRequest.class), eq("user@example.com")))
                .willThrow(new BusinessException(ErrorCode.ACCESS_DENIED));

        // When & Then
        mockMvc.perform(post("/api/lost-items")
                        .with(SecurityMockMvcRequestPostProcessors.user("user@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.ACCESS_DENIED.getCode()));
    }

    @Test
    @DisplayName("분실물 수정")
    void updateItem() throws Exception {
        // Given
        LostItemRequest request = new LostItemRequest("열쇠", "설명", "카테고리", "강남", LocalDate.now(), "은색", null, null);
        given(lostItemService.update(eq(5L), any(LostItemRequest.class), eq("user@example.com")))
                .willReturn(sampleResponse(5L, "열쇠"));

        // When & Then
        mockMvc.perform(put("/api/lost-items/5")
                        .with(SecurityMockMvcRequestPostProcessors.user("user@example.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("열쇠"));
    }

    @Test
    @DisplayName("분실물 삭제")
    void deleteItem() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/lost-items/7")
                        .with(SecurityMockMvcRequestPostProcessors.user("user@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(lostItemService).delete(7L, "user@example.com");
    }

    @Test
    @DisplayName("내 분실물 목록 조회")
    void getMyItems() throws Exception {
        // Given
        Page<LostItemResponse> page = new PageImpl<>(List.of(sampleResponse(1L, "지갑")), PageRequest.of(0, 20), 1);
        given(lostItemService.findMyItems(eq("user@example.com"), any(Pageable.class))).willReturn(page);

        // When & Then
        mockMvc.perform(get("/api/lost-items/my")
                        .with(SecurityMockMvcRequestPostProcessors.user("user@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("지갑"));
    }

    @Test
    @DisplayName("키워드 검색")
    void searchItems() throws Exception {
        // Given
        Page<LostItemResponse> page = new PageImpl<>(List.of(sampleResponse(2L, "우산")));
        given(lostItemService.searchByKeyword(eq("우산"), any(Pageable.class))).willReturn(page);

        // When & Then
        mockMvc.perform(get("/api/lost-items/search")
                        .param("keyword", "우산"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("우산"));
    }

    @Test
    @DisplayName("고급 검색")
    void advancedSearch() throws Exception {
        // Given
        Page<LostItemResponse> page = new PageImpl<>(List.of(sampleResponse(3L, "노트북")));
        given(lostItemService.searchComplex(eq("노트북"), eq("전자제품"), eq("FOUND"), any(), any(), any(Pageable.class)))
                .willReturn(page);

        // When & Then
        mockMvc.perform(get("/api/lost-items/search/advanced")
                        .param("keyword", "노트북")
                        .param("category", "전자제품")
                        .param("status", "FOUND"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("노트북"));
    }

    @Test
    @DisplayName("카테고리별 조회")
    void findByCategory() throws Exception {
        // Given
        Page<LostItemResponse> page = new PageImpl<>(List.of(sampleResponse(4L, "카드")));
        given(lostItemService.findByCategory(eq("카드"), any(Pageable.class))).willReturn(page);

        // When & Then
        mockMvc.perform(get("/api/lost-items/category/카드"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("카드"));
    }
    @Test
    @DisplayName("최근 분실물 10건 조회")
    void getRecentItems() throws Exception {
        // Given
        given(lostItemService.findRecentItems()).willReturn(List.of(sampleResponse(1L, "우산")));

        // When & Then
        mockMvc.perform(get("/api/lost-items/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("우산"));
    }

    @Test
    @DisplayName("분실물 상태 변경")
    void updateStatus() throws Exception {
        // Given
        given(lostItemService.updateStatus(3L, "CLAIMED", "user@example.com"))
                .willReturn(sampleResponse(3L, "지갑"));

        // When & Then
        mockMvc.perform(patch("/api/lost-items/3/status")
                        .with(SecurityMockMvcRequestPostProcessors.user("user@example.com"))
                        .param("status", "CLAIMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(3L));
    }

    @Test
    @DisplayName("지역별 검색")
    void searchByRegion() throws Exception {
        // Given
        Page<LostItemResponse> page = new PageImpl<>(List.of(sampleResponse(6L, "장갑")));
        given(lostItemService.searchByRegion(eq("서울"), any(Pageable.class))).willReturn(page);

        // When & Then
        mockMvc.perform(get("/api/lost-items/search/region")
                        .param("region", "서울"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("장갑"));
    }

    @Test
    @DisplayName("색상별 검색")
    void searchByColor() throws Exception {
        // Given
        Page<LostItemResponse> page = new PageImpl<>(List.of(sampleResponse(7L, "모자")));
        given(lostItemService.searchByColor(eq("파랑"), any(Pageable.class))).willReturn(page);

        // When & Then
        mockMvc.perform(get("/api/lost-items/search/color")
                        .param("color", "파랑"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("모자"));
    }

    @Test
    @DisplayName("전체 텍스트 검색")
    void searchByFullText() throws Exception {
        // Given
        Page<LostItemResponse> page = new PageImpl<>(List.of(sampleResponse(8L, "카메라")));
        given(lostItemService.searchByFullText(eq("카메라"), any(Pageable.class))).willReturn(page);

        // When & Then
        mockMvc.perform(get("/api/lost-items/search/fulltext")
                        .param("text", "카메라"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("카메라"));
    }

    @Test
    @DisplayName("상태별 최근 분실물")
    void getRecentItemsByStatus() throws Exception {
        // Given
        given(lostItemService.findRecentItemsByStatus("FOUND", 5))
                .willReturn(List.of(sampleResponse(9L, "시계")));

        // When & Then
        mockMvc.perform(get("/api/lost-items/recent/status/FOUND")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("시계"));
    }

    @Test
    @DisplayName("기간별 검색")
    void searchByDateRange() throws Exception {
        // Given
        Page<LostItemResponse> page = new PageImpl<>(List.of(sampleResponse(10L, "키")));
        given(lostItemService.findByDateRange(any(), any(), any(Pageable.class))).willReturn(page);

        // When & Then
        mockMvc.perform(get("/api/lost-items/search/daterange")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("키"));
    }

    @Test
    @DisplayName("내 분실물 상태 통계")
    void getMyStatusStatistics() throws Exception {
        // Given
        List<Object[]> stats = List.of(new Object[]{"FOUND", 3L}, new Object[]{"CLAIMED", 1L});
        given(lostItemService.getMyStatusStatistics("user@example.com")).willReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/lost-items/my/statistics")
                        .with(SecurityMockMvcRequestPostProcessors.user("user@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0][0]").value("FOUND"))
                .andExpect(jsonPath("$.data[0][1]").value(3));
    }

    @TestConfiguration
    static class MockConfig {

        @Bean
        @Primary
        LostItemService lostItemService() {
            return mock(LostItemService.class);
        }

        @Bean
        @Primary
        JwtUtils jwtUtils() {
            return mock(JwtUtils.class);
        }
    }
}
