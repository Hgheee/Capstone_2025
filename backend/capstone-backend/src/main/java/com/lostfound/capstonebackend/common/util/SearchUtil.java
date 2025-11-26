package com.lostfound.capstonebackend.common.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 검색 기능을 위한 유틸리티 클래스
 * 검색어 토큰화, 유사도 계산, 가중치 적용 등을 제공합니다.
 */
public class SearchUtil {

    /**
     * 검색어를 토큰으로 분리합니다.
     * 공백, 쉼표, 하이픈 등을 기준으로 분리합니다.
     * 
     * @param searchText 검색어
     * @return 토큰 리스트 (빈 문자열 제외)
     */
    public static List<String> tokenize(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        return Arrays.stream(searchText.trim().split("[\\s,\\-_]+"))
                .filter(token -> !token.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    /**
     * 두 문자열의 유사도를 계산합니다 (Levenshtein Distance 기반).
     * 0.0 (완전히 다름) ~ 1.0 (완전히 같음)
     * 
     * @param s1 첫 번째 문자열
     * @param s2 두 번째 문자열
     * @return 유사도 (0.0 ~ 1.0)
     */
    public static double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        
        if (s1.equalsIgnoreCase(s2)) {
            return 1.0;
        }
        
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) {
            return 1.0;
        }
        
        int distance = levenshteinDistance(s1.toLowerCase(), s2.toLowerCase());
        return 1.0 - ((double) distance / maxLength);
    }

    /**
     * Levenshtein Distance를 계산합니다.
     * 두 문자열 간의 편집 거리를 계산합니다.
     * 
     * @param s1 첫 번째 문자열
     * @param s2 두 번째 문자열
     * @return 편집 거리
     */
    private static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(
                        Math.min(dp[i - 1][j], dp[i][j - 1]),
                        dp[i - 1][j - 1]
                    );
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }

    /**
     * 검색어가 텍스트에 포함되어 있는지 확인합니다.
     * 부분 일치, 대소문자 무시, 유사도 기반 검색을 지원합니다.
     * 
     * @param text 검색 대상 텍스트
     * @param searchTerm 검색어
     * @param minSimilarity 최소 유사도 (0.0 ~ 1.0)
     * @return 매칭 여부
     */
    public static boolean matches(String text, String searchTerm, double minSimilarity) {
        if (text == null || searchTerm == null) {
            return false;
        }
        
        String lowerText = text.toLowerCase();
        String lowerSearch = searchTerm.toLowerCase();
        
        // 정확한 일치
        if (lowerText.contains(lowerSearch)) {
            return true;
        }
        
        // 토큰 기반 검색
        List<String> searchTokens = tokenize(searchTerm);
        List<String> textTokens = tokenize(text);
        
        for (String searchToken : searchTokens) {
            boolean found = false;
            for (String textToken : textTokens) {
                if (textToken.contains(searchToken) || searchToken.contains(textToken)) {
                    found = true;
                    break;
                }
                // 유사도 기반 매칭
                if (calculateSimilarity(textToken, searchToken) >= minSimilarity) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 검색어와 텍스트의 관련도를 계산합니다.
     * 관련도가 높을수록 검색 결과 상단에 표시됩니다.
     * 
     * @param text 검색 대상 텍스트
     * @param searchTerm 검색어
     * @param fieldWeight 필드별 가중치 (예: 제목=1.0, 설명=0.5)
     * @return 관련도 점수 (0.0 ~ 1.0)
     */
    public static double calculateRelevance(String text, String searchTerm, double fieldWeight) {
        if (text == null || searchTerm == null || text.isEmpty() || searchTerm.isEmpty()) {
            return 0.0;
        }
        
        String lowerText = text.toLowerCase();
        String lowerSearch = searchTerm.toLowerCase();
        
        double score = 0.0;
        
        // 1. 정확한 일치 (가장 높은 점수)
        if (lowerText.equals(lowerSearch)) {
            score = 1.0;
        }
        // 2. 시작 부분 일치
        else if (lowerText.startsWith(lowerSearch)) {
            score = 0.9;
        }
        // 3. 포함 여부
        else if (lowerText.contains(lowerSearch)) {
            score = 0.7;
        }
        // 4. 토큰 기반 매칭
        else {
            List<String> searchTokens = tokenize(searchTerm);
            List<String> textTokens = tokenize(text);
            
            int matchedTokens = 0;
            double similaritySum = 0.0;
            
            for (String searchToken : searchTokens) {
                double maxSimilarity = 0.0;
                for (String textToken : textTokens) {
                    if (textToken.contains(searchToken) || searchToken.contains(textToken)) {
                        maxSimilarity = Math.max(maxSimilarity, 0.8);
                    }
                    maxSimilarity = Math.max(maxSimilarity, calculateSimilarity(textToken, searchToken));
                }
                if (maxSimilarity > 0.5) {
                    matchedTokens++;
                    similaritySum += maxSimilarity;
                }
            }
            
            if (matchedTokens > 0) {
                score = (similaritySum / searchTokens.size()) * 0.6;
            }
        }
        
        return score * fieldWeight;
    }

    /**
     * 검색어를 정규화합니다.
     * 공백 제거, 대소문자 통일, 특수문자 처리 등을 수행합니다.
     * 
     * @param searchText 원본 검색어
     * @return 정규화된 검색어
     */
    public static String normalize(String searchText) {
        if (searchText == null) {
            return "";
        }
        
        return searchText.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }

    /**
     * 검색어 확장 (동의어, 유사어 처리).
     * 예: "핸드폰" -> ["핸드폰", "스마트폰", "휴대폰", "전화기"]
     * 
     * @param searchTerm 원본 검색어
     * @return 확장된 검색어 리스트
     */
    public static List<String> expandSearchTerms(String searchTerm) {
        List<String> expanded = new ArrayList<>();
        expanded.add(searchTerm);
        
        // 동의어 사전 (간단한 예시)
        Map<String, List<String>> synonyms = new HashMap<>();
        synonyms.put("핸드폰", Arrays.asList("스마트폰", "휴대폰", "전화기", "모바일"));
        synonyms.put("지갑", Arrays.asList("월렛", "카드지갑"));
        synonyms.put("가방", Arrays.asList("백", "핸드백", "백팩"));
        synonyms.put("노트북", Arrays.asList("랩탑", "컴퓨터"));
        synonyms.put("우산", Arrays.asList("양산", "우산"));
        synonyms.put("에어팟", Arrays.asList("이어폰", "헤드폰", "무선이어폰"));
        
        String normalized = normalize(searchTerm);
        if (synonyms.containsKey(normalized)) {
            expanded.addAll(synonyms.get(normalized));
        }
        
        return expanded.stream().distinct().collect(Collectors.toList());
    }
}


