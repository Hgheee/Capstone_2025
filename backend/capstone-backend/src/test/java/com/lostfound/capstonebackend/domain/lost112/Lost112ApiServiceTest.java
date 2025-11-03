package com.lostfound.capstonebackend.domain.lost112;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.capstonebackend.config.Lost112Properties;
import com.lostfound.capstonebackend.domain.lost112.dto.Lost112ItemDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Lost112ApiServiceTest {

    @Test
    @DisplayName("LOST112 API 페이지 호출 성공")
    void fetchPageSuccess() {
        // Given
        Lost112Properties properties = createProperties();
        ObjectMapper objectMapper = new ObjectMapper();

        String json = """
            {
              "response": {
                "header": {"resultCode": "00", "resultMsg": "OK"},
                "body": {
                  "totalCount": 1,
                  "items": {
                    "item": [{
                      "atcId": "A1",
                      "fdPrdtNm": "지갑",
                      "fdSbjt": "갈색 지갑",
                      "fdYmd": "20240101",
                      "prdtClNm": "지갑",
                      "clrNm": "갈색",
                      "depPlace": "서울경찰서",
                      "fdFilePathImg": "http://image"
                    }]
                  }
                }
              }
            }
            """;

        ExchangeFunction exchangeFunction = request ->
            Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .build());

        Lost112ApiService apiService = new Lost112ApiService(
            properties,
            WebClient.builder().exchangeFunction(exchangeFunction),
            objectMapper
        );

        // When
        LocalDate today = LocalDate.now();
        Lost112ApiService.Lost112PageResult result =
            apiService.fetchPage(today, today, "01", 1, 10);

        // Then
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).getAtcId()).isEqualTo("A1");
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.pageNo()).isEqualTo(1);
    }

    @Test
    @DisplayName("LOST112 API 호출 실패 코드")
    void fetchPageFailure() {
        // Given
        Lost112Properties properties = createProperties();
        ObjectMapper objectMapper = new ObjectMapper();

        String json = """
            {
              "response": {
                "header": {"resultCode": "99", "resultMsg": "ERROR"},
                "body": {
                  "totalCount": 0,
                  "items": null
                }
              }
            }
            """;

        ExchangeFunction exchangeFunction = request ->
            Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .build());

        Lost112ApiService apiService = new Lost112ApiService(
            properties,
            WebClient.builder().exchangeFunction(exchangeFunction),
            objectMapper
        );

        // When
        LocalDate today = LocalDate.now();
        Lost112ApiService.Lost112PageResult result =
            apiService.fetchPage(today, today, "01", 1, 10);

        // Then - API now returns empty result instead of throwing exception
        assertThat(result.items()).isEmpty();
        assertThat(result.totalCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("LOST112 API 빈 응답 처리")
    void fetchPageEmptyResponse() {
        // Given
        Lost112Properties properties = createProperties();
        ObjectMapper objectMapper = new ObjectMapper();

        String json = """
            {
              "response": {
                "header": {"resultCode": "00", "resultMsg": "OK"},
                "body": {
                  "totalCount": 0,
                  "items": null
                }
              }
            }
            """;

        ExchangeFunction exchangeFunction = request ->
            Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .build());

        Lost112ApiService apiService = new Lost112ApiService(
            properties,
            WebClient.builder().exchangeFunction(exchangeFunction),
            objectMapper
        );

        // When
        LocalDate today = LocalDate.now();
        Lost112ApiService.Lost112PageResult result =
            apiService.fetchPage(today, today, "", 1, 10);

        // Then
        assertThat(result.items()).isEmpty();
        assertThat(result.totalCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("LOST112 fetchAll 메서드 호출 성공")
    void fetchAllSuccess() {
        // Given
        Lost112Properties properties = createProperties();
        ObjectMapper objectMapper = new ObjectMapper();

        String json = """
            {
              "response": {
                "header": {"resultCode": "00", "resultMsg": "OK"},
                "body": {
                  "totalCount": 2,
                  "items": {
                    "item": [
                      {
                        "atcId": "A1",
                        "fdPrdtNm": "지갑",
                        "fdSbjt": "갈색 지갑",
                        "fdYmd": "20240101",
                        "prdtClNm": "지갑",
                        "clrNm": "갈색",
                        "depPlace": "서울경찰서",
                        "fdFilePathImg": "http://image1"
                      },
                      {
                        "atcId": "A2",
                        "fdPrdtNm": "핸드폰",
                        "fdSbjt": "검정 핸드폰",
                        "fdYmd": "20240102",
                        "prdtClNm": "전자기기",
                        "clrNm": "검정",
                        "depPlace": "부산경찰서",
                        "fdFilePathImg": "http://image2"
                      }
                    ]
                  }
                }
              }
            }
            """;

        ExchangeFunction exchangeFunction = request ->
            Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .build());

        Lost112ApiService apiService = new Lost112ApiService(
            properties,
            WebClient.builder().exchangeFunction(exchangeFunction),
            objectMapper
        );

        // When
        LocalDate today = LocalDate.now();
        var result = apiService.fetchAll(today, today, "");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAtcId()).isEqualTo("A1");
        assertThat(result.get(1).getAtcId()).isEqualTo("A2");
    }

    private Lost112Properties createProperties() {
        Lost112Properties properties = new Lost112Properties();
        properties.setBaseUrl("http://localhost");
        properties.setApiKey("test-key");

        // Timeouts 설정
        Lost112Properties.Timeouts timeouts = new Lost112Properties.Timeouts();
        timeouts.setConnectMs(5000);
        timeouts.setReadMs(30000);
        properties.setTimeouts(timeouts);

        // Retry 설정
        Lost112Properties.Retry retry = new Lost112Properties.Retry();
        retry.setMaxAttempts(3);
        retry.setDelayMs(1000);
        properties.setRetry(retry);

        // Page 설정
        Lost112Properties.Page page = new Lost112Properties.Page();
        page.setSize(100);
        page.setSleepMs(500);
        properties.setPage(page);

        return properties;
    }
}
