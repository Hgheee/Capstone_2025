package com.lostfound.capstonebackend.domain.lost112;

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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 경찰청 유실물 종합관리시스템(LOST112)의 외부 API와 통신하여 데이터를 가져오는 서비스입니다.
 * WebClient를 사용하여 비동기 방식으로 API를 호출합니다.
 */
@Service
@Slf4j
public class Lost112ApiService {

    private final Lost112Properties lost112Properties;
    private final WebClient webClient;

    /**
     * Lost112ApiService 생성자입니다.
     * WebClient를 초기화하며, 연결 및 응답 시간 초과 설정을 application.yml의 값으로 구성합니다.
     * @param lost112Properties LOST112 API 관련 설정 정보
     * @param webClientBuilder WebClient 생성을 위한 빌더
     */
    public Lost112ApiService(Lost112Properties lost112Properties, WebClient.Builder webClientBuilder) {
        this.lost112Properties = lost112Properties;
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, lost112Properties.getTimeouts().getConnectMs())
                .responseTimeout(Duration.ofMillis(lost112Properties.getTimeouts().getReadMs()));

        this.webClient = webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * LOST112 API를 호출하여 특정 페이지의 분실물 데이터를 가져옵니다.
     * @param pageNo 조회할 페이지 번호
     * @param numOfRows 한 페이지에 포함될 데이터의 수
     * @return API로부터 받은 분실물 데이터 리스트. 응답이 없거나 에러 발생 시 빈 리스트를 반환합니다.
     */
    public List<Lost112ItemDto> fetchLostItems(int pageNo, int numOfRows) {
        return fetchLostItems(pageNo, numOfRows, null, null, null);
    }

    /**
     * LOST112 API를 호출하여 지역 및 날짜 범위를 지정하여 분실물 데이터를 가져옵니다.
     * @param pageNo 조회할 페이지 번호
     * @param numOfRows 한 페이지에 포함될 데이터의 수
     * @param regionCode 지역 코드 (예: "11" = 서울, "26" = 부산)
     * @param startYmd 시작 날짜 (YYYYMMDD 형식)
     * @param endYmd 종료 날짜 (YYYYMMDD 형식)
     * @return API로부터 받은 분실물 데이터 리스트
     */
    public List<Lost112ItemDto> fetchLostItems(int pageNo, int numOfRows, String regionCode, String startYmd, String endYmd) {
        try {
            String url = buildApiUrl(pageNo, numOfRows, regionCode, startYmd, endYmd);
            log.info("LOST112 API 호출: {}", url);

            // WebClient를 사용하여 비동기 API 호출 및 응답을 동기적으로 대기
            Lost112ResponseDto response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Lost112ResponseDto.class)
                    .block(Duration.ofMillis(lost112Properties.getTimeouts().getReadMs()));

            // API 응답 유효성 검사
            if (response == null || response.getResponse() == null) {
                log.warn("LOST112 API로부터 유효하지 않은 응답을 받았습니다.");
                return new ArrayList<>();
            }

            // API 자체 헤더의 결과 코드 확인
            Lost112ResponseDto.Header header = response.getResponse().getHeader();
            if (!"00".equals(header.getResultCode())) {
                log.error("LOST112 API 오류: 코드={}, 메시지={}", header.getResultCode(), header.getResultMsg());
                return new ArrayList<>();
            }

            // 실제 데이터(body) 존재 여부 확인
            Lost112ResponseDto.Body body = response.getResponse().getBody();
            if (body == null || body.getItems() == null || body.getItems().getItem() == null) {
                log.info("LOST112 API: 페이지 {}에 데이터가 없습니다.", pageNo);
                return new ArrayList<>();
            }

            List<Lost112ItemDto> items = body.getItems().getItem();
            log.info("LOST112 API: {}개의 아이템을 가져왔습니다.", items.size());

            return items;

        } catch (WebClientResponseException e) {
            log.error("LOST112 API HTTP 오류: 상태코드={}, 응답바디={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("LOST112 API 호출 중 알 수 없는 오류 발생", e);
            return new ArrayList<>();
        }
    }

