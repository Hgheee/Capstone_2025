package com.lostfound.capstonebackend.common.util;

import com.lostfound.capstonebackend.domain.lostitem.LostItem;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 분실물 매칭 점수 계산을 위한 유틸리티 클래스
 * 거리, 시간, 카테고리, 텍스트, 태그 점수를 계산합니다.
 */
public class MatchingScoreUtil {

    // 가중치 상수
    private static final double WEIGHT_DISTANCE = 0.35;
    private static final double WEIGHT_TIME = 0.25;
    private static final double WEIGHT_CATEGORY = 0.20;
    private static final double WEIGHT_TEXT = 0.12;
    private static final double WEIGHT_TAG = 0.08;

    // 거리 계산 상수 (Haversine 공식 사용)
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * 두 분실물 간의 최종 매칭 점수를 계산합니다.
     * 
     * @param lostItem 분실물 (LOST 타입)
     * @param foundItem 습득물 (FOUND 타입)
     * @return 최종 매칭 점수 (0.0 ~ 1.0)
     */
    public static double calculateFinalScore(LostItem lostItem, LostItem foundItem) {
        // 기본 조건 검증
        if (!isValidMatch(lostItem, foundItem)) {
            return 0.0;
        }

        // 각 점수 계산
        double distanceScore = calculateDistanceScore(lostItem, foundItem);
        double timeScore = calculateTimeScore(lostItem, foundItem);
        double categoryScore = calculateCategoryScore(lostItem, foundItem);
        double textScore = calculateTextScore(lostItem, foundItem);
        double tagScore = calculateTagScore(lostItem, foundItem);

        // 가중합 계산
        double finalScore = 
            WEIGHT_DISTANCE * distanceScore +
            WEIGHT_TIME * timeScore +
            WEIGHT_CATEGORY * categoryScore +
            WEIGHT_TEXT * textScore +
            WEIGHT_TAG * tagScore;

        return Math.min(1.0, Math.max(0.0, finalScore));
    }

    /**
     * 기본 매칭 조건을 검증합니다.
     * - 서로 다른 타입 (LOST - FOUND)
     * - 카테고리 일치
     * - 시간 조건 충족 (분실일이 습득일보다 이전)
     * 
     * @param lostItem 분실물
     * @param foundItem 습득물
     * @return 유효한 매칭 여부
     */
    public static boolean isValidMatch(LostItem lostItem, LostItem foundItem) {
        // 타입 검증: LOST와 FOUND만 매칭
        if (lostItem.getItemType() == null || foundItem.getItemType() == null) {
            return false;
        }
        if (lostItem.getItemType() == foundItem.getItemType()) {
            return false;
        }

        // 카테고리 일치 확인
        if (lostItem.getCategory() == null || foundItem.getCategory() == null) {
            return false;
        }
        if (!lostItem.getCategory().equals(foundItem.getCategory())) {
            return false;
        }

        // 시간 조건: 분실일이 습득일보다 이전이어야 함
        if (lostItem.getFoundDate() != null && foundItem.getFoundDate() != null) {
            if (lostItem.getFoundDate().isAfter(foundItem.getFoundDate())) {
                return false;
            }
        }

        return true;
    }

    /**
     * 거리 점수를 계산합니다 (0.0 ~ 1.0)
     * 반경 2KM 이내: 1.0
     * 반경 5KM 이내: 0.8
     * 반경 10KM 이내: 0.5
     * 그 외: 거리에 따라 선형 감소
     * 
     * @param lostItem 분실물
     * @param foundItem 습득물
     * @return 거리 점수
     */
    public static double calculateDistanceScore(LostItem lostItem, LostItem foundItem) {
        if (lostItem.getLatitude() == null || lostItem.getLongitude() == null ||
            foundItem.getLatitude() == null || foundItem.getLongitude() == null) {
            // 좌표가 없으면 기본 점수 (0.5)
            return 0.5;
        }

        double distance = calculateDistance(
            lostItem.getLatitude(), lostItem.getLongitude(),
            foundItem.getLatitude(), foundItem.getLongitude()
        );

        // 거리에 따른 점수 계산
        if (distance <= 2.0) {
            return 1.0;
        } else if (distance <= 5.0) {
            return 0.8 + (2.0 / 3.0) * (1.0 - (distance - 2.0) / 3.0);
        } else if (distance <= 10.0) {
            return 0.5 + 0.3 * (1.0 - (distance - 5.0) / 5.0);
        } else {
            // 10KM 이상은 선형 감소 (최소 0.1)
            return Math.max(0.1, 0.5 * (1.0 - (distance - 10.0) / 20.0));
        }
    }

