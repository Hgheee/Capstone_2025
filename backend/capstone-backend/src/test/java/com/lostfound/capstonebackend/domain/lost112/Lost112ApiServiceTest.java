package com.lostfound.capstonebackend.domain.lost112;

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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Lost112ApiServiceTest {

    @Test
    @DisplayName("LOST112 API 호출 성공")
    void fetchLostItemsSuccess() {
        // Given
        Lost112Properties properties = createProperties();
        String json = "{" +
                "\"response\": {" +
                "\"header\": {\"resultCode\": \"00\", \"resultMsg\": \"OK\"}," +
                "\"body\": {" +
                "\"items\": {\"item\": [{" +
                "\"atcId\": \"A1\", \"fdPrdtNm\": \"지갑\", \"fdSbjt\": \"갈색 지갑\"," +
                "\"fdYmd\": \"20240101\", \"prdtClNm\": \"지갑\", \"clrNm\": \"갈색\"," +
                "\"depPlace\": \"서울경찰서\", \"fdFilePathImg\": \"http://image\"" +
                "}]}" +
                "}" +
                "}" +
                "}";
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .build());

        Lost112ApiService apiService = new Lost112ApiService(properties, WebClient.builder().exchangeFunction(exchangeFunction));

        // When
        List<Lost112ItemDto> result = apiService.fetchLostItems(1, 10);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAtcId()).isEqualTo("A1");
    }

    @Test
    @DisplayName("LOST112 API 호출 실패 코드")
    void fetchLostItemsFailure() {
        // Given
        Lost112Properties properties = createProperties();
        String json = "{" +
                "\"response\": {" +
                "\"header\": {\"resultCode\": \"99\", \"resultMsg\": \"ERROR\"}" +
                "}" +
                "}";
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .build());

        Lost112ApiService apiService = new Lost112ApiService(properties, WebClient.builder().exchangeFunction(exchangeFunction));

        // When
        List<Lost112ItemDto> result = apiService.fetchLostItems(1, 10);

        // Then
        assertThat(result).isEmpty();
    }

    private Lost112Properties createProperties() {
        Lost112Properties properties = new Lost112Properties();
        properties.setBaseUrl("http://localhost");
        properties.setApiKey("test-key");
        return properties;
    }
}
