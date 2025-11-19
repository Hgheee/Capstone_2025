package com.lostfound.capstonebackend.common.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * 한글 인코딩 문제를 원천 차단하고 깨진 데이터를 감지/수정하는 유틸리티 클래스
 */
@Slf4j
public class EncodingUtil {

    // 깨진 한글 감지 패턴
    private static final Pattern BROKEN_KOREAN_PATTERN = Pattern.compile(
        "[^가-힣a-zA-Z0-9\\s\\-_.,!?()@#$%&*+=/\\[\\]{}:<>~`|\\\\;\n\r\t]"
    );
    
    // 한글 자음/모음만 있는 패턴 (깨진 데이터 가능성)
    private static final Pattern INCOMPLETE_KOREAN_PATTERN = Pattern.compile(
        "[ㄱ-ㅎㅏ-ㅣ]"
    );

    /**
     * 문자열의 한글 인코딩을 안전하게 처리합니다.
     * UTF-8로 강제 변환하고 깨진 문자를 제거합니다.
     * 
     * @param text 원본 텍스트
     * @return 안전하게 인코딩된 텍스트 (null 또는 빈 문자열 시 null 반환)
     */
    public static String safeDecode(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        try {
            String trimmed = text.trim();
            
            // 1단계: UTF-8로 재인코딩
            byte[] bytes = trimmed.getBytes(StandardCharsets.UTF_8);
            String decoded = new String(bytes, StandardCharsets.UTF_8);
            
            // 2단계: 깨진 문자 제거
            decoded = removeBrokenCharacters(decoded);
            
            // 3단계: 최종 검증
            if (isBroken(decoded)) {
                log.warn("⚠️ 인코딩 후에도 깨진 텍스트 발견: {} → {}", text.substring(0, Math.min(50, text.length())), decoded);
                return null;
            }
            
            return decoded.trim();
            
        } catch (Exception e) {
            log.error("인코딩 처리 실패: {}", text.substring(0, Math.min(50, text.length())), e);
            return null;
        }
    }

    /**
     * 문자열에서 깨진 문자를 제거합니다.
     * 
     * @param text 원본 텍스트
     * @return 깨진 문자가 제거된 텍스트
     */
    public static String removeBrokenCharacters(String text) {
        if (text == null) {
            return null;
        }

        try {
            // Charset 디코더 생성 (에러 발생 시 대체 문자 사용)
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPLACE);
            decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
            decoder.replaceWith("?");  // ✅ 빈 문자열 대신 "?" 사용 (나중에 제거)

            CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
            encoder.onMalformedInput(CodingErrorAction.REPLACE);
            encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);

            ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(text));
            CharBuffer cbuf = decoder.decode(bbuf);
            
            String cleaned = cbuf.toString();
            
            // 대체 문자 "?" 제거 (깨진 문자를 제거한 것과 동일)
            cleaned = cleaned.replace("?", "");
            
            // 불완전한 한글 자음/모음 제거
            cleaned = INCOMPLETE_KOREAN_PATTERN.matcher(cleaned).replaceAll("");
            
            // 연속된 공백 정리
            cleaned = cleaned.replaceAll("\\s+", " ").trim();
            
            return cleaned;
            
        } catch (Exception e) {
            log.error("깨진 문자 제거 실패", e);
            return text;
        }
    }

    /**
     * 문자열이 깨진 데이터인지 확인합니다.
     * 
     * @param text 검사할 텍스트
     * @return 깨진 데이터인 경우 true
     */
    public static boolean isBroken(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String trimmed = text.trim();
        
        // 1. 깨진 한글 패턴 체크
        if (BROKEN_KOREAN_PATTERN.matcher(trimmed).find()) {
            log.debug("깨진 패턴 감지: {}", trimmed.substring(0, Math.min(30, trimmed.length())));
            return true;
        }
        
        // 2. 불완전한 한글 자음/모음만 있는 경우
        if (INCOMPLETE_KOREAN_PATTERN.matcher(trimmed).find()) {
            log.debug("불완전한 한글 감지: {}", trimmed.substring(0, Math.min(30, trimmed.length())));
            return true;
        }
        
        // 3. 유효한 UTF-8인지 확인
        byte[] bytes = trimmed.getBytes(StandardCharsets.UTF_8);
        String reencoded = new String(bytes, StandardCharsets.UTF_8);
        if (!trimmed.equals(reencoded)) {
            log.debug("UTF-8 재인코딩 불일치: {} vs {}", 
                trimmed.substring(0, Math.min(30, trimmed.length())),
                reencoded.substring(0, Math.min(30, reencoded.length())));
            return true;
        }
        
        return false;
    }

    /**
     * 여러 문자열 중 하나라도 깨진 데이터가 있는지 확인합니다.
     * 
     * @param texts 검사할 텍스트 배열
     * @return 하나라도 깨진 데이터가 있으면 true
     */
    public static boolean hasAnyBroken(String... texts) {
        if (texts == null || texts.length == 0) {
            return false;
        }
        
        for (String text : texts) {
            if (text != null && isBroken(text)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 문자열이 유효한 한글을 포함하고 있는지 확인합니다.
     * 
     * @param text 검사할 텍스트
     * @return 유효한 한글이 포함되어 있으면 true
     */
    public static boolean containsValidKorean(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        return Pattern.compile("[가-힣]+").matcher(text).find();
    }

    /**
     * 데이터 소스별로 적절한 인코딩 처리를 합니다.
     * 
     * @param text 원본 텍스트
     * @param dataSource 데이터 출처 (LOST112, SEOUL_LOST, USER 등)
     * @return 인코딩 처리된 텍스트
     */
    public static String safeDecodeBySource(String text, String dataSource) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        // 1. 기본 안전 디코딩
        String decoded = safeDecode(text);
        
        // 2. 데이터 소스별 추가 처리
        if (decoded != null && dataSource != null) {
            switch (dataSource.toUpperCase()) {
                case "LOST112":
                    // LOST112는 일부 특수문자가 있을 수 있음
                    decoded = decoded.replaceAll("[\\x00-\\x1F\\x7F]", ""); // 제어 문자 제거
                    break;
                case "SEOUL_LOST":
                    // 서울교통공사도 비슷한 처리
                    decoded = decoded.replaceAll("[\\x00-\\x1F\\x7F]", "");
                    break;
                case "USER":
                    // 사용자 데이터는 더 엄격하게
                    if (isBroken(decoded)) {
                        log.warn("사용자 데이터에서 깨진 텍스트 발견: {}", decoded);
                        return null;
                    }
                    break;
            }
        }
        
        return decoded;
    }

    /**
     * 로그 출력용으로 문자열을 안전하게 자릅니다.
     * 
     * @param text 원본 텍스트
     * @param maxLength 최대 길이
     * @return 잘린 텍스트
     */
    public static String safeSubstring(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}

