package com.lostfound.capstonebackend.domain.lostitem.service;

import com.lostfound.capstonebackend.common.util.MatchingScoreUtil;
import com.lostfound.capstonebackend.domain.lostitem.LostItem;
import com.lostfound.capstonebackend.domain.lostitem.LostItemRepository;
import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 분실물 매칭 서비스
 * LOST 타입과 FOUND 타입의 분실물을 매칭하여 후보 리스트를 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LostItemMatchingService {

    private final LostItemRepository lostItemRepository;
    private static final double MATCH_THRESHOLD = 0.60; // 임계치
    private static final int MAX_CANDIDATES = 10; // 최대 후보 개수
    private static final double MAX_DISTANCE_KM = 2.0; // 최대 거리 (KM)

    /**
     * 특정 분실물에 대한 매칭 후보를 찾습니다.
     * 
     * @param lostItemId 분실물 ID (LOST 타입)
     * @return 매칭 후보 리스트 (점수 내림차순, 최대 10개)
     */
    public List<MatchCandidate> findMatchingCandidates(Long lostItemId) {
        LostItem lostItem = lostItemRepository.findById(lostItemId)
                .orElseThrow(() -> new IllegalArgumentException("분실물을 찾을 수 없습니다: " + lostItemId));

        // LOST 타입이 아니면 빈 리스트 반환
        if (lostItem.getItemType() == null || 
            lostItem.getItemType() != LostItem.ItemType.LOST) {
            log.warn("LOST 타입이 아닌 항목에 대한 매칭 시도: {}", lostItemId);
            return Collections.emptyList();
        }

        // 1. 후보 집합 생성
        List<LostItem> candidates = generateCandidateSet(lostItem);
        log.info("후보 집합 생성 완료: {}개", candidates.size());

        // 2. 점수화 및 정렬
        List<MatchCandidate> scoredCandidates = candidates.stream()
                .map(foundItem -> {
                    double score = MatchingScoreUtil.calculateFinalScore(lostItem, foundItem);
                    return new MatchCandidate(foundItem, score);
                })
                .filter(candidate -> candidate.getScore() >= MATCH_THRESHOLD)
                .sorted(Comparator
                        .comparing(MatchCandidate::getScore).reversed()
                        .thenComparing(c -> calculateDistanceForSorting(lostItem, c.getItem()))
                        .thenComparing(c -> calculateTimeDiffForSorting(lostItem, c.getItem()))
                        .thenComparing(c -> calculateTextScoreForSorting(lostItem, c.getItem())))
                .limit(MAX_CANDIDATES)
                .collect(Collectors.toList());

        log.info("매칭 완료: {}개 후보 (임계치: {})", scoredCandidates.size(), MATCH_THRESHOLD);
        return scoredCandidates;
    }

    /**
     * 후보 집합을 생성합니다.
     * 기본 조건: FOUND 타입, 카테고리 일치, 시간 조건 충족
     * 위치 조건: 반경 2KM 이내 (좌표가 있는 경우)
     * 
     * @param lostItem 분실물
     * @return 후보 리스트
     */
    private List<LostItem> generateCandidateSet(LostItem lostItem) {
        // 기본 조건: FOUND 타입, 카테고리 일치, 시간 조건
        List<LostItem> baseCandidates = lostItemRepository.findByItemTypeAndCategoryAndFoundDate(
                LostItem.ItemType.FOUND,
                lostItem.getCategory(),
                lostItem.getFoundDate()
        );

        // 위치 조건 필터링 (좌표가 있는 경우)
        if (lostItem.getLatitude() != null && lostItem.getLongitude() != null) {
            return baseCandidates.stream()
                    .filter(foundItem -> {
                        if (foundItem.getLatitude() == null || foundItem.getLongitude() == null) {
                            return true; // 좌표가 없으면 포함 (기본 점수로 처리)
                        }
                        double distance = MatchingScoreUtil.calculateDistance(
                                lostItem.getLatitude(), lostItem.getLongitude(),
                                foundItem.getLatitude(), foundItem.getLongitude()
                        );
                        return distance <= MAX_DISTANCE_KM;
                    })
                    .collect(Collectors.toList());
        }

        return baseCandidates;
    }

    /**
     * 정렬용 거리 계산
     */
    private double calculateDistanceForSorting(LostItem lostItem, LostItem foundItem) {
        if (lostItem.getLatitude() == null || foundItem.getLatitude() == null) {
            return Double.MAX_VALUE;
        }
        return MatchingScoreUtil.calculateDistance(
                lostItem.getLatitude(), lostItem.getLongitude(),
                foundItem.getLatitude(), foundItem.getLongitude()
        );
    }

    /**
     * 정렬용 시간 차이 계산
     */
    private long calculateTimeDiffForSorting(LostItem lostItem, LostItem foundItem) {
        if (lostItem.getFoundDate() == null || foundItem.getFoundDate() == null) {
            return Long.MAX_VALUE;
        }
        return Math.abs(java.time.temporal.ChronoUnit.DAYS.between(
                lostItem.getFoundDate(), foundItem.getFoundDate()
        ));
    }

    /**
     * 정렬용 텍스트 점수 계산
     */
    private double calculateTextScoreForSorting(LostItem lostItem, LostItem foundItem) {
        return MatchingScoreUtil.calculateTextScore(lostItem, foundItem);
    }

    /**
     * 매칭 후보를 나타내는 내부 클래스
     */
    public static class MatchCandidate {
        private final LostItem item;
        private final double score;

        public MatchCandidate(LostItem item, double score) {
            this.item = item;
            this.score = score;
        }

        public LostItem getItem() {
            return item;
        }

        public double getScore() {
            return score;
        }

        public LostItemResponse toResponse() {
            return LostItemResponse.from(item);
        }
    }
}


