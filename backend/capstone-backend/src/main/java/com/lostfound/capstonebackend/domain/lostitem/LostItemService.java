package com.lostfound.capstonebackend.domain.lostitem;

import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemRequest;
import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LostItemService {
    private final LostItemRepository repo;

    public Page<LostItemResponse> findAll(Pageable pageable) {
        return repo.findAll(pageable)
                .map(LostItemResponse::from);
    }

    public LostItemResponse create(LostItemRequest req) {
        LostItem item = LostItem.builder()
                .title(req.title())
                .description(req.description())
                .build();
        return LostItemResponse.from(repo.save(item));
    }
}
