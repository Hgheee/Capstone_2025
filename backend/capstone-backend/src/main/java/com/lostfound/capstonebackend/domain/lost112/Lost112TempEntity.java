package com.lostfound.capstonebackend.domain.lost112;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * LOST112 외부 데이터를 임시로 저장하기 위한 엔티티입니다.
 * Python 스크립트 등을 통해 수집된 원본 데이터가 이 테이블에 먼저 저장된 후,
 * 가공 및 중복 검사를 거쳐 메인 분실물 테이블({@link com.lostfound.capstonebackend.domain.lostitem.LostItem})에 동기화됩니다.
 */
@Entity
@Table(name = "lost_items_temp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lost112TempEntity {

    /**
     * LOST112 시스템의 원본 데이터 ID (PK)
     */
    @Id
    @Column(name = "item_id", nullable = false, length = 255)
    private String itemId;

    /**
     * 분실물 명칭
     */
    @Column(name = "title", length = 255)
    private String title;

    /**
     * 습득일
     */
    @Column(name = "found_date")
    private LocalDate foundDate;

    /**
     * 보관 장소
     */
    @Column(name = "storage_place", length = 255)
    private String storagePlace;

    /**
     * 분실물 이미지 URL
     */
    @Column(name = "image_url", length = 512)
    private String imageUrl;

    /**
     * 분실물 색상
     */
    @Column(name = "color", length = 100)
    private String color;

    /**
     * 분실물 상세 설명
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 시스템 내부에서 매핑된 카테고리
     */
    @Column(name = "category", length = 100)
    private String category;

    /**
     * 매핑된 서브 카테고리 (현재 사용 안 함)
     */
    @Column(name = "subcategory", length = 100)
    private String subcategory;

    /**
     * LOST112에서 제공하는 원본 카테고리 문자열
     */
    @Column(name = "category_raw", length = 255)
    private String categoryRaw;

    /**
     * 레코드가 생성된 일시
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 레코드가 마지막으로 수정된 일시
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 엔티티가 처음 저장되기 전에 호출되어 생성 및 수정 시간을 초기화합니다.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * 엔티티가 업데이트되기 전에 호출되어 수정 시간을 갱신합니다.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
