package com.lostfound.capstonebackend.domain.lostitem;

import com.lostfound.capstonebackend.common.exception.BusinessException;
import com.lostfound.capstonebackend.common.exception.ErrorCode;
import com.lostfound.capstonebackend.common.util.RegionUtil;
import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemRequest;
import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemResponse;
import com.lostfound.capstonebackend.domain.user.User;
import com.lostfound.capstonebackend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 분실물 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 분실물 CRUD, 검색, 상태 변경 등의 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LostItemService {

    private final LostItemRepository lostItemRepository;
    private final UserRepository userRepository;

    /**
     * 모든 분실물 목록을 페이지네이션하여 조회합니다.
     * @param pageable 페이지네이션 정보 (페이지 번호, 사이즈 등)
     * @return 분실물 정보가 담긴 페이지 객체
     */
    public Page<LostItemResponse> findAll(Pageable pageable) {
        return lostItemRepository.findAll(pageable)
                .map(LostItemResponse::from);
    }

    /**
     * 주어진 ID에 해당하는 분실물 상세 정보를 조회합니다.
     * @param id 조회할 분실물의 ID
     * @return 조회된 분실물 상세 정보
     * @throws BusinessException 분실물을 찾을 수 없는 경우
     */
    public LostItemResponse findById(Long id) {
        LostItem item = lostItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "분실물을 찾을 수 없습니다. ID: " + id));
        return LostItemResponse.from(item);
    }

    /**
     * 새로운 분실물을 등록합니다.
     * @param request 등록할 분실물 정보 DTO
     * @param userEmail 분실물을 등록하는 사용자의 이메일
     * @return 등록된 분실물의 상세 정보
     * @throws BusinessException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public LostItemResponse create(LostItemRequest request, String userEmail) {
        // Attach the persisted User entity so the new lost item is linked to its author.
        User owner = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // title, location, storageLocation에서 지역 정보 추출
        String extractedRegion = RegionUtil.extractRegionFromAll(request.title(), request.location(), request.storageLocation());

        LostItem item = LostItem.builder()
                .title(request.title())
                .description(request.description())
                .category(request.category())
                .location(request.location())
                .region(extractedRegion)
                .foundDate(request.foundDate())
                .color(request.color())
                .storageLocation(request.storageLocation())
                .imagePath(request.imagePath())
                .owner(owner)
                .dataSource(LostItem.DataSource.USER)
                .status(LostItem.Status.FOUND)
                .build();

        LostItem savedItem = lostItemRepository.save(item);
        log.info("분실물 등록 완료 - ID: {}, 제목: {}, 지역: {}, 등록자: {}", 
                savedItem.getId(), savedItem.getTitle(), extractedRegion, userEmail);

        return LostItemResponse.from(savedItem);
    }

    /**
     * 기존 분실물 정보를 수정합니다.
     * LOST112에서 가져온 데이터는 수정할 수 없으며, 본인이 등록한 분실물만 수정 가능합니다.
     * @param id 수정할 분실물의 ID
     * @param request 수정할 분실물 정보 DTO
     * @param userEmail 수정을 시도하는 사용자의 이메일
     * @return 수정된 분실물의 상세 정보
     * @throws BusinessException 분실물을 찾을 수 없거나, 수정 권한이 없는 경우
     */
    @Transactional
    public LostItemResponse update(Long id, LostItemRequest request, String userEmail) {
        LostItem item = lostItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "분실물을 찾을 수 없습니다."));

        // 권한 확인 (본인 또는 LOST112 데이터는 수정 불가)
        if (item.getDataSource() == LostItem.DataSource.LOST112) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "LOST112 데이터는 수정할 수 없습니다.");
        }

        // Only the original author is allowed to manipulate a user-submitted lost item.
        if (item.getOwner() == null || !item.getOwner().getEmail().equals(userEmail)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인이 등록한 분실물만 수정할 수 있습니다.");
        }

        item.updateInfo(
                request.title(),
                request.description(),
                request.category(),
                request.location(),
                request.foundDate(),
                request.color()
        );
        
        // 수정 시 지역 정보도 다시 추출
        String extractedRegion = RegionUtil.extractRegionFromAll(item.getTitle(), request.location(), request.storageLocation());
        item.setRegion(extractedRegion);

        log.info("분실물 수정 완료 - ID: {}, 지역: {}, 수정자: {}", id, extractedRegion, userEmail);
        return LostItemResponse.from(item);
    }

    /**
     * 분실물 정보를 삭제합니다.
     * LOST112에서 가져온 데이터는 삭제할 수 없으며, 본인이 등록한 분실물만 삭제 가능합니다.
     * @param id 삭제할 분실물의 ID
     * @param userEmail 삭제를 시도하는 사용자의 이메일
     * @throws BusinessException 분실물을 찾을 수 없거나, 삭제 권한이 없는 경우
     */
    @Transactional
    public void delete(Long id, String userEmail) {
        LostItem item = lostItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "분실물을 찾을 수 없습니다."));

        // 권한 확인
        if (item.getDataSource() == LostItem.DataSource.LOST112) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "LOST112 데이터는 삭제할 수 없습니다.");
        }

        // Only the original author is allowed to manipulate a user-submitted lost item.
        if (item.getOwner() == null || !item.getOwner().getEmail().equals(userEmail)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인이 등록한 분실물만 삭제할 수 있습니다.");
        }

        lostItemRepository.delete(item);
        log.info("분실물 삭제 완료 - ID: {}, 삭제자: {}", id, userEmail);
    }

    /**
     * 현재 로그인한 사용자가 등록한 분실물 목록을 조회합니다.
     * @param userEmail 현재 사용자의 이메일
     * @param pageable 페이지네이션 정보
     * @return 해당 사용자가 등록한 분실물 목록 페이지
     * @throws BusinessException 사용자를 찾을 수 없는 경우
     */
    public Page<LostItemResponse> findMyItems(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        return lostItemRepository.findByOwnerId(user.getId(), pageable)
                .map(LostItemResponse::from);
    }

    /**
     * 키워드를 포함하는 분실물을 검색합니다.
     * 키워드가 비어있으면 전체 목록을 반환합니다.
     * @param keyword 검색할 키워드
     * @param pageable 페이지네이션 정보
     * @return 검색된 분실물 목록 페이지
     */
    public Page<LostItemResponse> searchByKeyword(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAll(pageable);
        }

        return lostItemRepository.findByKeyword(keyword.trim(), pageable)
                .map(LostItemResponse::from);
    }

    /**
     * 여러 조건을 조합하여 분실물을 복합 검색합니다.
     * @param keyword 검색 키워드 (선택 사항)
     * @param category 카테고리 (선택 사항)
     * @param status 분실물 상태 (선택 사항)
     * @param fromDate 검색 시작일 (선택 사항)
     * @param toDate 검색 종료일 (선택 사항)
     * @param pageable 페이지네이션 정보
     * @return 검색된 분실물 목록 페이지
     */
    public Page<LostItemResponse> searchComplex(String keyword, String category, String status,
                                              LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        LostItem.Status statusEnum = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = LostItem.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 상태값 무시: {}", status);
            }
        }

        return lostItemRepository.findByComplexSearch(
                keyword != null && !keyword.trim().isEmpty() ? keyword.trim() : null,
                category != null && !category.trim().isEmpty() ? category.trim() : null,
                statusEnum,
                fromDate,
                toDate,
                pageable
        ).map(LostItemResponse::from);
    }

    /**
     * 특정 카테고리에 해당하는 분실물 목록을 조회합니다.
     * @param category 조회할 카테고리
     * @param pageable 페이지네이션 정보
     * @return 해당 카테고리의 분실물 목록 페이지
     */
    public Page<LostItemResponse> findByCategory(String category, Pageable pageable) {
        return lostItemRepository.findByCategory(category, pageable)
                .map(LostItemResponse::from);
    }

    /**
     * 최근에 등록된 분실물 상위 10개를 조회합니다. (메인 페이지용)
     * @return 최근 등록된 분실물 10개 목록
     */
    public List<LostItemResponse> findRecentItems() {
        return lostItemRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(LostItemResponse::from)
                .toList();
    }

    /**
     * 분실물의 상태를 변경합니다. (예: FOUND -> COLLECTED)
     * 본인이 등록한 분실물만 상태를 변경할 수 있습니다.
     * @param id 상태를 변경할 분실물의 ID
     * @param status 변경할 새로운 상태 문자열
     * @param userEmail 상태 변경을 시도하는 사용자의 이메일
     * @return 상태가 변경된 분실물의 상세 정보
     * @throws BusinessException 분실물을 찾을 수 없거나, 권한이 없거나, 상태값이 잘못된 경우
     */
    @Transactional
    public LostItemResponse updateStatus(Long id, String status, String userEmail) {
        LostItem item = lostItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "분실물을 찾을 수 없습니다."));

        // 권한 확인 (본인 또는 관리자만)
        if (item.getOwner() != null && !item.getOwner().getEmail().equals(userEmail)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인이 등록한 분실물만 상태를 변경할 수 있습니다.");
        }

        try {
            // Convert the incoming status string to the enum and fail fast on invalid values.
            LostItem.Status newStatus = LostItem.Status.valueOf(status.toUpperCase());
            item.updateStatus(newStatus);
            log.info("분실물 상태 변경 완료 - ID: {}, 새 상태: {}, 변경자: {}", id, newStatus, userEmail);
            return LostItemResponse.from(item);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "잘못된 상태값입니다: " + status);
        }
    }

    // ========== 고도화된 검색 기능 추가 ==========

    /**
     * 지역(위치)별 분실물을 검색합니다.
     * 선택된 지역과 인접한 지역들을 모두 포함하여 검색합니다.
     * @param region 지역명
     * @param pageable 페이지네이션 정보
     * @return 해당 지역 및 인접 지역의 분실물 목록 페이지
     */
    public Page<LostItemResponse> searchByRegion(String region, Pageable pageable) {
        if (region == null || region.trim().isEmpty() || "전체".equals(region.trim())) {
            return findAll(pageable);
        }
        
        String trimmedRegion = region.trim();
        
        // 선택된 지역과 인접 지역 목록을 가져옵니다
        List<String> searchableRegions = RegionUtil.getSearchableRegions(trimmedRegion);
        
        if (searchableRegions.isEmpty()) {
            // 검색 가능한 지역이 없으면 전체 검색
            return findAll(pageable);
        }
        
        log.info("지역 검색 - 선택된 지역: {}, 검색 범위: {}", trimmedRegion, searchableRegions);
        
        // 여러 지역에서 검색
        return lostItemRepository.findByRegionIn(searchableRegions, pageable)
                .map(LostItemResponse::from);
    }

    /**
     * 색상별 분실물을 검색합니다.
     * @param color 색상 키워드 (대소문자 구분 없음)
     * @param pageable 페이지네이션 정보
     * @return 해당 색상의 분실물 목록 페이지
     */
    public Page<LostItemResponse> searchByColor(String color, Pageable pageable) {
        if (color == null || color.trim().isEmpty()) {
            return findAll(pageable);
        }
        
        return lostItemRepository.findByColorContainingIgnoreCase(color.trim(), pageable)
                .map(LostItemResponse::from);
    }

    /**
     * 전체 텍스트 검색을 수행합니다.
     * 제목, 설명, 카테고리, 색상, 위치 모든 필드에서 검색합니다.
     * @param searchText 검색할 텍스트
     * @param pageable 페이지네이션 정보
     * @return 검색된 분실물 목록 페이지
     */
    public Page<LostItemResponse> searchByFullText(String searchText, Pageable pageable) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return findAll(pageable);
        }
        
        return lostItemRepository.findByFullTextSearch(searchText.trim(), pageable)
                .map(LostItemResponse::from);
    }

    /**
     * 특정 상태의 최근 분실물을 조회합니다.
     * @param status 분실물 상태
     * @param limit 조회할 개수
     * @return 해당 상태의 최근 분실물 목록
     */
    public List<LostItemResponse> findRecentItemsByStatus(String status, int limit) {
        try {
            LostItem.Status statusEnum = LostItem.Status.valueOf(status.toUpperCase());
            Pageable pageable = PageRequest.of(0, limit);
            return lostItemRepository.findRecentByStatus(statusEnum, pageable)
                    .stream()
                    .map(LostItemResponse::from)
                    .toList();
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 상태값으로 최근 분실물 조회 시도: {}", status);
            return List.of();
        }
    }

    /**
     * 특정 기간 동안 등록된 분실물을 조회합니다. (생성일 기준)
     * @param startDate 시작일
     * @param endDate 종료일
     * @param pageable 페이지네이션 정보
     * @return 해당 기간의 분실물 목록 페이지
     */
    public Page<LostItemResponse> findByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        if (startDate == null && endDate == null) {
            return findAll(pageable);
        }
        
        // LocalDate를 LocalDateTime으로 변환 (시작일은 00:00:00, 종료일은 23:59:59)
        var startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        var endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;
        
        if (startDateTime == null) {
            startDateTime = java.time.LocalDateTime.MIN;
        }
        if (endDateTime == null) {
            endDateTime = java.time.LocalDateTime.MAX;
        }
        
        return lostItemRepository.findByCreatedAtBetween(startDateTime, endDateTime, pageable)
                .map(LostItemResponse::from);
    }

    /**
     * 현재 로그인한 사용자의 분실물 상태별 통계를 조회합니다.
     * @param userEmail 사용자 이메일
     * @return [상태, 개수] 형태의 통계 목록
     */
    public List<Object[]> getMyStatusStatistics(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다."));
        
        return lostItemRepository.getStatusStatisticsByOwner(user.getId());
    }

    // ========== 데이터 마이그레이션 및 유지보수 기능 ==========

    /**
     * 전체 분실물 개수를 반환합니다.
     */
    public long getTotalCount() {
        return lostItemRepository.count();
    }

    /**
     * region 필드가 있는 분실물 개수를 반환합니다.
     */
    public long getCountWithRegion() {
        return lostItemRepository.countWithRegion();
    }

    /**
     * region별 분포를 반환합니다.
     */
    public Map<String, Long> getRegionDistribution() {
        List<Object[]> results = lostItemRepository.getRegionDistribution();
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (Object[] result : results) {
            String region = (String) result[0];
            Long count = (Long) result[1];
            distribution.put(region != null ? region : "미분류", count);
        }
        return distribution;
    }

    /**
     * region이 없는 데이터 샘플을 조회합니다.
     */
    public List<LostItemResponse> getItemsWithoutRegion(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return lostItemRepository.findItemsWithoutRegion(pageable)
                .stream()
                .map(LostItemResponse::from)
                .toList();
    }

    /**
     * 기존 분실물 데이터의 region 필드를 업데이트합니다.
     * location이나 storageLocation에서 지역 정보를 추출하여 region 필드에 저장합니다.
     * @return 업데이트된 항목 수
     */
    @Transactional
    public int updateAllRegions() {
        log.info("전체 분실물 데이터의 지역 정보 추출 시작...");
        
        List<LostItem> allItems = lostItemRepository.findAll();
        int updatedCount = 0;
        int totalCount = allItems.size();
        
        for (LostItem item : allItems) {
            String extractedRegion = RegionUtil.extractRegionFromAll(
                    item.getTitle(),
                    item.getLocation(), 
                    item.getStorageLocation()
            );
            
            // 지역이 추출되었고 기존 값과 다른 경우에만 업데이트
            if (extractedRegion != null && !extractedRegion.equals(item.getRegion())) {
                item.setRegion(extractedRegion);
                updatedCount++;
                
                if (updatedCount % 100 == 0) {
                    log.info("진행 중... {}/{} 완료", updatedCount, totalCount);
                }
            }
        }
        
        log.info("지역 정보 추출 완료 - 전체: {}, 업데이트: {}", totalCount, updatedCount);
        return updatedCount;
    }

    /**
     * 특정 ID 범위의 분실물 데이터의 region 필드를 업데이트합니다.
     * @param startId 시작 ID
     * @param endId 종료 ID
     * @return 업데이트된 항목 수
     */
    @Transactional
    public int updateRegionsByIdRange(Long startId, Long endId) {
        log.info("ID 범위 {}-{} 분실물 데이터의 지역 정보 추출 시작...", startId, endId);
        
        List<LostItem> items = lostItemRepository.findAllById(
                java.util.stream.LongStream.rangeClosed(startId, endId)
                        .boxed()
                        .toList()
        );
        
        int updatedCount = 0;
        for (LostItem item : items) {
            String extractedRegion = RegionUtil.extractRegionFromAll(
                    item.getTitle(),
                    item.getLocation(), 
                    item.getStorageLocation()
            );
            
            if (extractedRegion != null) {
                item.setRegion(extractedRegion);
                updatedCount++;
            }
        }
        
        log.info("지역 정보 추출 완료 - 처리: {}, 업데이트: {}", items.size(), updatedCount);
        return updatedCount;
    }
}