    /**
     * LOST112 API의 모든 분실물 데이터를 가져옵니다.
     * 데이터가 더 이상 없을 때까지 페이지를 순차적으로 호출하여 모든 데이터를 수집합니다.
     * API 과부하를 방지하기 위해 각 페이지 요청 사이에 지연 시간(sleep)을 둡니다.
     * 무한 루프를 방지하기 위해 최대 100페이지까지만 조회합니다.
     * @return API로부터 수집된 모든 분실물 데이터 리스트
     */
    public List<Lost112ItemDto> fetchAllLostItems() {
        List<Lost112ItemDto> allItems = new ArrayList<>();
        int pageNo = 1;
        int pageSize = lost112Properties.getPage().getSize();
        int sleepMs = lost112Properties.getPage().getSleepMs();

        while (true) {
            List<Lost112ItemDto> pageItems = fetchLostItems(pageNo, pageSize);

            if (pageItems.isEmpty()) {
                log.info("LOST112 데이터 수집 완료. 총 {}개 아이템 수집.", allItems.size());
                break;
            }

            allItems.addAll(pageItems);
            log.info("LOST112 페이지 {} 병합 완료. 누적 아이템 수: {}", pageNo, allItems.size());

            pageNo++;

            // API 서버 부하를 줄이기 위한 요청 간 지연
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("LOST112 데이터 수집 중단됨");
                break;
            }

            // 무한 페이지 조회를 방지하기 위한 안전 장치
            if (pageNo > 100) {
                log.warn("최대 페이지 제한(100)에 도달하여 수집을 중단합니다.");
                break;
            }
        }

        return allItems;
    }

    /**
     * LOST112 API 요청을 위한 전체 URL을 생성합니다.
     * @param pageNo 페이지 번호
     * @param numOfRows 페이지 당 행 수
     * @param regionCode 지역 코드 (선택사항)
     * @param startYmd 시작 날짜 (선택사항, YYYYMMDD)
     * @param endYmd 종료 날짜 (선택사항, YYYYMMDD)
     * @return API 명세에 맞는 완전한 URL 문자열
     */
    private String buildApiUrl(int pageNo, int numOfRows, String regionCode, String startYmd, String endYmd) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(lost112Properties.getBaseUrl())
                .path("/getLostGoodsInfoAccToClAreaPd")
                .queryParam("serviceKey", lost112Properties.getApiKey())
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", Math.min(numOfRows, 100)) // API 최대 허용치는 100
                .queryParam("type", "json");
        
        // 지역 코드가 있으면 추가
        if (regionCode != null && !regionCode.trim().isEmpty()) {
            builder.queryParam("LST_LCT_CD", regionCode);
            log.info("지역 코드 설정: {}", regionCode);
        }
        
        // 시작 날짜가 있으면 추가
        if (startYmd != null && !startYmd.trim().isEmpty()) {
            builder.queryParam("START_YMD", startYmd);
            log.info("시작 날짜 설정: {}", startYmd);
        }
        
        // 종료 날짜가 있으면 추가
        if (endYmd != null && !endYmd.trim().isEmpty()) {
            builder.queryParam("END_YMD", endYmd);
            log.info("종료 날짜 설정: {}", endYmd);
        }
        
        return builder.build().toUriString();
    }

    /**
     * 여러 지역의 데이터를 병렬로 수집합니다.
     * @param regionCodes 수집할 지역 코드 목록
     * @param startYmd 시작 날짜
     * @param endYmd 종료 날짜
     * @return 모든 지역의 분실물 데이터
     */
    public List<Lost112ItemDto> fetchAllRegions(List<String> regionCodes, String startYmd, String endYmd) {
        List<Lost112ItemDto> allItems = new ArrayList<>();
        int pageSize = lost112Properties.getPage().getSize();
        int sleepMs = lost112Properties.getPage().getSleepMs();
        
        for (String regionCode : regionCodes) {
            log.info("지역 코드 {} 데이터 수집 시작", regionCode);
            
            int pageNo = 1;
            while (pageNo <= 10) { // 각 지역당 최대 10페이지
                List<Lost112ItemDto> pageItems = fetchLostItems(pageNo, pageSize, regionCode, startYmd, endYmd);
                
                if (pageItems.isEmpty()) {
                    log.info("지역 {} - 더 이상 데이터 없음. 페이지: {}", regionCode, pageNo);
                    break;
                }
                
                allItems.addAll(pageItems);
                log.info("지역 {} - 페이지 {} 완료. 수집: {}개, 누적: {}개", 
                        regionCode, pageNo, pageItems.size(), allItems.size());
                
                pageNo++;
                
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("데이터 수집 중단됨");
                    return allItems;
                }
            }
        }
        
        log.info("전체 지역 데이터 수집 완료. 총 {}개", allItems.size());
        return allItems;
    }
}
