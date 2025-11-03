package com.lostfound.capstonebackend.domain.lost112;

import com.lostfound.capstonebackend.domain.lost112.dto.Lost112ItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Python 스크립트 의존 없이 순수 자바 코드로 LOST112 데이터를 수집하고 임시 테이블에 적재하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Lost112JavaImportService {

    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final Lost112ApiService lost112ApiService;
    private final Lost112TempRepository lost112TempRepository;

    /**
     * 지정한 기간과 지역에 대해 LOST112 데이터를 수집하고 임시 테이블에 업서트한다.
     *
     * @param startDate 수집 시작일 (null 이면 endDate 또는 오늘)
     * @param endDate   수집 종료일 (null 이면 startDate 또는 오늘)
     * @param regionCode 지역 코드 (null 또는 공백이면 전체)
     * @return 수집/저장 요약 정보
     */
    @Transactional
    public CollectionSummary collectAndUpsert(LocalDate startDate,
                                              LocalDate endDate,
                                              String regionCode) {
        LocalDate effectiveEnd = Optional.ofNullable(endDate).orElse(LocalDate.now());
        LocalDate effectiveStart = Optional.ofNullable(startDate).orElse(effectiveEnd);

        if (effectiveStart.isAfter(effectiveEnd)) {
            LocalDate temp = effectiveStart;
            effectiveStart = effectiveEnd;
            effectiveEnd = temp;
        }

        String normalizedRegion = Optional.ofNullable(regionCode)
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .orElse("");

        log.info("LOST112 순수 자바 수집 시작 - start={}, end={}, region={}",
                effectiveStart, effectiveEnd, normalizedRegion.isEmpty() ? "ALL" : normalizedRegion);

        List<Lost112ItemDto> items = lost112ApiService.fetchAll(effectiveStart, effectiveEnd, normalizedRegion);

        int inserted = 0;
        int updated = 0;

        for (Lost112ItemDto dto : items) {
            String itemId = dto == null ? null : clean(dto.getAtcId());
            if (itemId == null) {
                continue;
            }

            Lost112TempEntity entity = lost112TempRepository.findByItemId(itemId)
                    .orElseGet(() -> Lost112TempEntity.builder().itemId(itemId).build());

            boolean isNew = entity.getId() == null;

            populateEntity(entity, dto);
            lost112TempRepository.save(entity);

            if (isNew) {
                inserted++;
            } else {
                updated++;
            }
        }

        log.info("LOST112 순수 자바 수집 완료 - 기간 {}~{}, 지역 '{}', 총 {}, 신규 {}, 갱신 {}",
                effectiveStart, effectiveEnd,
                normalizedRegion.isEmpty() ? "ALL" : normalizedRegion,
                items.size(), inserted, updated);

        return new CollectionSummary(effectiveStart, effectiveEnd, normalizedRegion, items.size(), inserted, updated);
    }

    private void populateEntity(Lost112TempEntity entity, Lost112ItemDto dto) {
        entity.setTitle(truncate(clean(dto.getFdPrdtNm()), 500));
        entity.setFoundDate(parseFoundDate(dto.getFdYmd()));
        entity.setStoragePlace(truncate(clean(dto.getDepPlace()), 255));
        entity.setImageUrl(truncate(clean(dto.getFdFilePathImg()), 512));
        entity.setColor(truncate(clean(dto.getClrNm()), 100));
        entity.setDescription(clean(dto.getFdSbjt()));

        String categoryRaw = clean(dto.getPrdtClNm());
        entity.setCategoryRaw(truncate(categoryRaw, 255));

        if (categoryRaw != null) {
            String[] parts = splitCategory(categoryRaw);
            entity.setCategory(truncate(parts[0], 100));
            entity.setSubcategory(truncate(parts[1], 100));
        } else {
            entity.setCategory(null);
            entity.setSubcategory(null);
        }
    }

    private String[] splitCategory(String categoryRaw) {
        String[] tokens = categoryRaw.split(">");
        String category = clean(tokens.length > 0 ? tokens[0] : null);
        String subcategory = clean(tokens.length > 1 ? tokens[1] : null);
        return new String[]{category, subcategory};
    }

    private LocalDate parseFoundDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String sanitized = raw.trim();
        try {
            // ISO 포맷 (YYYY-MM-DD)
            if (sanitized.length() == 10 && sanitized.contains("-")) {
                return LocalDate.parse(sanitized, DateTimeFormatter.ISO_LOCAL_DATE);
            }
            // BASIC 포맷 (YYYYMMDD)
            if (sanitized.length() == 8) {
                return LocalDate.parse(sanitized, BASIC_DATE);
            }
            log.error("LOST112 날짜 파싱 실패 - 예상 형식 불일치, value={}", raw);
            return null;
        } catch (DateTimeParseException e) {
            log.error("LOST112 날짜 파싱 실패 - value={}, message={}", raw, e.getMessage());
            return null;
        }
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) ? null : trimmed;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || maxLength <= 0) {
            return value;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * 수집·저장 요약 정보.
     */
    public record CollectionSummary(LocalDate startDate,
                                    LocalDate endDate,
                                    String regionCode,
                                    int totalFetched,
                                    int insertedCount,
                                    int updatedCount) {
    }
}
