package com.lostfound.capstonebackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * LOST112 서비스 연동과 관련된 설정.
 */
@Component
@ConfigurationProperties(prefix = "lost112")
@Getter
@Setter
public class Lost112Properties {

    /**
     * LOST112 API 설정
     */
    private Api api = new Api();

    /**
     * 페이지네이션 설정
     */
    private Page page = new Page();

    /**
     * 재시도 설정
     */
    private Retry retry = new Retry();

    /**
     * 타임아웃 설정 (신규 위치)
     */
    private Timeouts timeouts;

    /**
     * 타임아웃 설정
     */
    @Getter
    @Setter
    public static class Timeouts {
        private int connectMs = 2000;
        private int readMs = 8000;
    }

    /**
     * 페이지네이션 설정
     */
    @Getter
    @Setter
    public static class Page {
        private int size = 100;
        private int sleepMs = 500;
    }

    /**
     * LOST112 API 설정
     */
    @Getter
    public static class Api {
        private String baseUrl;
        private String apiKey;
        private String serviceKey;
        private Timeouts timeouts = new Timeouts();

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = normalize(apiKey);
            if (!StringUtils.hasText(this.serviceKey)) {
                this.serviceKey = this.apiKey;
            }
        }

        public void setServiceKey(String serviceKey) {
            this.serviceKey = normalize(serviceKey);
            if (!StringUtils.hasText(this.apiKey)) {
                this.apiKey = this.serviceKey;
            }
        }

        public void setTimeouts(Timeouts timeouts) {
            this.timeouts = timeouts;
        }

        public String resolveApiKey() {
            if (StringUtils.hasText(this.apiKey)) {
                return this.apiKey;
            }
            return this.serviceKey;
        }

        private String normalize(String value) {
            return StringUtils.hasText(value) ? value : null;
        }
    }

    /**
     * 재시도 설정
     */
    @Getter
    @Setter
    public static class Retry {
        private int maxAttempts = 3;
        private long delayMs = 1000;
    }

    /**
     * 호환성 유지를 위한 접근자
     */
    public String getBaseUrl() {
        return ensureApi().getBaseUrl();
    }

    public void setBaseUrl(String baseUrl) {
        ensureApi().setBaseUrl(baseUrl);
    }

    public String getApiKey() {
        return ensureApi().resolveApiKey();
    }

    public void setApiKey(String apiKey) {
        ensureApi().setApiKey(apiKey);
    }

    public void setServiceKey(String serviceKey) {
        ensureApi().setServiceKey(serviceKey);
    }

    public Timeouts getTimeouts() {
        if (this.timeouts != null) {
            return this.timeouts;
        }
        Timeouts apiTimeouts = ensureApi().getTimeouts();
        if (apiTimeouts == null) {
            apiTimeouts = new Timeouts();
            ensureApi().setTimeouts(apiTimeouts);
        }
        this.timeouts = apiTimeouts;
        return this.timeouts;
    }

    public void setTimeouts(Timeouts timeouts) {
        this.timeouts = timeouts;
        ensureApi().setTimeouts(timeouts);
    }

    private Api ensureApi() {
        if (api == null) {
            api = new Api();
        }
        return api;
    }
}
