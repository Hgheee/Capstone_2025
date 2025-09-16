package com.lostfound.capstonebackend.domain.lostitem.dto;

import com.lostfound.capstonebackend.domain.lostitem.LostItem;

public record LostItemResponse(Long id, String title, String description) {
    public static LostItemResponse from(LostItem e) {
        return new LostItemResponse(e.getId(), e.getTitle(), e.getDescription());
    }
}
