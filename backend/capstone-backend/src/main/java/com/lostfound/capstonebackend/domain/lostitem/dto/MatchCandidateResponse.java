package com.lostfound.capstonebackend.domain.lostitem.dto;

/**
 * 매칭 후보 응답 DTO
 * 
 * @param item 분실물 정보
 * @param score 매칭 점수 (0.0 ~ 1.0)
 * @param distanceScore 거리 점수
 * @param timeScore 시간 점수
 * @param categoryScore 카테고리 점수
 * @param textScore 텍스트 점수
 * @param tagScore 태그 점수
 */
public record MatchCandidateResponse(
        LostItemResponse item,
        double score,
        double distanceScore,
        double timeScore,
        double categoryScore,
        double textScore,
        double tagScore
) {
}





