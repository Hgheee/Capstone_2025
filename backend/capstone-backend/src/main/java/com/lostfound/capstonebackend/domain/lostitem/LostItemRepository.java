package com.lostfound.capstonebackend.domain.lostitem;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 분실물(LostItem) 엔티티에 대한 데이터베이스 접근을 처리하는 리포지토리입니다.
 * JpaRepository를 상속받아 기본적인 CRUD 기능을 제공하며, 추가적인 커스텀 쿼리를 정의합니다.
 */
public interface LostItemRepository extends JpaRepository<LostItem, Long> {

    /**
     * 외부 시스템(LOST112)의 ID를 사용하여 분실물을 조회합니다.
     * 데이터 동기화 시 중복 등록을 방지하는 데 사용됩니다.
     * @param externalId 조회할 외부 ID
     * @return 해당 외부 ID를 가진 분실물 Optional 객체
     */
    Optional<LostItem> findByExternalId(String externalId);

    /**
     * 데이터 출처(사용자 등록 또는 LOST112)에 따라 분실물 목록을 조회합니다.
     * @param dataSource 데이터 출처 Enum 값
     * @param pageable 페이지네이션 정보
     * @return 해당 출처의 분실물 목록 페이지
     */
    Page<LostItem> findByDataSource(LostItem.DataSource dataSource, Pageable pageable);

    /**
     * 분실물의 현재 상태(FOUND, COLLECTED 등)에 따라 목록을 조회합니다.
     * @param status 분실물 상태 Enum 값
     * @param pageable 페이지네이션 정보
     * @return 해당 상태의 분실물 목록 페이지
     */
    Page<LostItem> findByStatus(LostItem.Status status, Pageable pageable);

    /**
     * 특정 사용자가 등록한 분실물 목록을 조회합니다.
     * @param ownerId 소유자(사용자)의 ID
     * @param pageable 페이지네이션 정보
     * @return 해당 사용자가 등록한 분실물 목록 페이지
     */
    Page<LostItem> findByOwnerId(Long ownerId, Pageable pageable);

    /**
     * 키워드를 사용하여 분실물의 제목 또는 설명에서 일치하는 항목을 검색합니다.
     * @param keyword 검색할 키워드
     * @param pageable 페이지네이션 정보
     * @return 키워드가 포함된 분실물 목록 페이지
     */
    @Query("SELECT l FROM LostItem l WHERE l.title LIKE %:keyword% OR l.description LIKE %:keyword%")
    Page<LostItem> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 특정 카테고리에 해당하는 분실물 목록을 조회합니다.
     * @param category 조회할 카테고리명
     * @param pageable 페이지네이션 정보
     * @return 해당 카테고리의 분실물 목록 페이지
     */
    Page<LostItem> findByCategory(String category, Pageable pageable);

    /**
     * 습득 장소 또는 보관 장소에 키워드가 포함된 분실물 목록을 검색합니다.
     * @param location 검색할 위치 키워드
     * @param pageable 페이지네이션 정보
     * @return 해당 위치 정보가 포함된 분실물 목록 페이지
     */
    @Query("SELECT l FROM LostItem l WHERE l.location LIKE %:location% OR l.storageLocation LIKE %:location%")
    Page<LostItem> findByLocation(@Param("location") String location, Pageable pageable);

    /**
     * 특정 습득 날짜 범위 내에 있는 분실물 목록을 조회합니다.
     * @param fromDate 검색 시작일
     * @param toDate 검색 종료일
     * @param pageable 페이지네이션 정보
     * @return 해당 기간 내에 습득된 분실물 목록 페이지
     */
    Page<LostItem> findByFoundDateBetween(LocalDate fromDate, LocalDate toDate, Pageable pageable);

