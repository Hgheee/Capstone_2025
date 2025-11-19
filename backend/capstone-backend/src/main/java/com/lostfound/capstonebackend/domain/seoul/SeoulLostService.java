package com.lostfound.capstonebackend.domain.seoul;

import com.lostfound.capstonebackend.common.util.EncodingUtil;
import com.lostfound.capstonebackend.common.util.RegionUtil;
import com.lostfound.capstonebackend.domain.lostitem.LostItem;
import com.lostfound.capstonebackend.domain.seoul.dto.SeoulLostResponse;
import com.lostfound.capstonebackend.domain.seoul.dto.SeoulLostRow;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@Transactional
public class SeoulLostService {

    private static final String URL_TEMPLATE = "http://openapi.seoul.go.kr:8088/%s/json/lostArticleInfo/%d/%d";
    private static final int BATCH_SIZE = 1000; // 한 번에 가져올 데이터 개수 (최대 1000)
    private static final int BATCH_INSERT_SIZE = 100;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30); // 대량 데이터 수집을 위해 타임아웃 증가
    private static final Map<String, String> CATEGORY_MAP = createCategoryMap();

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;
    private final String apiKey;

    public SeoulLostService(JdbcTemplate jdbcTemplate,
                            RestTemplateBuilder restTemplateBuilder,
                            @Value("${api.seoul.key}") String apiKey) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = restTemplateBuilder
                .requestFactory(this::createRequestFactory)
                .defaultHeader("Accept-Charset", "UTF-8")
                .defaultHeader("Content-Type", "application/json;charset=UTF-8")
                .build();
        this.apiKey = apiKey;
    }

    /**
     * 서울교통공사 분실물 데이터 전체 수집 (페이징)
     * 최근 데이터부터 최대한 많이 가져옵니다.
     */
    public int importSeoulLostItems() {
        log.info("========================================");
        log.info("서울교통공사 분실물 데이터 수집 시작");
        log.info("========================================");

        int totalImported = 0;
        int startIndex = 1;
        int pageCount = 0;
        int maxPages = 100; // 최대 100페이지 (100,000건) - 필요시 조정 가능

        while (pageCount < maxPages) {
            pageCount++;
            int endIndex = startIndex + BATCH_SIZE - 1;

            log.info("[{}페이지] 데이터 수집 중... ({} ~ {})", pageCount, startIndex, endIndex);

            SeoulLostResponse response;
            try {
                response = fetchBatch(startIndex, endIndex);
            } catch (RestClientException e) {
                log.error("서울교통공사 API 호출 실패 (페이지: {}) - 사유: {}", pageCount, e.getMessage());
                break; // API 오류 시 중단
            }

            if (response == null || response.getLostArticleInfo() == null) {
                log.warn("서울교통공사 API 응답이 비어있습니다. (페이지: {})", pageCount);
                break;
            }

            SeoulLostResponse.LostArticleInfo info = response.getLostArticleInfo();
            SeoulLostResponse.Result result = info.getResult();

            // 응답 코드 확인
            if (result == null || !Objects.equals("INFO-000", result.getCode())) {
                log.warn("서울교통공사 API 응답 코드 비정상 (페이지: {}) - code: {}, message: {}",
                        pageCount,
                        result != null ? result.getCode() : null,
                        result != null ? result.getMessage() : null);
                
                // 데이터가 없으면 종료
                if (result != null && "INFO-200".equals(result.getCode())) {
                    log.info("더 이상 수집할 데이터가 없습니다. (총 페이지: {})", pageCount - 1);
                }
                break;
            }

            List<SeoulLostRow> rows = info.getRow();
            if (rows == null || rows.isEmpty()) {
                log.info("서울교통공사 API에서 반환된 데이터가 없습니다. (페이지: {})", pageCount);
                break; // 데이터 없으면 종료
            }

            // 전체 개수 확인 (첫 페이지에서만)
            if (pageCount == 1) {
                Integer totalCount = info.getListTotalCount();
                if (totalCount != null && totalCount > 0) {
                    log.info("📊 전체 데이터 개수: {}건", totalCount);
                    int estimatedPages = (totalCount / BATCH_SIZE) + 1;
                    log.info("📄 예상 페이지 수: {}페이지", estimatedPages);
                    maxPages = Math.min(estimatedPages, 100); // 최대 100페이지로 제한
                }
            }

            // 데이터 변환 및 저장 (인코딩 처리 + 깨진 데이터 필터링)
            List<LostItem> items = new ArrayList<>(rows.size());
            int brokenCount = 0;
            
            for (SeoulLostRow row : rows) {
                if (row == null) {
                    continue;
                }

                String rawExternalId = row.getLostMngNo();
                if (!StringUtils.hasText(rawExternalId)) {
                    continue;
                }

                // 엔티티 변환 (인코딩 처리 포함)
                LostItem item = convertToEntity(row);
                
                // 깨진 데이터 필터링
                if (item != null && !isDataBroken(item)) {
                    items.add(item);
                } else {
                    brokenCount++;
                    if (item != null) {
                        log.warn("⚠️ 깨진 데이터 제외 (서울교통공사) - ID: {}, Title: {}", 
                                item.getExternalId(), 
                                EncodingUtil.safeSubstring(item.getTitle(), 30));
                    }
                }
            }
            
            if (brokenCount > 0) {
                log.warn("⚠️ [{}페이지] 깨진 데이터 {}건 제외됨", pageCount, brokenCount);
            }

            if (!items.isEmpty()) {
                batchInsertLostItems(items);
                totalImported += items.size();
                log.info("✅ [{}페이지] {}건 처리 완료 (누적: {}건)", pageCount, items.size(), totalImported);
            }

            // 다음 페이지로
            startIndex = endIndex + 1;

            // API 부하 방지를 위한 짧은 대기 (0.5초)
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("데이터 수집 중 인터럽트 발생");
                break;
            }
        }

        log.info("========================================");
        log.info("서울교통공사 분실물 수집 완료!");
        log.info("총 {}페이지, {}건 수집", pageCount, totalImported);
        log.info("========================================");

        return totalImported;
    }

    /**
     * 서울교통공사 API에서 특정 범위의 데이터 가져오기
     * @param startIndex 시작 인덱스 (1부터 시작)
     * @param endIndex 종료 인덱스
     */
    private SeoulLostResponse fetchBatch(int startIndex, int endIndex) {
        String url = String.format(URL_TEMPLATE, apiKey, startIndex, endIndex);
        ResponseEntity<SeoulLostResponse> response = restTemplate.getForEntity(url, SeoulLostResponse.class);
        return response.getBody();
    }

    private ClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.toIntExact(CONNECT_TIMEOUT.toMillis()));
        factory.setReadTimeout(Math.toIntExact(READ_TIMEOUT.toMillis()));
        return factory;
    }

    private void batchInsertLostItems(List<LostItem> items) {
        if (items.isEmpty()) {
            return;
        }

        // 중복 체크: external_id가 이미 존재하는 항목 제외
        final String sql = "INSERT INTO lost_item "
                + "(title, status, datasource, category, location, description, external_id, "
                + "storage_location, received_date, found_date, created_at, updated_at, view_count, region) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), ?, ?) "
                + "ON DUPLICATE KEY UPDATE external_id = external_id";  // 중복 시 무시

        jdbcTemplate.batchUpdate(sql, items, BATCH_INSERT_SIZE, (ps, item) -> {
            ps.setString(1, item.getTitle());
            ps.setString(2, item.getStatus() != null ? item.getStatus().name() : LostItem.Status.STORED.name());
            ps.setString(3, item.getDataSource() != null ? item.getDataSource().name() : LostItem.DataSource.SEOUL_LOST.name());

            if (item.getCategory() != null) {
                ps.setString(4, item.getCategory());
            } else {
                ps.setNull(4, Types.VARCHAR);
            }

            if (item.getLocation() != null) {
                ps.setString(5, item.getLocation());
            } else {
                ps.setNull(5, Types.VARCHAR);
            }

            if (item.getDescription() != null) {
                ps.setString(6, item.getDescription());
            } else {
                ps.setNull(6, Types.VARCHAR);
            }

            ps.setString(7, item.getExternalId());

            if (item.getStorageLocation() != null) {
                ps.setString(8, item.getStorageLocation());
            } else {
                ps.setNull(8, Types.VARCHAR);
            }

            if (item.getReceivedDate() != null) {
                ps.setTimestamp(9, Timestamp.valueOf(item.getReceivedDate()));
            } else {
                ps.setNull(9, Types.TIMESTAMP);
            }

            if (item.getFoundDate() != null) {
                ps.setDate(10, Date.valueOf(item.getFoundDate()));
            } else {
                ps.setNull(10, Types.DATE);
            }

            if (item.getViewCount() != null) {
                ps.setInt(11, item.getViewCount());
            } else {
                ps.setNull(11, Types.INTEGER);
            }

            // region 추가
            if (item.getRegion() != null) {
                ps.setString(12, item.getRegion());
            } else {
                ps.setNull(12, Types.VARCHAR);
            }
        });
        
        log.info("서울교통공사 배치 삽입 완료 - 총 {}건 시도 (중복은 자동 제외됨)", items.size());
    }

    private LostItem convertToEntity(SeoulLostRow row) {
        LocalDate foundDate = parseDate(row.getRegYmd());
        LocalDate received = parseDate(row.getRcvYmd());
        LocalDateTime receivedDate = received != null ? received.atStartOfDay() : null;
        Integer viewCount = parseInteger(row.getInqCnt());

        log.debug("서울시 원본 status 값: '{}'", row.getLostStts());

        // ✅ 한글 인코딩 안전 처리
        String title = EncodingUtil.safeDecodeBySource(row.getLostNm(), "SEOUL_LOST");
        if (title == null || title.trim().isEmpty()) {
            title = "무제 분실물";
        }
        
        String description = EncodingUtil.safeDecodeBySource(row.getLgsDtlCn(), "SEOUL_LOST");
        String location = EncodingUtil.safeDecodeBySource(row.getRcpl(), "SEOUL_LOST");
        String storageLocation = EncodingUtil.safeDecodeBySource(row.getCstdPlc(), "SEOUL_LOST");
        String category = mapCategory(row.getLostKnd());
        
        // 지역 정보 추출 (title 포함)
        String region = RegionUtil.extractRegionFromAll(title, location, storageLocation);

        return LostItem.builder()
                .externalId(StringUtils.hasText(row.getLostMngNo()) ? row.getLostMngNo().trim() : "UNKNOWN")
                .title(title)
                .description(description)
                .category(category)
                .location(location)
                .region(region)
                .storageLocation(storageLocation)
                .foundDate(foundDate)
                .receivedDate(receivedDate)
                .viewCount(viewCount != null ? viewCount : 0)
                .owner(null)
                .dataSource(LostItem.DataSource.SEOUL_LOST)
                .status(mapStatus(row.getLostStts()))
                .build();
    }

    /**
     * 깨진 데이터인지 확인합니다.
     */
    private boolean isDataBroken(LostItem item) {
        if (item == null) {
            return true;
        }
        
        return EncodingUtil.hasAnyBroken(
                item.getTitle(),
                item.getDescription(),
                item.getCategory(),
                item.getLocation(),
                item.getStorageLocation()
        );
    }

    private String mapCategory(String originalCategory) {
        if (!StringUtils.hasText(originalCategory)) {
            return "ETC";
        }
        String normalized = originalCategory.trim();
        return CATEGORY_MAP.getOrDefault(normalized, "ETC");
    }

    private LostItem.Status mapStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) {
            return LostItem.Status.STORED;
        }

        String normalized = rawStatus.trim();

        if (normalized.equals("수령완료") || normalized.equals("반환")) {
            return LostItem.Status.RETURNED;
        }
        if (normalized.equals("폐기") || normalized.equals("폐기됨")) {
            return LostItem.Status.DISPOSED;
        }
        if (normalized.equals("보관중") || normalized.equals("보관")) {
            return LostItem.Status.STORED;
        }
        if (normalized.equals("만료") || normalized.equals("만료됨")) {
            return LostItem.Status.EXPIRED;
        }

        return LostItem.Status.STORED;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            String normalized = dateStr.trim();
            if (normalized.contains(" ")) {
                normalized = normalized.split(" ")[0];
            }
            return LocalDate.parse(normalized, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("서울시 분실물 날짜 파싱 실패 - 값: {}", dateStr);
            return null;
        }
    }

    private Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("서울시 분실물 조회수 파싱 실패 - 값: {}", value);
            return null;
        }
    }

    private static Map<String, String> createCategoryMap() {
        Map<String, String> map = new HashMap<>();
        map.put("지갑/카드", "WALLET");
        map.put("가방/배낭", "BAG");
        map.put("핸드폰/휴대폰", "PHONE");
        map.put("의류/옷", "CLOTHING");
        map.put("서류/문서", "DOCUMENT");
        map.put("열쇠", "KEY");
        map.put("우산", "UMBRELLA");
        map.put("기타", "ETC");
        return map;
    }
}
