package com.lostfound.capstonebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 애플리케이션의 메인 클래스입니다.
 * {@link SpringBootApplication} 애노테이션은 다음 세 가지를 포함합니다:
 * <ul>
 *     <li>{@code @Configuration}: 이 클래스를 구성 소스로 태그합니다.</li>
 *     <li>{@code @EnableAutoConfiguration}: Spring Boot의 자동 구성 메커니즘을 활성화합니다.</li>
 *     <li>{@code @ComponentScan}: 이 패키지 및 하위 패키지에서 컴포넌트(@Component, @Service 등)를 스캔합니다.</li>
 * </ul>
 */
@SpringBootApplication
public class CapstoneBackendApplication {

    /**
     * 애플리케이션의 주 진입점(entry point)입니다.
     * 이 메소드는 Spring Boot 애플리케이션을 시작합니다.
     * @param args 커맨드 라인 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(CapstoneBackendApplication.class, args);
    }
}
