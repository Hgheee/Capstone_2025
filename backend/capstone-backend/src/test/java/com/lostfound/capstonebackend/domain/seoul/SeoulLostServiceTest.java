package com.lostfound.capstonebackend.domain.seoul;

import com.lostfound.capstonebackend.domain.lostitem.LostItem;
import com.lostfound.capstonebackend.domain.lostitem.LostItemRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SeoulLostServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private LostItemRepository lostItemRepository;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    private SeoulLostService seoulLostService;

    @BeforeEach
    void setUp() {
        given(restTemplateBuilder.requestFactory(any(java.util.function.Supplier.class))).willReturn(restTemplateBuilder);
        given(restTemplateBuilder.build()).willReturn(restTemplate);

        // 중복 체크 Mock: 기본적으로 중복 없음으로 설정
        given(lostItemRepository.findExternalIdsByDataSourceAndExternalIdIn(
                any(LostItem.DataSource.class),
                anyList()
        )).willReturn(Collections.emptyList());

        seoulLostService = new SeoulLostService(jdbcTemplate, lostItemRepository, restTemplateBuilder, "test-api-key");
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

    @Test
    @DisplayName("서울시 분실물 수집 - 중복 데이터 건너뛰기")
    void importSeoulLostItemsSkipDuplicates() {
        // Given: 3개 중 1개는 이미 DB에 존재
        List<SeoulLostRow> rows = List.of(
                buildRow("A123", "지갑1", "2024-01-01", "2024-01-02", "보관중", "5"),
                buildRow("A124", "지갑2", "2024-01-01", "2024-01-02", "보관중", "5"),
                buildRow("A125", "지갑3", "2024-01-01", "2024-01-02", "보관중", "5")
        );
        SeoulLostResponse response = buildResponse("INFO-000", rows);
        given(restTemplate.getForEntity(anyString(), eq(SeoulLostResponse.class))).willReturn(ResponseEntity.ok(response));

        // A124는 이미 DB에 존재
        given(lostItemRepository.findExternalIdsByDataSourceAndExternalIdIn(
                eq(LostItem.DataSource.SEOUL_LOST),
                anyList()
        )).willReturn(List.of("A124"));

        ArgumentCaptor<List<LostItem>> captor = ArgumentCaptor.forClass(List.class);
        given(jdbcTemplate.batchUpdate(anyString(), anyList(), anyInt(), any())).willReturn(new int[0][]);

        // When
        int saved = seoulLostService.importSeoulLostItems();

        // Then: 중복 제외하고 2개만 저장
        assertThat(saved).isEqualTo(2);
        verify(jdbcTemplate).batchUpdate(anyString(), captor.capture(), eq(100), any());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).extracting(LostItem::getExternalId)
                .containsExactlyInAnyOrder("A123", "A125");
    }

    @Test
    @DisplayName("서울시 분실물 수집 - 모든 데이터가 중복인 경우")
    void importSeoulLostItemsAllDuplicates() {
        // Given: 모든 데이터가 이미 DB에 존재
        List<SeoulLostRow> rows = List.of(
                buildRow("A123", "지갑1", "2024-01-01", "2024-01-02", "보관중", "5"),
                buildRow("A124", "지갑2", "2024-01-01", "2024-01-02", "보관중", "5")
        );
        SeoulLostResponse response = buildResponse("INFO-000", rows);
        given(restTemplate.getForEntity(anyString(), eq(SeoulLostResponse.class))).willReturn(ResponseEntity.ok(response));

        // 모든 ID가 이미 DB에 존재
        given(lostItemRepository.findExternalIdsByDataSourceAndExternalIdIn(
                eq(LostItem.DataSource.SEOUL_LOST),
                anyList()
        )).willReturn(List.of("A123", "A124"));

        // When
        int saved = seoulLostService.importSeoulLostItems();

        // Then: 저장된 항목 없음
        assertThat(saved).isEqualTo(0);
        verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList(), anyInt(), any());
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
