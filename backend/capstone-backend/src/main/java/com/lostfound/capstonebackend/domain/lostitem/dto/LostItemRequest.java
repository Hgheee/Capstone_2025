package com.lostfound.capstonebackend.domain.lostitem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 분실물 생성 및 수정을 위한 요청 데이터를 담는 DTO(Record)입니다.
 * 각 필드에는 유효성 검증(Validation) 애노테이션이 적용되어 있습니다.
 *
 * @param title           분실물 제목 (필수, 최대 100자)
 * @param description     상세 설명 (최대 2000자)
 * @param category        카테고리 (최대 50자)
 * @param location        습득 장소 (최대 200자)
 * @param foundDate       습득일
 * @param color           색상 (최대 50자)
 * @param storageLocation 보관 장소 (최대 200자)
 * @param imagePath       이미지 파일 경로
 */
public record LostItemRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 최대 100자입니다.")
        String title,

        @Size(max = 2000, message = "설명은 최대 2000자입니다.")
        String description,

        @Size(max = 50, message = "카테고리는 최대 50자입니다.")
        String category,

        @Size(max = 200, message = "위치는 최대 200자입니다.")
        String location,

        LocalDate foundDate,

        @Size(max = 50, message = "색상은 최대 50자입니다.")
        String color,

        @Size(max = 200, message = "보관장소는 최대 200자입니다.")
        String storageLocation,

        String imagePath
) {}