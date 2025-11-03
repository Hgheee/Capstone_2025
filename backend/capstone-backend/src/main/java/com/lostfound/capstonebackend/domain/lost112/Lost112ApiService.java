package com.lostfound.capstonebackend.domain.lost112;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.capstonebackend.config.Lost112Properties;
import com.lostfound.capstonebackend.domain.lost112.dto.Lost112ItemDto;
import com.lostfound.capstonebackend.domain.lost112.dto.Lost112ResponseDto;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * LOST112 외부 API 호출을 담당하는 서비스.
 */
@Service
@Slf4j
public class Lost112ApiService {

    private static final String ENDPOINT_PATH = "/getLosfundInfoAccToClAreaPd";
    private static final DateTimeFormatter BASIC_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final Lost112Properties lost112Properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public Lost112ApiService(Lost112Properties lost112Properties,
                             WebClient.Builder webClientBuilder,
                             ObjectMapper objectMapper) {
        this.lost112Properties = lost112Properties;
        this.objectMapper = objectMapper;

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, lost112Properties.getTimeouts().getConnectMs())
                .responseTimeout(Duration.ofMillis(lost112Properties.getTimeouts().getReadMs()));

        this.webClient = webClientBuilder
                .baseUrl(Objects.requireNonNull(lost112Properties.getBaseUrl(), "LOST112 baseUrl must not be null"))
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * 지정한 조건의 단일 페이지를 호출한다.
     */
    public Lost112PageResult fetchPage(LocalDate startDate,
                                       LocalDate endDate,
                                       String regionCode,
                                       int pageNo,
                                       int numOfRows) {
        Supplier<Lost112PageResult> supplier = () -> doFetchPage(startDate, endDate, regionCode, pageNo, numOfRows);
        return executeWithRetry(supplier);
    }

    /**
     * 지정된 기간과 지역에 대해 모든 페이지를 순회하면서 데이터를 수집한다.
     */
    public List<Lost112ItemDto> fetchAll(LocalDate startDate,
                                         LocalDate endDate,
                                         String regionCode) {
        int pageSize = Math.min(Math.max(lost112Properties.getPage().getSize(), 1), 100);
        int sleepMs = Math.max(lost112Properties.getPage().getSleepMs(), 0);

        int pageNo = 1;
        int totalCount = Integer.MAX_VALUE;
        List<Lost112ItemDto> allItems = new java.util.ArrayList<>();

        while ((pageNo - 1) * pageSize < totalCount) {
            Lost112PageResult pageResult = fetchPage(startDate, endDate, regionCode, pageNo, pageSize);
            List<Lost112ItemDto> pageItems = pageResult.items();
            if (pageItems.isEmpty()) {
                log.info("LOST112 {}페이지에 더 이상 데이터가 없어 수집을 종료합니다.", pageNo);
                break;
            }

            allItems.addAll(pageItems);
            totalCount = pageResult.totalCount();

            log.info("LOST112 {}페이지 수집 완료 (누적: {}, 전체: {})", pageNo, allItems.size(), totalCount);
            pageNo++;

            if (sleepMs > 0 && (pageNo - 1) * pageSize < totalCount) {
                try {
                    TimeUnit.MILLISECONDS.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("LOST112 페이지네이션 대기 중 인터럽트 발생, 수집을 중단합니다.");
                    break;
                }
            }
        }

        return allItems;
    }

