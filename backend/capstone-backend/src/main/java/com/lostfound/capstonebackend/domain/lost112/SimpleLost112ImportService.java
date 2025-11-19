package com.lostfound.capstonebackend.domain.lost112;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.capstonebackend.common.util.EncodingUtil;
import com.lostfound.capstonebackend.common.util.RegionUtil;
import com.lostfound.capstonebackend.config.Lost112Properties;
import com.lostfound.capstonebackend.domain.lost112.dto.Lost112ItemDto;
import com.lostfound.capstonebackend.domain.lostitem.LostItem;
import com.lostfound.capstonebackend.domain.lostitem.LostItemRepository;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 간단한 LOST112 수집기를 제공하는 서비스입니다.
 * Python 스크립트 없이 WebClient로 LOST112 API를 호출하고, 분실물 본 테이블에 직접 저장합니다.
 */
@Service
@Slf4j
@Transactional
public class SimpleLost112ImportService {

    private static final String API_PATH = "/getLosfundInfoAccToClAreaPd";
    private static final String DEFAULT_REGION_CODE = "01";
    private static final String SUCCESS_CODE = "00";
    private static final DateTimeFormatter INPUT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter API_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final TypeReference<List<Lost112ItemDto>> LOST112_ITEM_LIST = new TypeReference<>() {};
    private static final Map<String, String> REGION_KEYWORDS = createRegionKeywordMap();

    private final LostItemRepository lostItemRepository;
    private final Lost112Properties lost112Properties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final String baseUrl;
    private final int defaultPageSize;
    private final int sleepMs;
    private final Duration readTimeout;

    public SimpleLost112ImportService(LostItemRepository lostItemRepository,
                                      Lost112Properties lost112Properties,
                                      ObjectMapper objectMapper,
                                      WebClient.Builder webClientBuilder) {
        this.lostItemRepository = lostItemRepository;
        this.lost112Properties = lost112Properties;
        this.objectMapper = objectMapper;

        this.baseUrl = normalizeBaseUrl(lost112Properties.getBaseUrl());

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, lost112Properties.getTimeouts().getConnectMs())
                .responseTimeout(Duration.ofMillis(lost112Properties.getTimeouts().getReadMs()));

        this.webClient = webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(this.baseUrl)
                .build();

