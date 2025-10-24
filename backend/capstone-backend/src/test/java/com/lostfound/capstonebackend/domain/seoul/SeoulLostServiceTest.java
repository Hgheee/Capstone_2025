package com.lostfound.capstonebackend.domain.seoul;

import com.lostfound.capstonebackend.domain.lostitem.LostItem;
import com.lostfound.capstonebackend.domain.seoul.dto.SeoulLostResponse;
import com.lostfound.capstonebackend.domain.seoul.dto.SeoulLostRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SeoulLostServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    private SeoulLostService seoulLostService;

    @BeforeEach
    void setUp() {
        given(restTemplateBuilder.requestFactory(any(java.util.function.Supplier.class))).willReturn(restTemplateBuilder);
        given(restTemplateBuilder.build()).willReturn(restTemplate);
        seoulLostService = new SeoulLostService(jdbcTemplate, restTemplateBuilder, "test-api-key");
    }

    @Test
    @DisplayName("서울시 분실물 수집 성공")
    void importSeoulLostItemsSuccess() {
        // Given
        SeoulLostResponse response = buildResponse("INFO-000", List.of(buildRow("A123", "지갑", "2024-01-01", "2024-01-02", "보관중", "5")));
        given(restTemplate.getForEntity(anyString(), eq(SeoulLostResponse.class))).willReturn(ResponseEntity.ok(response));
        ArgumentCaptor<List<LostItem>> captor = ArgumentCaptor.forClass(List.class);
        given(jdbcTemplate.batchUpdate(anyString(), anyList(), anyInt(), any())).willReturn(new int[0][]);

        // When
        int saved = seoulLostService.importSeoulLostItems();

        // Then
        assertThat(saved).isEqualTo(1);
        verify(jdbcTemplate).batchUpdate(anyString(), captor.capture(), eq(100), any());
        LostItem stored = captor.getValue().get(0);
        assertThat(stored.getTitle()).isEqualTo("지갑");
        assertThat(stored.getDataSource()).isEqualTo(LostItem.DataSource.SEOUL_LOST);
        assertThat(stored.getStatus()).isEqualTo(LostItem.Status.STORED);
    }

    @Test
    @DisplayName("서울시 분실물 수집 실패 - 응답 코드 비정상")
    void importSeoulLostItemsFailure() {
        // Given
        SeoulLostResponse response = buildResponse("ERROR", List.of(buildRow("A123", "지갑", "2024-01-01", "2024-01-02", "보관", "5")));
        given(restTemplate.getForEntity(anyString(), eq(SeoulLostResponse.class))).willReturn(ResponseEntity.ok(response));

        // When
        int saved = seoulLostService.importSeoulLostItems();

        // Then
        assertThat(saved).isEqualTo(0);
        verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList(), anyInt(), any());
    }

    @Test
    @DisplayName("서울시 분실물 수집 실패 - 응답 Null")
    void importSeoulLostItemsNullResponse() {
        given(restTemplate.getForEntity(anyString(), eq(SeoulLostResponse.class))).willReturn(ResponseEntity.ok(null));

        int saved = seoulLostService.importSeoulLostItems();

        assertThat(saved).isEqualTo(0);
        verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList(), anyInt(), any());
    }

    @Test
    @DisplayName("서울시 분실물 수집 실패 - API 예외")
    void importSeoulLostItemsApiException() {
        given(restTemplate.getForEntity(anyString(), eq(SeoulLostResponse.class)))
                .willThrow(new RestClientException("network error"));

        int saved = seoulLostService.importSeoulLostItems();

        assertThat(saved).isEqualTo(0);
        verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList(), anyInt(), any());
    }

    @Test
    @DisplayName("서울시 분실물 수집 실패 - 데이터 없음")
    void importSeoulLostItemsEmptyRows() {
        SeoulLostResponse response = buildResponse("INFO-000", List.of());
        given(restTemplate.getForEntity(anyString(), eq(SeoulLostResponse.class))).willReturn(ResponseEntity.ok(response));

        int saved = seoulLostService.importSeoulLostItems();

        assertThat(saved).isEqualTo(0);
        verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList(), anyInt(), any());
    }

    @Test
    @DisplayName("서울시 분실물 수집 성공 - 대용량 배치")
    void importSeoulLostItemsLargeBatch() {
        List<SeoulLostRow> rows = new ArrayList<>();
        IntStream.range(0, 150).forEach(i -> rows.add(buildRow("ID" + i, "제목" + i, "2024-01-01", "2024-01-02", "보관중", String.valueOf(i))));
        SeoulLostResponse response = buildResponse("INFO-000", rows);
        given(restTemplate.getForEntity(anyString(), eq(SeoulLostResponse.class))).willReturn(ResponseEntity.ok(response));
        ArgumentCaptor<List<LostItem>> captor = ArgumentCaptor.forClass(List.class);
        given(jdbcTemplate.batchUpdate(anyString(), anyList(), anyInt(), any())).willReturn(new int[0][]);

        int saved = seoulLostService.importSeoulLostItems();

        assertThat(saved).isEqualTo(150);
        verify(jdbcTemplate).batchUpdate(anyString(), captor.capture(), eq(100), any());
        assertThat(captor.getValue()).hasSize(150);
    }

    @Test
    @DisplayName("서울시 분실물 수집 성공 - 저장 데이터 검증")
    void importSeoulLostItemsVerifyStoredData() {
        SeoulLostRow row = buildRow("B999", "가방", "2024-02-01", "2024-02-02", "보관", "10");
        SeoulLostResponse response = buildResponse("INFO-000", List.of(row));
        given(restTemplate.getForEntity(anyString(), eq(SeoulLostResponse.class))).willReturn(ResponseEntity.ok(response));
        ArgumentCaptor<List<LostItem>> captor = ArgumentCaptor.forClass(List.class);
        given(jdbcTemplate.batchUpdate(anyString(), anyList(), anyInt(), any())).willReturn(new int[0][]);

        seoulLostService.importSeoulLostItems();

        verify(jdbcTemplate).batchUpdate(anyString(), captor.capture(), eq(100), any());
        LostItem stored = captor.getValue().get(0);
        assertThat(stored.getExternalId()).isEqualTo("B999");
        assertThat(stored.getTitle()).isEqualTo("가방");
        assertThat(stored.getReceivedDate()).isNotNull();
        assertThat(stored.getViewCount()).isEqualTo(10);
    }

    private SeoulLostResponse buildResponse(String code, List<SeoulLostRow> rows) {
        SeoulLostResponse response = new SeoulLostResponse();
        SeoulLostResponse.LostArticleInfo info = new SeoulLostResponse.LostArticleInfo();
        SeoulLostResponse.Result result = new SeoulLostResponse.Result();
        result.setCode(code);
        result.setMessage("OK");
        info.setResult(result);
        info.setRow(rows);
        response.setLostArticleInfo(info);
        return response;
    }

    private SeoulLostRow buildRow(String id, String title, String regYmd, String rcvYmd, String status, String viewCount) {
        SeoulLostRow row = new SeoulLostRow();
        row.setLostMngNo(id);
        row.setLostNm(title);
        row.setLgsDtlCn("설명");
        row.setLostKnd("지갑/카드");
        row.setCstdPlc("서울역");
        row.setRegYmd(regYmd);
        row.setRcvYmd(rcvYmd);
        row.setRcpl("서울역");
        row.setLostStts(status);
        row.setInqCnt(viewCount);
        return row;
    }
}
