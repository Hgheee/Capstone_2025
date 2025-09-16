package com.lostfound.capstonebackend.domain.lostitem;

import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemRequest;
import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LostItemService {
    private final LostItemRepository repo;

    public List<LostItemResponse> findAll() {
        return repo.findAll().stream()
                .map(LostItemResponse::from)
                .toList();
    }

    public LostItemResponse create(LostItemRequest req) {
        LostItem item = LostItem.builder()
                .title(req.title())
                .description(req.description())
                .build();
        return LostItemResponse.from(repo.save(item));
    }
}
