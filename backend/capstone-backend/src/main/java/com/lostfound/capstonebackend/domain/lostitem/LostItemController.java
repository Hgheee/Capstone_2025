package com.lostfound.capstonebackend.domain.lostitem;

import com.lostfound.capstonebackend.common.dto.ApiResponse;
import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemRequest;
import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public ApiResponse<Page<LostItemResponse>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "id,asc") String sort
    ) {
        Sort sortSpec = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        return ApiResponse.ok(service.findAll(pageable));
    }

    private Sort parseSort(String sortParam) {
        // 다중 정렬 지원: 세미콜론 구분("id,desc;title,asc")
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Order.asc("id"));
        }
        String[] parts = sortParam.split(";");
        java.util.List<Sort.Order> orders = new java.util.ArrayList<>();
        for (String part : parts) {
            String[] tokens = part.split(",");
            String property = tokens[0].trim();
            String direction = tokens.length > 1 ? tokens[1].trim().toLowerCase() : "asc";
            if (property.isEmpty()) continue;
            if ("desc".equals(direction)) {
                orders.add(Sort.Order.desc(property));
            } else {
                orders.add(Sort.Order.asc(property));
            }
        }
        if (orders.isEmpty()) {
            orders.add(Sort.Order.asc("id"));
        }
        return Sort.by(orders);
    }

    @PostMapping
    public ApiResponse<LostItemResponse> create(@RequestBody @Valid LostItemRequest req) {
        return ApiResponse.ok(service.create(req));
    }
}
