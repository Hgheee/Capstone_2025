package com.lostfound.capstonebackend.lostitem;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lost-items")
@RequiredArgsConstructor
public class LostItemController {

    private final LostItemRepository repo;

    @GetMapping
    public List<LostItem> list() {
        return repo.findAll();
    }

    @PostMapping
    public LostItem add(@RequestBody LostItem item) {
        return repo.save(item);
    }
}
