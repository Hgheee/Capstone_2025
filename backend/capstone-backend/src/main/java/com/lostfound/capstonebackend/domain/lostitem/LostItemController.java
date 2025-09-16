package com.lostfound.capstonebackend.domain.lostitem;

import com.lostfound.capstonebackend.common.dto.ApiResponse;
import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemRequest;
import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * LostItem REST API
 * - GET  /api/lost-items : 목록 조회
 * - POST /api/lost-items : 생성(요청 바디 검증)
 */
@RestController
@RequestMapping("/api/lost-items")
@RequiredArgsConstructor
public class LostItemController {
    private final LostItemService service;

    @GetMapping
    public ApiResponse<List<LostItemResponse>> list() {
        return ApiResponse.ok(service.findAll());
    }

    @PostMapping
    public ApiResponse<LostItemResponse> create(@RequestBody @Valid LostItemRequest req) {
        return ApiResponse.ok(service.create(req));
    }
}
