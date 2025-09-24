package com.lostfound.capstonebackend.config;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LOST112 서비스 연동과 관련된 설정을 담는 클래스입니다.
 * 'application.yml' 파일의 'lost112' 접두사를 가진 속성들이 이 클래스의 필드에 매핑됩니다.
 */
@Component
@ConfigurationProperties(prefix = "lost112")
@Getter
@Setter
public class Lost112Properties {

    /**
     * LOST112 API의 기본 URL
     */
    private String baseUrl;

    /**
     * LOST112 API 호출 시 사용할 API 키
     */
    private String apiKey;

    /**
     * API 호출 시의 타임아웃 설정
     */
    private Timeouts timeouts = new Timeouts();

    /**
     * API 페이지네이션 관련 설정
     */
    private Page page = new Page();

    /**
     * Python 스크립트 실행 관련 설정
     */
    private Python python = new Python();

    /**
     * API 호출 타임아웃 설정
     */
    @Getter
    @Setter
    public static class Timeouts {
        /**
         * 연결 타임아웃 (밀리초, 기본값: 2000)
         */
        private int connectMs = 2000;
        /**
         * 응답 읽기 타임아웃 (밀리초, 기본값: 8000)
         */
        private int readMs = 8000;
    }

    /**
     * API 페이지네이션 관련 설정
     */
    @Getter
    @Setter
    public static class Page {
        /**
         * 한 페이지당 요청할 데이터 수 (기본값: 50)
         */
        private int size = 50;
        /**
         * 각 페이지 요청 사이의 지연 시간 (밀리초, 기본값: 300)
         */
        private int sleepMs = 300;
    }

    /**
     * Python 스크립트 실행 관련 설정
     */
    @Getter
    @Setter
    public static class Python {
        /**
         * Python 스크립트 실행 기능 활성화 여부 (기본값: true)
         */
        private boolean enabled = true;
        /**
         * 실행할 Python 명령어 (예: python, python3)
         */
        private String executable = "python";
        /**
         * 실행할 Python 스크립트의 경로
         */
        private String scriptPath = "../../data/lost112_collect_and_sync_fast.py";
        /**
         * Python 스크립트가 실행될 작업 디렉토리
         */
        private String workDir = "../../data";
        /**
         * Python 스크립트의 출력 파일이 저장될 디렉토리
         */
        private String outputDir = "../../data/out";
        /**
         * Python 스크립트가 사용할 환경변수 파일(.env)의 경로
         */
        private String envFile = "../../data/.env";
        /**
         * 한 번에 요청할 기본 데이터 행 수 (기본값: 100)
         */
        private int defaultRows = 100;
        /**
         * 조회할 최대 페이지 수 (기본값: 20)
         */
        private int defaultMaxPages = 20;
        /**
         * 조회할 기본 날짜 범위 (일, 기본값: 7)
         */
        private int defaultDayRange = 7;
        /**
         * 각 요청 사이의 지연 시간 (초, 기본값: 0.3)
         */
        private double sleepSec = 0.3d;
        /**
         * 스크립트 내부에서 사용할 동시성 수준 (기본값: 2)
         */
        private int defaultConcurrency = 2;
        /**
         * 스크립트 실행 최대 대기 시간 (초, 기본값: 120)
         */
        private long timeoutSec = 120;
        /**
         * 스크립트 출력의 최대 문자 수 (기본값: 200,000)
         */
        private int maxOutputChars = 200_000;
        /**
         * 스크립트 실행 시 추가할 환경 변수 맵
         */
        private Map<String, String> environment = new LinkedHashMap<>();
    }
}
