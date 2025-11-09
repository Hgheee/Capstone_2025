package com.lostfound.capstonebackend.common.util;

import java.util.*;

/**
 * LOST112 API에서 사용하는 지역 코드 상수
 */
public class Lost112RegionCodes {

    /**
     * 지역 코드 매핑 (코드 -> 지역명)
     */
    public static final Map<String, String> REGION_CODE_TO_NAME = new LinkedHashMap<>() {{
        put("11", "서울특별시");
        put("26", "부산광역시");
        put("27", "대구광역시");
        put("28", "인천광역시");
        put("29", "광주광역시");
        put("30", "대전광역시");
        put("31", "울산광역시");
        put("36", "세종특별자치시");
        put("41", "경기도");
        put("42", "강원특별자치도");
        put("43", "충청북도");
        put("44", "충청남도");
        put("45", "전북특별자치도");
        put("46", "전라남도");
        put("47", "경상북도");
        put("48", "경상남도");
        put("50", "제주특별자치도");
    }};

    /**
     * 지역명에서 코드 찾기
     */
    public static final Map<String, String> REGION_NAME_TO_CODE = new HashMap<>() {{
        REGION_CODE_TO_NAME.forEach((code, name) -> put(name, code));
        // 약칭도 추가
        put("서울", "11");
        put("부산", "26");
        put("대구", "27");
        put("인천", "28");
        put("광주", "29");
        put("대전", "30");
        put("울산", "31");
        put("세종", "36");
        put("경기", "41");
        put("강원", "42");
        put("충북", "43");
        put("충남", "44");
        put("전북", "45");
        put("전남", "46");
        put("경북", "47");
        put("경남", "48");
        put("제주", "50");
    }};

    /**
     * 모든 지역 코드 목록
     */
    public static List<String> getAllRegionCodes() {
        return new ArrayList<>(REGION_CODE_TO_NAME.keySet());
    }

    /**
     * 주요 지역 코드 목록 (서울, 경기, 부산, 인천)
     */
    public static List<String> getMajorRegionCodes() {
        return Arrays.asList("11", "41", "26", "28");
    }

    /**
     * 지역명으로 코드 찾기
     */
    public static String getCodeByName(String name) {
        return REGION_NAME_TO_CODE.get(name);
    }

    /**
     * 코드로 지역명 찾기
     */
    public static String getNameByCode(String code) {
        return REGION_CODE_TO_NAME.get(code);
    }
}