        this.defaultPageSize = Math.min(100, Math.max(1, lost112Properties.getPage().getSize()));
        this.sleepMs = Math.max(0, lost112Properties.getPage().getSleepMs());
        this.readTimeout = Duration.ofMillis(lost112Properties.getTimeouts().getReadMs());
    }

    /**
     * LOST112 API에서 데이터를 조회해 바로 lost_item 테이블에 저장합니다.
     *
     * @param startDate  시작일 (yyyy-MM-dd)
     * @param endDate    종료일 (yyyy-MM-dd)
     * @param regionCode 지역 코드
     * @param maxPages   조회할 최대 페이지 수
     * @param rowsPerPage 한 페이지당 조회 수
     * @return totalFetched, newlySaved, duplicatesSkipped 정보를 담은 Map
     */
    public Map<String, Object> importData(String startDate,
                                          String endDate,
                                          String regionCode,
                                          Integer maxPages,
                                          Integer rowsPerPage) {
        LocalDate start = parseDate(startDate, "startDate");
        LocalDate end = parseDate(endDate, "endDate");
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("startDate는 endDate보다 이후일 수 없습니다.");
        }

        int pageLimit = (maxPages == null || maxPages < 1) ? 1 : maxPages;
        int pageSize = resolvePageSize(rowsPerPage);
        String targetRegion = (regionCode == null || regionCode.isBlank())
                ? DEFAULT_REGION_CODE
                : regionCode.trim();

        String startYmd = API_DATE_FORMAT.format(start);
        String endYmd = API_DATE_FORMAT.format(end);

        int totalFetched = 0;
        int newlySaved = 0;
        int duplicatesSkipped = 0;

        int totalCountFromApi = -1;
        for (int pageNo = 1; pageNo <= pageLimit; pageNo++) {
            PageResult pageResult = fetchPage(pageNo, targetRegion, startYmd, endYmd, pageSize);
            List<Lost112ItemDto> pageItems = pageResult.items();

            if (totalCountFromApi < 0 && pageResult.totalCount() >= 0) {
                totalCountFromApi = pageResult.totalCount();
            }

            if (pageItems.isEmpty()) {
                if (pageNo == 1) {
                    log.info("LOST112 API에서 요청 조건에 해당하는 데이터가 없습니다. start={}, end={}, region={}",
                            startYmd, endYmd, targetRegion);
                }
                break;
            }

            totalFetched += pageItems.size();

            BatchSaveResult batchResult = persistNewItems(pageItems);
            newlySaved += batchResult.savedCount();
            duplicatesSkipped += batchResult.duplicateCount();

            if (totalCountFromApi > 0 && totalFetched >= totalCountFromApi) {
                log.debug("API totalCount({}) 이상을 수집했으므로 종료합니다.", totalCountFromApi);
                break;
            }

            if (pageItems.size() < pageSize && totalCountFromApi < 0) {
                log.debug("마지막 페이지({}) 도달 - pageSize보다 적은 {}개의 항목", pageNo, pageItems.size());
                break;
            }

            if (pageNo < pageLimit) {
                sleepBetweenPages();
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalFetched", totalFetched);
        result.put("newlySaved", newlySaved);
        result.put("duplicatesSkipped", duplicatesSkipped);
        return result;
    }

    private BatchSaveResult persistNewItems(List<Lost112ItemDto> items) {
        List<String> candidateExternalIds = items.stream()
                .map(Lost112ItemDto::getAtcId)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (candidateExternalIds.isEmpty()) {
            return new BatchSaveResult(0, 0);
        }

        List<LostItem> existing = lostItemRepository.findByExternalIdInAndDataSource(candidateExternalIds, LostItem.DataSource.LOST112);
        Set<String> knownExternalIds = existing.stream()
                .map(LostItem::getExternalId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<LostItem> toSave = new ArrayList<>();
        int duplicates = 0;
        int brokenCount = 0;

        for (Lost112ItemDto itemDto : items) {
            String externalId = normalize(itemDto.getAtcId());
            if (externalId == null) {
                continue;
            }
            if (!knownExternalIds.add(externalId)) {
                duplicates++;
                continue;
            }
            
            // ✅ 인코딩 처리 및 깨진 데이터 필터링
            LostItem item = convertToLostItem(itemDto);
            if (item != null && !isDataBroken(item)) {
                toSave.add(item);
            } else {
                brokenCount++;
                if (item != null) {
                    log.warn("⚠️ 깨진 데이터 제외 (LOST112) - ID: {}, Title: {}", 
                            item.getExternalId(), 
                            EncodingUtil.safeSubstring(item.getTitle(), 30));
                }
            }
        }

        if (brokenCount > 0) {
            log.warn("⚠️ 깨진 데이터 {}건 제외됨", brokenCount);
        }

        if (toSave.isEmpty()) {
            return new BatchSaveResult(0, duplicates);
        }

        lostItemRepository.saveAll(toSave);
        return new BatchSaveResult(toSave.size(), duplicates);
    }

    private int resolvePageSize(Integer rowsPerPage) {
        if (rowsPerPage == null) {
            return defaultPageSize;
        }
        return Math.min(100, Math.max(1, rowsPerPage));
    }

    private PageResult fetchPage(int pageNo, String regionCode, String startYmd, String endYmd, int pageSize) {
        String apiKey = normalize(lost112Properties.getApiKey());
        if (apiKey == null) {
            throw new IllegalStateException("LOST112 API Key가 설정되지 않았습니다.");
        }

        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path(API_PATH)
                .queryParam("serviceKey", UriUtils.encode(apiKey, StandardCharsets.UTF_8))
                .queryParam("START_YMD", startYmd)
                .queryParam("END_YMD", endYmd)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", pageSize)
                .queryParam("_type", "json")
                .build(true)
                .toUri();

        JsonNode rootNode;
        try {
            rootNode = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(readTimeout);
        } catch (WebClientResponseException e) {
            log.error("LOST112 API HTTP 오류 (pageNo={}): {}", pageNo, e.getResponseBodyAsString(), e);
            throw new IllegalStateException("LOST112 API 응답 오류: " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new IllegalStateException("LOST112 API 호출 실패", e);
        }

        if (rootNode == null || rootNode.isMissingNode()) {
            throw new IllegalStateException("LOST112 API 응답이 존재하지 않습니다.");
        }

        JsonNode headerNode = rootNode.path("response").path("header");
        String resultCode = headerNode.path("resultCode").asText();
        if (!SUCCESS_CODE.equals(resultCode)) {
            String message = headerNode.path("resultMsg").asText("Unknown");
            throw new IllegalStateException("LOST112 API 오류 [" + resultCode + "]: " + message);
        }

        JsonNode bodyNode = rootNode.path("response").path("body");
        int totalCount = bodyNode.path("totalCount").asInt(-1);
        JsonNode itemsNode = bodyNode.path("items").path("item");
        if (itemsNode.isMissingNode() || itemsNode.isNull()) {
            log.info("LOST112 API page {} 응답 - totalCount={}, 실제 0개 (요청 {})",
                    pageNo, totalCount, pageSize);
            return new PageResult(Collections.emptyList(), totalCount);
        }

        List<Lost112ItemDto> items = itemsNode.isArray()
                ? objectMapper.convertValue(itemsNode, LOST112_ITEM_LIST)
                : Collections.singletonList(objectMapper.convertValue(itemsNode, Lost112ItemDto.class));

        log.info("LOST112 API page {} 응답 - totalCount={}, 실제 {}개 (요청 {})",
                pageNo, totalCount, items.size(), pageSize);
        return new PageResult(items, totalCount);
    }

    private void sleepBetweenPages() {
        if (sleepMs <= 0) {
            return;
        }
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("LOST112 수집 중 인터럽트 발생 - 즉시 중단합니다.");
            throw new IllegalStateException("LOST112 수집이 인터럽트되었습니다.", e);
        }
    }

    private LostItem convertToLostItem(Lost112ItemDto dto) {
        // ✅ 한글 인코딩 안전 처리
        String title = EncodingUtil.safeDecodeBySource(resolveTitle(dto), "LOST112");
        if (title == null || title.trim().isEmpty()) {
            title = "무제 분실물";
        }
        
        String description = EncodingUtil.safeDecodeBySource(dto.getFdSbjt(), "LOST112");
        String storageLocation = EncodingUtil.safeDecodeBySource(dto.getDepPlace(), "LOST112");
        String color = EncodingUtil.safeDecodeBySource(dto.getClrNm(), "LOST112");
        
        String category = extractCategory(dto.getPrdtClNm());
        LocalDate foundDate = parseFoundDate(dto.getFdYmd());
        String region = extractRegionFromDepPlace(storageLocation);

        return LostItem.builder()
                .externalId(normalize(dto.getAtcId()))
                .title(title)
                .description(description)
                .category(category)
                .location(storageLocation)
                .storageLocation(storageLocation)
                .region(region)
                .foundDate(foundDate)
                .imagePath(dto.getFdFilePathImg())
                .color(color)
                .dataSource(LostItem.DataSource.LOST112)
                .status(LostItem.Status.STORED)
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
                item.getStorageLocation(),
                item.getColor()
        );
    }

    private String extractRegionFromDepPlace(String depPlace) {
        if (depPlace == null || depPlace.isBlank()) {
            return null;
        }

        String normalized = depPlace.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : REGION_KEYWORDS.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        String fallback = RegionUtil.extractRegionFromLocations(depPlace, depPlace);
        if (fallback == null) {
            fallback = RegionUtil.extractRegion(depPlace);
        }
        return normalizeProvince(fallback);
    }

    private String normalizeProvince(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.contains(" ")) {
            return trimmed.split(" ")[0];
        }
        return trimmed;
    }

    private String resolveTitle(Lost112ItemDto dto) {
        if (dto.getFdPrdtNm() != null && !dto.getFdPrdtNm().isBlank()) {
            return dto.getFdPrdtNm();
        }
        if (dto.getFdSbjt() != null && !dto.getFdSbjt().isBlank()) {
            return dto.getFdSbjt();
        }
        return "LOST112 분실물";
    }

    private String extractCategory(String categoryRaw) {
        if (categoryRaw == null || categoryRaw.isBlank()) {
            return null;
        }
        String[] parts = categoryRaw.split(">");
        return parts.length == 0 ? categoryRaw.trim() : parts[0].trim();
    }

    private LocalDate parseFoundDate(String fdYmd) {
        if (fdYmd == null || fdYmd.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(fdYmd.trim(), API_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            log.warn("fdYmd 파싱 실패 - 값: {}", fdYmd);
            return null;
        }
    }

    private LocalDate parseDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 값이 비어있습니다. (yyyy-MM-dd 형식)");
        }
        try {
            return LocalDate.parse(value.trim(), INPUT_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + " 값이 yyyy-MM-dd 형식인지 확인하세요.", e);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String fallback = "https://apis.data.go.kr/1320000/LosfundInfoInqireService";
        String target = (baseUrl == null || baseUrl.isBlank()) ? fallback : baseUrl.trim();
        if (target.endsWith("/")) {
            return target.substring(0, target.length() - 1);
        }
        return target;
    }

    private record BatchSaveResult(int savedCount, int duplicateCount) { }

    private record PageResult(List<Lost112ItemDto> items, int totalCount) { }

    private static Map<String, String> createRegionKeywordMap() {
        Map<String, String> map = new LinkedHashMap<>();
        registerKeywords(map, "서울",
                "당산", "잠실", "이태원", "영등포", "강남", "노량진", "마포", "종로", "홍대", "용산", "송파",
                "월드컵", "불암", "신길", "을지로", "쌍문", "응암", "둔촌", "번동", "화랑", "상봉", "대치", "정릉",
                "면목", "회기", "압구정", "천호", "문래", "논현", "망우", "가락", "학동", "왕십리", "상일", "충정로",
                "마들", "개봉", "가양", "반포", "부암", "홍은", "서래", "가덕도", "까치산", "방이", "염창", "명동",
                "고척", "청운", "사직", "답십리", "약수", "신당", "먹골", "남현", "삼각지", "목1", "길동", "신촌",
                "서강", "대학로", "왕릉", "이수", "삼양", "방화", "봉천", "백운", "중곡", "관산", "진관", "홍제",
                "마곡", "낙성대", "강일", "도산", "수유", "홍익", "남태령", "독산", "도곡", "신월1", "신대방", "을지",
                "서곶", "숭의", "화양", "청천", "사당", "여의도", "목2", "신사", "제기", "신월", "서정", "신정3",
                "신정2", "충무", "금광", "미아", "남대문", "서초2", "청량리", "아중", "성정", "수송", "내곡", "감전",
                "가좌", "보람", "화곡", "연신내", "관악산", "불당", "철산", "신풍", "성수", "대방", "가리봉", "방배1",
                "자양", "전농2", "성동경찰서", "관악경찰서", "성북경찰서", "광진경찰서",
                "율천", "삼전", "청담", "개포", "일원", "수서", "왕십리", "명동", "청운", "사직",
                "홍제", "서래", "압구정", "회기", "약수", "신당", "답십리", "먹골", "삼각지",
                "왕릉", "낙성대", "도산", "대학로", "강일", "청량리", "충정로", "이수");
        registerKeywords(map, "경기",
                "미사", "수원", "성남", "일산", "안산", "용인", "부천", "고양", "광명", "평택",
                "운정호수", "매산", "상동", "가산", "역곡", "범계", "인덕원", "구일", "당곡", "전하",
                "위례", "본오", "서판교", "대화", "동판교", "원종", "금촌", "죽전", "옥산", "호매실",
                "경안", "평내", "전곡", "양근", "세교", "보정", "공도", "별내", "와동", "오포", "솔내",
                "구성", "호원", "중마", "백강", "운양", "배곧", "비봉", "장기", "가남", "화전", "남양",
                "회천", "가평", "여주", "의정부", "중동", "서현", "읍내", "동탄3", "매탄", "당하", "풍사",
                "신방", "오산", "운정야당", "우동", "천왕", "평산", "오라", "영산", "영통", "두정", "과천",
                "옥정", "도통", "원당", "송내", "용암", "아림", "수지", "강동", "동탄", "송탄", "금곡", "수진",
                "토평", "군포", "진접", "하남", "대야", "고읍", "선화", "인주", "남신암", "상현", "구갈", "정왕",
                "안양", "의왕", "파주경찰서", "파주", "양주경찰서", "양평경찰서", "구리경찰서", "김포경찰서",
                "시흥경찰서", "소사", "부개2", "복정", "주엽", "와부", "초월", "갈매", "팽성", "발안", "원곡",
                "봉담", "미원", "당고개");
        registerKeywords(map, "부산",
                "황금", "대연", "해운대", "남포", "센텀", "부산진", "사하", "사상",
                "구포", "전포", "감천", "초량", "주례", "농소1", "농소2", "학장", "신천", "반여",
                "내당4동", "양포", "장전", "좌동", "하서", "송현", "용원", "수영망미2", "당감",
                "상대", "광남", "연산", "양정");
        registerKeywords(map, "인천",
                "송도", "인하", "계양", "부평",
                "송도국제도시", "청라", "주안2", "석남", "계산", "간석", "만수", "구월", "송도국제도시2", "갈산", "동춘",
                "신안", "주안역");
        registerKeywords(map, "대전",
                "둔산", "유성", "계룡", "대덕",
                "중앙", "신탄진", "노은", "어은", "청사", "중화", "사창");
        registerKeywords(map, "대구",
                "동성로", "수성", "달서", "달성", "북구", "중구",
                "대명", "불로", "동대명", "성서", "다사", "신천", "두류", "복현", "상인", "안심", "범어",
                "신관", "일곡", "난우", "연지", "현풍", "동덕", "팔곡");
        registerKeywords(map, "광주",
                "상무", "첨단", "송정", "남구",
                "금남", "수완", "용강", "개양", "남문", "백운", "동암", "신어", "고산", "용당", "노송",
                "금당", "당현");
        registerKeywords(map, "충북",
                "남성", "분평", "복대", "분당", "청주", "청원", "음성", "진천", "제천",
                "창전", "압량", "금왕", "청안", "흥덕", "증평");
        registerKeywords(map, "충남",
                "천안", "아산", "온양", "부강", "보령", "서산",
                "서호", "유가", "예산경찰서", "예산", "태안경찰서", "공주경찰서");
        registerKeywords(map, "강원",
                "문막", "치악", "후평", "청초", "솔샘", "산남", "삼척", "태백", "홍천",
                "동해경찰서", "속초경찰서", "강릉경찰서", "평창경찰서", "우산", "강현",
                "묵호", "울진경찰서", "울진");
        registerKeywords(map, "경남",
                "양산", "창원", "김해", "진주", "거제", "거창", "밀양", "물금", "옥포", "신마산", "의창",
                "공항", "광도", "남강", "연일", "용전", "신장",
                "마산동부경찰서", "마산중부경찰서", "마산", "통영경찰서", "사천경찰서");
        registerKeywords(map, "경북",
                "포항", "경주", "안동", "구미", "영천", "김천", "상주", "문경", "신녕",
                "신영주", "관수", "일직", "영주경찰서", "경산경찰서", "칠곡경찰서", "왜관");
        registerKeywords(map, "전북",
                "인창", "전주", "군산", "익산", "옥구", "완주", "순창",
                "역전", "남원경찰서", "진안경찰서", "고창경찰서");
        registerKeywords(map, "전남",
                "담양", "순천", "여수", "목포", "광양", "나주",
                "강진경찰서", "진도경찰서");
        registerKeywords(map, "울산",
                "범서", "무거", "온산", "방어진", "야음",
                "내동", "효성", "태화");
        registerKeywords(map, "세종",
                "아름", "한솔", "조치원",
                "합성");
        registerKeywords(map, "제주",
                "노형", "제주", "서귀포", "한림", "성산", "애월",
                "표선", "고등");
        return map;
    }

    private static void registerKeywords(Map<String, String> map, String region, String... keywords) {
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            String normalized = keyword.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
            map.put(normalized, region);
        }
    }
}
