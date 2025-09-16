package com.lostfound.capstonebackend.domain.lostitem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LostItemRequest(
        @NotBlank(message = "title은 필수입니다.")
        @Size(max = 100, message = "title은 최대 100자입니다.")
        String title,

        @Size(max = 2000, message = "description은 최대 2000자입니다.")
        String description
) {}