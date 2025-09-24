package com.lostfound.capstonebackend.domain.lostitem;

import com.lostfound.capstonebackend.common.exception.BusinessException;
import com.lostfound.capstonebackend.common.exception.ErrorCode;
import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemRequest;
import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemResponse;
import com.lostfound.capstonebackend.domain.user.User;
import com.lostfound.capstonebackend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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

        LostItem item = LostItem.builder()
                .title(request.title())
                .description(request.description())
                .category(request.category())
                .location(request.location())
                .foundDate(request.foundDate())
                .color(request.color())
                .storageLocation(request.storageLocation())
                .imagePath(request.imagePath())
                .owner(owner)
                .dataSource(LostItem.DataSource.USER)
                .status(LostItem.Status.FOUND)
                .build();

        LostItem savedItem = lostItemRepository.save(item);
        log.info("분실물 등록 완료 - ID: {}, 제목: {}, 등록자: {}", savedItem.getId(), savedItem.getTitle(), userEmail);

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

        log.info("분실물 수정 완료 - ID: {}, 수정자: {}", id, userEmail);
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
}
