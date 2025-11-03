package com.lostfound.capstonebackend.domain.lost112;

import com.lostfound.capstonebackend.domain.lost112.dto.Lost112ItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Lost112JavaImportService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class Lost112JavaImportServiceTest {

    @Mock
    private Lost112ApiService lost112ApiService;

    @Mock
    private Lost112TempRepository lost112TempRepository;

    private Lost112JavaImportService importService;

    @BeforeEach
    void setUp() {
        importService = new Lost112JavaImportService(lost112ApiService, lost112TempRepository);
    }

    @Test
    @DisplayName("데이터 수집 및 업서트 성공 - 신규 데이터")
    void collectAndUpsertNewData() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        String regionCode = "01";

        Lost112ItemDto dto = createTestDto("A1", "지갑", "20240101");
        List<Lost112ItemDto> items = List.of(dto);

        when(lost112ApiService.fetchAll(startDate, endDate, regionCode)).thenReturn(items);
        when(lost112TempRepository.findByItemId("A1")).thenReturn(Optional.empty());
        when(lost112TempRepository.save(any(Lost112TempEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Lost112JavaImportService.CollectionSummary result =
            importService.collectAndUpsert(startDate, endDate, regionCode);

        // Then
        assertThat(result.totalFetched()).isEqualTo(1);
        assertThat(result.insertedCount()).isEqualTo(1);
        assertThat(result.updatedCount()).isEqualTo(0);
        assertThat(result.startDate()).isEqualTo(startDate);
        assertThat(result.endDate()).isEqualTo(endDate);

        verify(lost112ApiService).fetchAll(startDate, endDate, regionCode);
        verify(lost112TempRepository).findByItemId("A1");
        verify(lost112TempRepository).save(any(Lost112TempEntity.class));
    }

    @Test
    @DisplayName("데이터 수집 및 업서트 성공 - 기존 데이터 갱신")
    void collectAndUpsertExistingData() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        String regionCode = "";

        Lost112ItemDto dto = createTestDto("A1", "지갑", "20240101");
        List<Lost112ItemDto> items = List.of(dto);

        Lost112TempEntity existingEntity = Lost112TempEntity.builder()
            .id(1L)
            .itemId("A1")
            .title("구 지갑")
            .build();

        when(lost112ApiService.fetchAll(startDate, endDate, regionCode)).thenReturn(items);
        when(lost112TempRepository.findByItemId("A1")).thenReturn(Optional.of(existingEntity));
        when(lost112TempRepository.save(any(Lost112TempEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Lost112JavaImportService.CollectionSummary result =
            importService.collectAndUpsert(startDate, endDate, regionCode);

        // Then
        assertThat(result.totalFetched()).isEqualTo(1);
        assertThat(result.insertedCount()).isEqualTo(0);
        assertThat(result.updatedCount()).isEqualTo(1);

        verify(lost112TempRepository).save(existingEntity);
        assertThat(existingEntity.getTitle()).isEqualTo("지갑");
    }

    @Test
    @DisplayName("빈 데이터 수집 처리")
    void collectAndUpsertEmptyData() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        String regionCode = null;

        when(lost112ApiService.fetchAll(startDate, endDate, "")).thenReturn(Collections.emptyList());

        // When
        Lost112JavaImportService.CollectionSummary result =
            importService.collectAndUpsert(startDate, endDate, regionCode);

        // Then
        assertThat(result.totalFetched()).isEqualTo(0);
        assertThat(result.insertedCount()).isEqualTo(0);
        assertThat(result.updatedCount()).isEqualTo(0);

        verify(lost112TempRepository, never()).save(any());
    }

    @Test
    @DisplayName("날짜 매개변수 null 처리")
    void collectAndUpsertWithNullDates() {
        // Given
        LocalDate today = LocalDate.now();
        when(lost112ApiService.fetchAll(today, today, "")).thenReturn(Collections.emptyList());

        // When
        Lost112JavaImportService.CollectionSummary result =
            importService.collectAndUpsert(null, null, null);

        // Then
        assertThat(result.startDate()).isEqualTo(today);
        assertThat(result.endDate()).isEqualTo(today);
        assertThat(result.regionCode()).isEmpty();

        verify(lost112ApiService).fetchAll(today, today, "");
    }

    @Test
    @DisplayName("시작일이 종료일보다 큰 경우 자동 스왑")
    void collectAndUpsertWithReversedDates() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 31);
        LocalDate endDate = LocalDate.of(2024, 1, 1);

        when(lost112ApiService.fetchAll(endDate, startDate, "")).thenReturn(Collections.emptyList());

        // When
        Lost112JavaImportService.CollectionSummary result =
            importService.collectAndUpsert(startDate, endDate, "");

        // Then
        assertThat(result.startDate()).isEqualTo(endDate);
        assertThat(result.endDate()).isEqualTo(startDate);

        verify(lost112ApiService).fetchAll(endDate, startDate, "");
    }

    @Test
    @DisplayName("카테고리 분할 처리 - 대분류>소분류")
    void collectAndUpsertWithCategorySplit() {
        // Given
        LocalDate today = LocalDate.now();
        Lost112ItemDto dto = createTestDto("A1", "지갑", "20240101");
        dto.setPrdtClNm("가방>지갑");

        when(lost112ApiService.fetchAll(today, today, "")).thenReturn(List.of(dto));
        when(lost112TempRepository.findByItemId("A1")).thenReturn(Optional.empty());
        when(lost112TempRepository.save(any(Lost112TempEntity.class)))
            .thenAnswer(invocation -> {
                Lost112TempEntity entity = invocation.getArgument(0);
                assertThat(entity.getCategory()).isEqualTo("가방");
                assertThat(entity.getSubcategory()).isEqualTo("지갑");
                return entity;
            });

        // When
        importService.collectAndUpsert(today, today, "");

        // Then
        verify(lost112TempRepository).save(any(Lost112TempEntity.class));
    }

    @Test
    @DisplayName("여러 아이템 일괄 처리")
    void collectAndUpsertMultipleItems() {
        // Given
        LocalDate today = LocalDate.now();
        List<Lost112ItemDto> items = List.of(
            createTestDto("A1", "지갑", "20240101"),
            createTestDto("A2", "핸드폰", "20240102"),
            createTestDto("A3", "가방", "20240103")
        );

        when(lost112ApiService.fetchAll(today, today, "")).thenReturn(items);
        when(lost112TempRepository.findByItemId(anyString())).thenReturn(Optional.empty());
        when(lost112TempRepository.save(any(Lost112TempEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Lost112JavaImportService.CollectionSummary result =
            importService.collectAndUpsert(today, today, "");

        // Then
        assertThat(result.totalFetched()).isEqualTo(3);
        assertThat(result.insertedCount()).isEqualTo(3);
        assertThat(result.updatedCount()).isEqualTo(0);

        verify(lost112TempRepository, times(3)).save(any(Lost112TempEntity.class));
    }

    @Test
    @DisplayName("null atcId 처리 - 스킵")
    void collectAndUpsertWithNullAtcId() {
        // Given
        LocalDate today = LocalDate.now();
        Lost112ItemDto invalidDto = new Lost112ItemDto();
        invalidDto.setAtcId(null);

        when(lost112ApiService.fetchAll(today, today, "")).thenReturn(List.of(invalidDto));

        // When
        Lost112JavaImportService.CollectionSummary result =
            importService.collectAndUpsert(today, today, "");

        // Then
        assertThat(result.totalFetched()).isEqualTo(1);
        assertThat(result.insertedCount()).isEqualTo(0);
        assertThat(result.updatedCount()).isEqualTo(0);

        verify(lost112TempRepository, never()).save(any());
    }

    @Test
    @DisplayName("날짜 파싱 성공 - YYYYMMDD 형식")
    void collectAndUpsertWithBasicDateFormat() {
        // Given
        LocalDate today = LocalDate.now();
        Lost112ItemDto dto = createTestDto("A1", "지갑", "20240115");

        when(lost112ApiService.fetchAll(today, today, "")).thenReturn(List.of(dto));
        when(lost112TempRepository.findByItemId("A1")).thenReturn(Optional.empty());
        when(lost112TempRepository.save(any(Lost112TempEntity.class)))
            .thenAnswer(invocation -> {
                Lost112TempEntity entity = invocation.getArgument(0);
                assertThat(entity.getFoundDate()).isEqualTo(LocalDate.of(2024, 1, 15));
                return entity;
            });

        // When
        importService.collectAndUpsert(today, today, "");

        // Then
        verify(lost112TempRepository).save(any(Lost112TempEntity.class));
    }

    @Test
    @DisplayName("날짜 파싱 성공 - YYYY-MM-DD 형식")
    void collectAndUpsertWithIsoDateFormat() {
        // Given
        LocalDate today = LocalDate.now();
        Lost112ItemDto dto = createTestDto("A1", "지갑", "2024-01-15");

        when(lost112ApiService.fetchAll(today, today, "")).thenReturn(List.of(dto));
        when(lost112TempRepository.findByItemId("A1")).thenReturn(Optional.empty());
        when(lost112TempRepository.save(any(Lost112TempEntity.class)))
            .thenAnswer(invocation -> {
                Lost112TempEntity entity = invocation.getArgument(0);
                assertThat(entity.getFoundDate()).isEqualTo(LocalDate.of(2024, 1, 15));
                return entity;
            });

        // When
        importService.collectAndUpsert(today, today, "");

        // Then
        verify(lost112TempRepository).save(any(Lost112TempEntity.class));
    }

    private Lost112ItemDto createTestDto(String atcId, String name, String date) {
        Lost112ItemDto dto = new Lost112ItemDto();
        dto.setAtcId(atcId);
        dto.setFdPrdtNm(name);
        dto.setFdSbjt(name + " 설명");
        dto.setFdYmd(date);
        dto.setPrdtClNm("기타");
        dto.setClrNm("검정");
        dto.setDepPlace("서울경찰서");
        dto.setFdFilePathImg("http://image.jpg");
        return dto;
    }
}
