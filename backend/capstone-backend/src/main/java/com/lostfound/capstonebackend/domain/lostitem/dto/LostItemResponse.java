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
 * @param foundDate       습득일
 * @param status          현재 상태 (FOUND, CLAIMED, EXPIRED)
 * @param dataSource      데이터 출처 (USER, LOST112)
 * @param color           색상
 * @param storageLocation 보관 장소
 * @param imagePath       이미지 경로
 * @param ownerName       등록한 사용자의 이름 (외부 데이터의 경우 null)
 * @param createdAt       생성 일시
 * @param updatedAt       수정 일시
 */
public record LostItemResponse(
        Long id,
        String title,
        String description,
        String category,
        String location,
        LocalDate foundDate,
        String status,
        String dataSource,
        String color,
        String storageLocation,
        String imagePath,
        String ownerName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
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
                item.getFoundDate(),
                item.getStatus() != null ? item.getStatus().name() : null,
                item.getDataSource() != null ? item.getDataSource().name() : null,
                item.getColor(),
                item.getStorageLocation(),
                item.getImagePath(),
                item.getOwner() != null ? item.getOwner().getName() : null,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
