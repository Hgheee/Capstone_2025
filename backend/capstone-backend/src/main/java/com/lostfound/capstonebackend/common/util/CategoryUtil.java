package com.lostfound.capstonebackend.common.util;

import java.util.*;

/**
 * 카테고리 매핑 유틸리티
 * 프론트엔드의 카테고리 이름을 백엔드에 저장된 카테고리 이름으로 매핑합니다.
 */
public class CategoryUtil {
    
    /**
     * 프론트엔드 카테고리 -> 백엔드 카테고리 매핑
     */
    private static final Map<String, List<String>> CATEGORY_MAPPING = new HashMap<>();
    
    static {
        // 지갑 -> 가방/지갑, 지갑, WALLET 등
        CATEGORY_MAPPING.put("지갑", Arrays.asList("가방/지갑", "지갑", "WALLET", "지갑/카드"));
        
        // 가방 -> 가방/지갑, 가방, BAG 등
        CATEGORY_MAPPING.put("가방", Arrays.asList("가방/지갑", "가방", "BAG", "가방/배낭"));
        
        // 핸드폰 -> 전자기기, PHONE, 핸드폰/휴대폰 등
        CATEGORY_MAPPING.put("핸드폰", Arrays.asList("전자기기", "PHONE", "핸드폰", "핸드폰/휴대폰", "휴대폰", "스마트폰"));
        
        // 노트북 -> 전자기기, 노트북 등
        CATEGORY_MAPPING.put("노트북", Arrays.asList("전자기기", "노트북", "컴퓨터", "랩탑"));
        
        // 서류 -> 서류/도서, DOCUMENT, 서류 등
        CATEGORY_MAPPING.put("서류", Arrays.asList("서류/도서", "DOCUMENT", "서류", "서류/문서"));
        
        // 귀중품 -> 귀중품, 귀금속 등
        CATEGORY_MAPPING.put("귀중품", Arrays.asList("귀중품", "귀금속", "시계", "장신구"));
        
        // 의류 -> 의류/잡화, CLOTHING, 의류 등
        CATEGORY_MAPPING.put("의류", Arrays.asList("의류/잡화", "CLOTHING", "의류", "의류/옷"));
        
        // 우산 -> 우산, UMBRELLA 등
        CATEGORY_MAPPING.put("우산", Arrays.asList("우산", "UMBRELLA"));
        
        // 도서 -> 서류/도서, 도서 등
        CATEGORY_MAPPING.put("도서", Arrays.asList("서류/도서", "도서", "서적", "책"));
        
        // 기타 -> 기타, ETC 등
        CATEGORY_MAPPING.put("기타", Arrays.asList("기타", "ETC"));
    }
    
    /**
     * 프론트엔드 카테고리 이름에 해당하는 모든 가능한 백엔드 카테고리 이름 목록을 반환합니다.
     * @param frontendCategory 프론트엔드 카테고리 이름
     * @return 백엔드 카테고리 이름 목록 (매핑이 없으면 원본 카테고리만 포함)
     */
    public static List<String> getBackendCategories(String frontendCategory) {
        if (frontendCategory == null || frontendCategory.trim().isEmpty() || "전체".equals(frontendCategory)) {
            return Collections.emptyList();
        }
        
        List<String> categories = CATEGORY_MAPPING.get(frontendCategory.trim());
        if (categories != null && !categories.isEmpty()) {
            // 원본 카테고리도 포함
            List<String> result = new ArrayList<>(categories);
            result.add(frontendCategory.trim());
            return result;
        }
        
        // 매핑이 없으면 원본 카테고리만 반환
        return Collections.singletonList(frontendCategory.trim());
    }
}