    /**
     * 여러 조건을 조합하여 분실물을 동적으로 검색합니다.
     * 모든 파라미터는 선택 사항이며, null이 아닌 파라미터만 검색 조건에 포함됩니다.
     * @param keyword 제목 또는 설명에 포함될 검색 키워드
     * @param category 필터링할 카테고리
     * @param status 필터링할 분실물 상태
     * @param fromDate 검색 시작일 (습득일 기준)
     * @param toDate 검색 종료일 (습득일 기준)
     * @param pageable 페이지네이션 정보
     * @return 검색 조건에 맞는 분실물 목록 페이지
     */
    @Query("SELECT l FROM LostItem l WHERE " +
           "(:keyword IS NULL OR l.title LIKE %:keyword% OR l.description LIKE %:keyword%) AND " +
           "(:category IS NULL OR l.category = :category) AND " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:fromDate IS NULL OR l.foundDate >= :fromDate) AND " +
           "(:toDate IS NULL OR l.foundDate <= :toDate)")
    Page<LostItem> findByComplexSearch(@Param("keyword") String keyword,
                                       @Param("category") String category,
                                       @Param("status") LostItem.Status status,
                                       @Param("fromDate") LocalDate fromDate,
                                       @Param("toDate") LocalDate toDate,
                                       Pageable pageable);

    /**
     * 최근에 등록된 분실물 상위 10개를 조회합니다. (메인 페이지용)
     * @return 최근 등록된 분실물 10개 목록
     */
    List<LostItem> findTop10ByOrderByCreatedAtDesc();

    /**
     * 통계용 쿼리로, 각 카테고리별 분실물의 개수를 계산합니다.
     * @return 각 row가 [카테고리명, 개수] 형태인 Object 배열의 리스트
     */
    @Query("SELECT l.category, COUNT(l) FROM LostItem l WHERE l.category IS NOT NULL GROUP BY l.category ORDER BY COUNT(l) DESC")
    List<Object[]> countByCategory();

    /**
     * 통계용 쿼리로, 각 상태별 분실물의 개수를 계산합니다.
     * @return 각 row가 [상태 Enum, 개수] 형태인 Object 배열의 리스트
     */
    @Query("SELECT l.status, COUNT(l) FROM LostItem l GROUP BY l.status")
    List<Object[]> countByStatus();

    /**
     * 통계용 쿼리로, 특정 기간 동안 등록된 분실물의 총 개수를 계산합니다.
     * @param startDate 조회 시작 일시
     * @param endDate 조회 종료 일시
     * @return 해당 기간에 등록된 분실물 총 개수
     */
    @Query("SELECT COUNT(l) FROM LostItem l WHERE l.createdAt BETWEEN :startDate AND :endDate")
    Long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ========== N+1 쿼리 해결을 위한 fetch join 쿼리들 ==========

    /**
     * ID로 분실물을 조회할 때 연관된 소유자(owner) 정보를 함께 로드(fetch join)합니다.
     * N+1 쿼리 문제를 방지하여 성능을 최적화합니다.
     * @param id 조회할 분실물의 ID
     * @return 소유자 정보가 포함된 분실물 Optional 객체
     */
    @Query("SELECT l FROM LostItem l LEFT JOIN FETCH l.owner WHERE l.id = :id")
    Optional<LostItem> findByIdWithOwner(@Param("id") Long id);

    /**
     * 특정 사용자가 등록한 분실물 목록 조회 시, 소유자 정보를 함께 로드(fetch join)합니다.
     * N+1 쿼리 문제를 방지합니다.
     * @param ownerId 소유자(사용자)의 ID
     * @param pageable 페이지네이션 정보
     * @return 소유자 정보가 포함된 분실물 목록 페이지
     */
    @Query("SELECT l FROM LostItem l LEFT JOIN FETCH l.owner WHERE l.owner.id = :ownerId")
    Page<LostItem> findByOwnerIdWithOwner(@Param("ownerId") Long ownerId, Pageable pageable);

    /**
     * 최근 등록된 분실물 목록 조회 시, 소유자 정보를 함께 로드(fetch join)합니다.
     * N+1 쿼리 문제를 방지합니다.
     * @return 소유자 정보가 포함된 최근 분실물 10개 목록
     */
    @Query("SELECT l FROM LostItem l LEFT JOIN FETCH l.owner ORDER BY l.createdAt DESC")
    List<LostItem> findTop10ByOrderByCreatedAtDescWithOwner();
}
