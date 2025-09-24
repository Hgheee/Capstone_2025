package com.lostfound.capstonebackend.domain.lost112.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

/**
 * Python 데이터 수집 스크립트의 실행 결과를 담는 DTO입니다.
 * 스크립트 실행의 성공 여부, 결과 메시지, 수집된 데이터 수, 실행 시간 등 상세 정보를 포함합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PythonCollectionResult {

    /**
     * 스크립트 실행 및 결과 처리가 성공했는지 여부
     */
    private boolean success;

    /**
     * 스크립트의 표준 출력(stdout) 원본 문자열
     */
    private String output;

    /**
     * 실행 결과를 요약하는 사용자 친화적 메시지
     */
    private String message;

    /**
     * 스크립트 실행을 통해 수집된 총 아이템 개수
     */
    private int totalCollected;

    /**
     * 작업이 실행된 시간
     */
    private LocalDateTime executedAt;

    /**
     * 스크립트 실행과 관련된 추가적인 메타데이터 맵
     */
    @Builder.Default
    private Map<String, Object> meta = Collections.emptyMap();

    /**
     * JSON 응답에서 아이템 리스트가 위치한 경로 (예: "data.items")
     */
    private String itemsPath;

    /**
     * 스크립트의 표준 에러(stderr) 출력 미리보기 (일부)
     */
    private String stderrPreview;

    /**
     * 스크립트 실행 시 사용된 동시성 수준
     */
    private int concurrency;

    /**
     * 총 실행 시간 (밀리초)
     */
    private long durationMillis;

    /**
     * 메타데이터 맵을 반환합니다.
     * null을 반환하지 않고, 항상 수정 불가능한 맵을 반환하여 안정성을 보장합니다.
     * @return 수정 불가능한 메타데이터 맵
     */
    public Map<String, Object> getMeta() {
        return meta == null ? Collections.emptyMap() : Collections.unmodifiableMap(meta);
    }
}
