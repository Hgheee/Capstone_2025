package com.lostfound.capstonebackend.domain.lost112;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;

/**
 * 임시 분실물 데이터({@link Lost112TempEntity})에 대한 데이터베이스 작업을 처리하는 리포지토리입니다.
 */
public interface Lost112TempRepository extends JpaRepository<Lost112TempEntity, String> {

    /**
     * 임시 테이블에 저장된 모든 항목의 총 개수를 반환합니다.
     * @return 임시 테이블의 총 레코드 수
     */
    @Query("SELECT COUNT(t) FROM Lost112TempEntity t")
    long countTotalItems();

    /**
     * 임시 테이블의 모든 항목을 생성일(createdAt) 기준 내림차순으로 정렬하여 페이지네이션된 형태로 반환합니다.
     * @param pageable 페이지네이션 정보
     * @return 정렬 및 페이지네이션이 적용된 임시 분실물 데이터 페이지
     */
    @Query("SELECT t FROM Lost112TempEntity t ORDER BY t.createdAt DESC")
    Page<Lost112TempEntity> findAllOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 지정된 날짜 이후에 습득된 임시 분실물 데이터를 조회합니다.
     * @param foundDate 검색 기준 날짜
     * @param pageable 페이지네이션 정보
     * @return 해당 조건에 맞는 임시 분실물 데이터 페이지
     */
    Page<Lost112TempEntity> findByFoundDateAfter(LocalDate foundDate, Pageable pageable);
}