    private Lost112PageResult doFetchPage(LocalDate startDate,
                                          LocalDate endDate,
                                          String regionCode,
                                          int pageNo,
                                          int numOfRows) {
        String requestUri = buildUri(startDate, endDate, regionCode, pageNo, numOfRows);
        log.debug("LOST112 API 호출 URI={}", requestUri);

        try {
            String rawResponse = webClient.get()
                    .uri(requestUri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofMillis(lost112Properties.getTimeouts().getReadMs()));
            if (rawResponse == null || rawResponse.trim().isEmpty()) {
                log.warn("LOST112 API 응답이 비어 있습니다. uri={}", requestUri);
                return Lost112PageResult.empty(pageNo, numOfRows);
            }

            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode responseNode = root.path("response");
            if (responseNode.isMissingNode() || responseNode.isNull()) {
                log.warn("LOST112 API 응답에 response 노드가 없습니다. uri={}", requestUri);
                return Lost112PageResult.empty(pageNo, numOfRows);
            }

            JsonNode headerNode = responseNode.path("header");
            if (headerNode.isMissingNode() || headerNode.isNull()) {
                throw new IllegalStateException("LOST112 API 응답 헤더가 존재하지 않습니다.");
            }

            String resultCode = headerNode.path("resultCode").asText("");
            if (!"00".equals(resultCode)) {
                String resultMsg = headerNode.path("resultMsg").asText("UNKNOWN");
                throw new IllegalStateException(
                        "LOST112 API 오류 - 코드: " + resultCode + ", 메시지: " + resultMsg
                );
            }

            JsonNode bodyNode = responseNode.get("body");
            if (bodyNode == null || bodyNode.isNull() || (bodyNode.isTextual() && bodyNode.asText().trim().isEmpty())) {
                log.info("LOST112 API 응답 본문이 비어 있습니다. uri={}", requestUri);
                return Lost112PageResult.empty(pageNo, numOfRows);
            }

            Lost112ResponseDto.Body body = objectMapper.treeToValue(bodyNode, Lost112ResponseDto.Body.class);

            int totalCount = Optional.ofNullable(body.getTotalCount()).orElse(0);
            List<Lost112ItemDto> items = Optional.ofNullable(body.getItems())
                    .map(Lost112ResponseDto.Items::getItem)
                    .orElse(Collections.emptyList());

            log.debug("LOST112 API 응답 - pageNo={}, numOfRows={}, totalCount={}, items={}",
                    pageNo, numOfRows, totalCount, items.size());

            return new Lost112PageResult(items, totalCount, pageNo, numOfRows);
        } catch (WebClientResponseException e) {
            log.error("LOST112 API HTTP 오류 - status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw e;
        } catch (IOException e) {
            log.warn("LOST112 API 응답 파싱 실패 - uri={}, message={}", requestUri, e.getMessage());
            return Lost112PageResult.empty(pageNo, numOfRows);
        } catch (RuntimeException e) {
            log.warn("LOST112 API 응답 파싱 실패 (빈 응답 가능) - uri={}, message={}", requestUri, e.getMessage());
            return Lost112PageResult.empty(pageNo, numOfRows);
        }
    }

    private Lost112PageResult executeWithRetry(Supplier<Lost112PageResult> supplier) {
        Lost112Properties.Retry retry = lost112Properties.getRetry();
        int maxAttempts = Math.max(retry.getMaxAttempts(), 1);
        long delayMs = Math.max(retry.getDelayMs(), 0L);

        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return supplier.get();
            } catch (RuntimeException ex) {
                lastException = ex;
                if (attempt >= maxAttempts) {
                    break;
                }

                log.warn("LOST112 API 호출 실패 (시도 {}/{}). {}ms 후 재시도합니다. 원인: {}",
                        attempt, maxAttempts, delayMs, ex.getMessage());

                if (delayMs > 0) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("LOST112 재시도 대기 중 인터럽트 발생");
                        throw new IllegalStateException("LOST112 API 재시도 중단", ex);
                    }
                }
            }
        }

        throw lastException != null ? lastException : new IllegalStateException("LOST112 API 호출 실패");
    }

    private String buildUri(LocalDate startDate,
                            LocalDate endDate,
                            String regionCode,
                            int pageNo,
                            int numOfRows) {
        String sanitizedRegionCode = Optional.ofNullable(regionCode).orElse("").trim();
        return UriComponentsBuilder.fromPath(ENDPOINT_PATH)
                .queryParam("serviceKey", lost112Properties.getApiKey())
                .queryParam("START_YMD", BASIC_DATE_FORMAT.format(startDate))
                .queryParam("END_YMD", BASIC_DATE_FORMAT.format(endDate))
                .queryParam("NUM_OF_ROWS", Math.min(Math.max(numOfRows, 1), 100))
                .queryParam("pageNo", pageNo)
                .queryParam("LST_LCT_CD", sanitizedRegionCode)
                .queryParam("_type", "json")
                .build(true)
                .toUriString();
    }

    /**
     * 호환성을 위한 기본 수집 메서드 (당일, 전체 지역).
     */
    public List<Lost112ItemDto> fetchAllLostItems() {
        LocalDate today = LocalDate.now();
        return fetchAll(today, today, "");
    }

    /**
     * 페이지 호출 결과를 담는 레코드.
     */
    public record Lost112PageResult(List<Lost112ItemDto> items, int totalCount, int pageNo, int numOfRows) {
        public static Lost112PageResult empty(int pageNo, int numOfRows) {
            return new Lost112PageResult(Collections.emptyList(), 0, pageNo, numOfRows);
        }
    }
}
