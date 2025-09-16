package com.lostfound.capstonebackend.domain.lostitem;

import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemRequest;
import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lost-items")
@RequiredArgsConstructor
public class LostItemController {
    private final LostItemService service;

    @GetMapping
    public List<LostItemResponse> list() {
        return service.findAll();
    }

    @PostMapping
    public LostItemResponse create(@RequestBody LostItemRequest req) {
        return service.create(req);
    }
}