    /**
     * Haversine 공식을 사용하여 두 좌표 간의 거리를 계산합니다 (단위: KM)
     * 
     * @param lat1 첫 번째 위도
     * @param lon1 첫 번째 경도
     * @param lat2 두 번째 위도
     * @param lon2 두 번째 경도
     * @return 거리 (KM)
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * 시간 점수를 계산합니다 (0.0 ~ 1.0)
     * 시간 차이가 적을수록 높은 점수
     * 
     * @param lostItem 분실물
     * @param foundItem 습득물
     * @return 시간 점수
     */
    public static double calculateTimeScore(LostItem lostItem, LostItem foundItem) {
        if (lostItem.getFoundDate() == null || foundItem.getFoundDate() == null) {
            return 0.5; // 날짜 정보가 없으면 기본 점수
        }

        long daysDiff = ChronoUnit.DAYS.between(lostItem.getFoundDate(), foundItem.getFoundDate());

        // 음수면 이미 필터링되었지만 안전장치
        if (daysDiff < 0) {
            return 0.0;
        }

        // 시간 차이에 따른 점수 계산
        if (daysDiff == 0) {
            return 1.0;
        } else if (daysDiff <= 1) {
            return 0.9;
        } else if (daysDiff <= 3) {
            return 0.8;
        } else if (daysDiff <= 7) {
            return 0.6;
        } else if (daysDiff <= 14) {
            return 0.4;
        } else if (daysDiff <= 30) {
            return 0.2;
        } else {
            return 0.1;
        }
    }

    /**
     * 카테고리 점수를 계산합니다 (0.0 ~ 1.0)
     * 정확 일치: 1.0
     * 유사 카테고리: 0.7
     * 불일치: 0.0
     * 
     * @param lostItem 분실물
     * @param foundItem 습득물
     * @return 카테고리 점수
     */
    public static double calculateCategoryScore(LostItem lostItem, LostItem foundItem) {
        if (lostItem.getCategory() == null || foundItem.getCategory() == null) {
            return 0.0;
        }

        String lostCategory = lostItem.getCategory().trim().toLowerCase();
        String foundCategory = foundItem.getCategory().trim().toLowerCase();

        // 정확 일치
        if (lostCategory.equals(foundCategory)) {
            return 1.0;
        }

        // 유사 카테고리 매핑
        if (isSimilarCategory(lostCategory, foundCategory)) {
            return 0.7;
        }

        return 0.0;
    }

    /**
     * 유사 카테고리인지 확인합니다.
     * 
     * @param cat1 첫 번째 카테고리
     * @param cat2 두 번째 카테고리
     * @return 유사 여부
     */
    private static boolean isSimilarCategory(String cat1, String cat2) {
        // 유사 카테고리 매핑 (예시)
        if (cat1.contains("가방") && cat2.contains("백")) {
            return true;
        }
        if (cat1.contains("핸드폰") && (cat2.contains("스마트폰") || cat2.contains("휴대폰"))) {
            return true;
        }
        if (cat1.contains("노트북") && cat2.contains("컴퓨터")) {
            return true;
        }
        return false;
    }

    /**
     * 텍스트 점수를 계산합니다 (0.0 ~ 1.0)
     * 제목과 설명에 대해 BM25 기반 점수 계산
     * 
     * @param lostItem 분실물
     * @param foundItem 습득물
     * @return 텍스트 점수
     */
    public static double calculateTextScore(LostItem lostItem, LostItem foundItem) {
        double titleScore = calculateTextSimilarity(
            lostItem.getTitle(), foundItem.getTitle()
        );
        double descScore = calculateTextSimilarity(
            lostItem.getDescription(), foundItem.getDescription()
        );

        // 제목 가중치 0.7, 설명 가중치 0.3
        return 0.7 * titleScore + 0.3 * descScore;
    }

    /**
     * 두 텍스트 간의 유사도를 계산합니다 (간단한 버전)
     * 실제 BM25 알고리즘 대신 토큰 기반 유사도 사용
     * 
     * @param text1 첫 번째 텍스트
     * @param text2 두 번째 텍스트
     * @return 유사도 점수 (0.0 ~ 1.0)
     */
    private static double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isEmpty() || text2.isEmpty()) {
            return 0.0;
        }

        // 토큰화
        String[] tokens1 = text1.toLowerCase().split("\\s+");
        String[] tokens2 = text2.toLowerCase().split("\\s+");

        // 공통 토큰 개수 계산
        int commonTokens = 0;
        for (String token1 : tokens1) {
            for (String token2 : tokens2) {
                if (token1.equals(token2)) {
                    commonTokens++;
                    break;
                }
            }
        }

        // Jaccard 유사도
        int totalUniqueTokens = tokens1.length + tokens2.length - commonTokens;
        if (totalUniqueTokens == 0) {
            return 0.0;
        }

        return (double) commonTokens / totalUniqueTokens;
    }

    /**
     * 태그 점수를 계산합니다 (0.0 ~ 1.0)
     * 색상, 브랜드, 소재 등의 키워드 기반 유사도
     * 
     * @param lostItem 분실물
     * @param foundItem 습득물
     * @return 태그 점수
     */
    public static double calculateTagScore(LostItem lostItem, LostItem foundItem) {
        double colorScore = 0.0;
        if (lostItem.getColor() != null && foundItem.getColor() != null) {
            String lostColor = lostItem.getColor().toLowerCase().trim();
            String foundColor = foundItem.getColor().toLowerCase().trim();
            
            if (lostColor.equals(foundColor)) {
                colorScore = 1.0;
            } else if (lostColor.contains(foundColor) || foundColor.contains(lostColor)) {
                colorScore = 0.7;
            }
        }

        // 추가 태그 점수는 여기에 구현 가능 (브랜드, 소재 등)
        // 현재는 색상만 고려

        return colorScore;
    }
}


