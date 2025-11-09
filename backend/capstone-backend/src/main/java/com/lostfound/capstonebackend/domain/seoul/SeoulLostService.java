package com.lostfound.capstonebackend.domain.seoul;

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

    private static final String URL_TEMPLATE = "http://openapi.seoul.go.kr:8088/%s/json/lostArticleInfo/1/100";
    private static final int BATCH_INSERT_SIZE = 100;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);
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

    public int importSeoulLostItems() {
        SeoulLostResponse response;
        try {
            response = fetchBatch();
        } catch (RestClientException e) {
            log.error("서울시 분실물 API 호출 실패 - 사유: {}", e.getMessage(), e);
            return 0;
        }

        if (response == null || response.getLostArticleInfo() == null) {
            log.warn("서울시 분실물 API 응답이 비어있습니다.");
            return 0;
        }

        SeoulLostResponse.LostArticleInfo info = response.getLostArticleInfo();
        SeoulLostResponse.Result result = info.getResult();

        if (result == null || !Objects.equals("INFO-000", result.getCode())) {
            log.warn("서울시 분실물 API 응답 코드 비정상 - code: {}, message: {}",
                    result != null ? result.getCode() : null,
                    result != null ? result.getMessage() : null);
            return 0;
        }

        List<SeoulLostRow> rows = info.getRow();
        if (rows == null || rows.isEmpty()) {
            log.info("서울시 분실물 API에서 반환된 데이터가 없습니다.");
            return 0;
        }

        List<LostItem> items = new ArrayList<>(rows.size());
        for (SeoulLostRow row : rows) {
            if (row == null) {
                continue;
            }

            String rawExternalId = row.getLostMngNo();
            if (!StringUtils.hasText(rawExternalId)) {
                continue;
            }

            items.add(convertToEntity(row));
        }

        if (!items.isEmpty()) {
            batchInsertLostItems(items);
        }

        log.info("서울시 분실물 수집 완료 - 총 {}건 수신, 저장: {}건", rows.size(), items.size());
        return items.size();
    }

    private SeoulLostResponse fetchBatch() {
        String url = String.format(URL_TEMPLATE, apiKey);
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

        final String sql = "INSERT INTO lost_item "
                + "(title, status, datasource, category, location, description, external_id, "
                + "storage_location, received_date, found_date, created_at, updated_at, view_count) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), ?)";

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
        });
    }

    private LostItem convertToEntity(SeoulLostRow row) {
        LocalDate foundDate = parseDate(row.getRegYmd());
        LocalDate received = parseDate(row.getRcvYmd());
        LocalDateTime receivedDate = received != null ? received.atStartOfDay() : null;
        Integer viewCount = parseInteger(row.getInqCnt());

        log.info("서울시 원본 status 값: '{}'", row.getLostStts());

        String title = StringUtils.hasText(row.getLostNm()) ? row.getLostNm().trim() : "무제 분실물";
        String location = StringUtils.hasText(row.getRcpl()) ? row.getRcpl().trim() : null;
        String storageLocation = StringUtils.hasText(row.getCstdPlc()) ? row.getCstdPlc().trim() : null;
        
        // 지역 정보 추출 (title 포함)
        String region = RegionUtil.extractRegionFromAll(title, location, storageLocation);

        return LostItem.builder()
                .externalId(StringUtils.hasText(row.getLostMngNo()) ? row.getLostMngNo().trim() : "UNKNOWN")
                .title(title)
                .description(StringUtils.hasText(row.getLgsDtlCn()) ? row.getLgsDtlCn().trim() : null)
                .category(mapCategory(row.getLostKnd()))
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
