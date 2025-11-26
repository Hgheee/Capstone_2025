package com.lostfound.capstonebackend.domain.lostitem.dto;

import com.lostfound.capstonebackend.domain.lostitem.LostItem;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 분실물 정보를 클라이언트에 전달하기 위한 응답 DTO(Record)입니다.
 *
 * @param id              분실물 ID
 * @param title           제목
 * @param description     상세 설명
 * @param category        카테고리
 * @param location        습득 장소
 * @param region          지역 (서울시 구)
 * @param foundDate       습득일
 * @param status          현재 상태 (FOUND, CLAIMED, EXPIRED 등)
 * @param externalId      외부 시스템 고유 ID
 * @param dataSource      데이터 출처 (USER, LOST112, SEOUL_LOST 등)
 * @param color           색상
 * @param storageLocation 보관 장소
 * @param imagePath       이미지 경로
 * @param ownerName       등록한 사용자의 이름 (외부 데이터의 경우 null)
 * @param viewCount       외부 데이터 조회수
 * @param receivedDate    외부 시스템 기준 수령 일시
 * @param createdAt       생성 일시
 * @param updatedAt       수정 일시
 * @param itemType        분실물 타입 (LOST: 분실물, FOUND: 습득물)
 * @param latitude        위도 (매칭 알고리즘용)
 * @param longitude       경도 (매칭 알고리즘용)
 */
public record LostItemResponse(
        Long id,
        String title,
        String description,
        String category,
        String location,
        String region,
        LocalDate foundDate,
        String status,
        String externalId,
        String dataSource,
        String color,
        String storageLocation,
        String imagePath,
        String ownerName,
        Integer viewCount,
        LocalDateTime receivedDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String itemType,
        Double latitude,
        Double longitude
) {
    /**
     * LostItem 엔티티 객체를 LostItemResponse DTO로 변환하는 정적 팩토리 메소드입니다.
     * @param item 변환할 LostItem 엔티티
     * @return 변환된 LostItemResponse DTO
     */
    public static LostItemResponse from(LostItem item) {
        return new LostItemResponse(
                item.getId(),
                item.getTitle(),
                item.getDescription(),
                item.getCategory(),
                item.getLocation(),
                item.getRegion(),
                item.getFoundDate(),
                item.getStatus() != null ? item.getStatus().name() : null,
                item.getExternalId(),
                item.getDataSource() != null ? item.getDataSource().name() : null,
                item.getColor(),
                item.getStorageLocation(),
                item.getImagePath(),
                item.getOwner() != null ? item.getOwner().getName() : null,
                item.getViewCount(),
                item.getReceivedDate(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                item.getItemType() != null ? item.getItemType().name() : "FOUND", // 기본값 FOUND
                item.getLatitude(),
                item.getLongitude()
        );
    }
}
