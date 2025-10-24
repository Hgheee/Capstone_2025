package com.lostfound.capstonebackend.domain.lostitem;

import com.lostfound.capstonebackend.common.dto.ApiResponse;
import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemRequest;
import com.lostfound.capstonebackend.domain.lostitem.dto.LostItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 분실물(LostItem)에 대한 CRUD 및 검색 기능을 제공하는 REST 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/lost-items")
@RequiredArgsConstructor
@Tag(name = "분실물 관리", description = "분실물 등록, 조회, 수정, 삭제 및 검색 API")
public class LostItemController {

    private final LostItemService lostItemService;

    /**
     * 모든 분실물 목록을 페이지네이션하여 조회합니다.
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지당 항목 수
     * @param sort 정렬 조건 (예: "createdAt,desc")
     * @return 분실물 목록 페이지가 포함된 ApiResponse
     */
    @GetMapping
    @Operation(summary = "분실물 목록 조회", description = "페이징과 정렬을 지원하는 분실물 목록 조회")
    public ApiResponse<Page<LostItemResponse>> getAllItems(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size,

            @Parameter(description = "정렬 조건 (property,direction)", example = "createdAt,desc")
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort
    ) {
        Sort sortSpec = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        return ApiResponse.ok(lostItemService.findAll(pageable));
    }

    /**
     * ID를 사용하여 특정 분실물의 상세 정보를 조회합니다.
     * @param id 조회할 분실물의 ID
     * @return 분실물 상세 정보가 포함된 ApiResponse
     */
    @GetMapping("/{id}")
    @Operation(summary = "분실물 상세 조회", description = "분실물 ID로 상세 정보 조회")
    public ApiResponse<LostItemResponse> getItemById(
            @Parameter(description = "분실물 ID") @PathVariable Long id
    ) {
        return ApiResponse.ok(lostItemService.findById(id));
    }

    /**
     * 새로운 분실물 정보를 등록합니다. 인증된 사용자만 호출할 수 있습니다.
     * @param request 등록할 분실물 정보를 담은 DTO
     * @param userDetails 현재 인증된 사용자의 정보
     * @return 생성된 분실물 정보가 포함된 ApiResponse
     */
    @PostMapping
    @Operation(summary = "분실물 등록", description = "새로운 분실물 정보 등록 (인증 필요)")
    public ApiResponse<LostItemResponse> createItem(
            @RequestBody @Valid LostItemRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ApiResponse.ok(lostItemService.create(request, userDetails.getUsername()));
    }

    /**
     * 기존 분실물 정보를 수정합니다. 본인이 등록한 분실물만 수정할 수 있습니다.
     * @param id 수정할 분실물의 ID
     * @param request 수정할 분실물 정보를 담은 DTO
     * @param userDetails 현재 인증된 사용자의 정보
     * @return 수정된 분실물 정보가 포함된 ApiResponse
     */
    @PutMapping("/{id}")
    @Operation(summary = "분실물 수정", description = "분실물 정보 수정 (본인 등록 분실물만 가능)")
    public ApiResponse<LostItemResponse> updateItem(
            @Parameter(description = "분실물 ID") @PathVariable Long id,
            @RequestBody @Valid LostItemRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ApiResponse.ok(lostItemService.update(id, request, userDetails.getUsername()));
    }

    /**
     * 분실물 정보를 삭제합니다. 본인이 등록한 분실물만 삭제할 수 있습니다.
     * @param id 삭제할 분실물의 ID
     * @param userDetails 현재 인증된 사용자의 정보
     * @return 데이터가 없는 성공 ApiResponse
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "분실물 삭제", description = "분실물 삭제 (본인 등록 분실물만 가능)")
    public ApiResponse<Void> deleteItem(
            @Parameter(description = "분실물 ID") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        lostItemService.delete(id, userDetails.getUsername());
        return ApiResponse.ok();
    }

    /**
     * 현재 인증된 사용자가 등록한 분실물 목록을 조회합니다.
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param sort 정렬 조건
     * @param userDetails 현재 인증된 사용자의 정보
     * @return 해당 사용자의 분실물 목록 페이지가 포함된 ApiResponse
     */
    @GetMapping("/my")
    @Operation(summary = "내 분실물 목록", description = "현재 사용자가 등록한 분실물 목록 조회")
    public ApiResponse<Page<LostItemResponse>> getMyItems(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Sort sortSpec = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        return ApiResponse.ok(lostItemService.findMyItems(userDetails.getUsername(), pageable));
    }

    /**
     * 키워드를 사용하여 분실물을 검색합니다. (제목 및 설명 대상)
     * @param keyword 검색할 키워드
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param sort 정렬 조건
     * @return 검색된 분실물 목록 페이지가 포함된 ApiResponse
     */
    @GetMapping("/search")
    @Operation(summary = "분실물 키워드 검색", description = "제목과 설명에서 키워드로 검색")
    public ApiResponse<Page<LostItemResponse>> searchItems(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort
    ) {
        Sort sortSpec = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        return ApiResponse.ok(lostItemService.searchByKeyword(keyword, pageable));
    }

    /**
     * 여러 조건을 조합하여 분실물을 복합 검색합니다.
     * 모든 검색 파라미터는 선택 사항입니다.
     * @param keyword    검색 키워드
     * @param category   카테고리
     * @param status     분실물 상태
     * @param fromDate   검색 시작일
     * @param toDate     검색 종료일
     * @param page       페이지 번호
     * @param size       페이지 크기
     * @param sort       정렬 조건
     * @return 검색 조건에 맞는 분실물 목록 페이지가 포함된 ApiResponse
     */
    @GetMapping("/search/advanced")
    @Operation(summary = "고급 검색", description = "키워드, 카테고리, 상태, 기간을 조합한 복합 검색")
    public ApiResponse<Page<LostItemResponse>> advancedSearch(
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String keyword,
            @Parameter(description = "카테고리") @RequestParam(required = false) String category,
            @Parameter(description = "상태 (FOUND, CLAIMED, EXPIRED)") @RequestParam(required = false) String status,
            @Parameter(description = "검색 시작일 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "검색 종료일 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort
    ) {
        Sort sortSpec = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        return ApiResponse.ok(lostItemService.searchComplex(keyword, category, status, fromDate, toDate, pageable));
    }

    /**
     * 특정 카테고리에 해당하는 분실물 목록을 조회합니다.
     * @param category   카테고리명
     * @param page       페이지 번호
     * @param size       페이지 크기
     * @param sort       정렬 조건
     * @return 해당 카테고리의 분실물 목록 페이지가 포함된 ApiResponse
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "카테고리별 검색", description = "특정 카테고리의 분실물 목록 조회")
    public ApiResponse<Page<LostItemResponse>> getItemsByCategory(
            @Parameter(description = "카테고리명") @PathVariable String category,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort
    ) {
        Sort sortSpec = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        return ApiResponse.ok(lostItemService.findByCategory(category, pageable));
    }

    /**
     * 최근에 등록된 분실물 상위 10개를 조회합니다. (메인 페이지용)
     * @return 최근 등록된 분실물 10개 목록이 포함된 ApiResponse
     */
    @GetMapping("/recent")
    @Operation(summary = "최근 등록 분실물", description = "최근에 등록된 분실물 10건 조회 (메인 페이지용)")
    public ApiResponse<List<LostItemResponse>> getRecentItems() {
        return ApiResponse.ok(lostItemService.findRecentItems());
    }

    /**
     * 특정 분실물의 상태를 변경합니다.
     * @param id          상태를 변경할 분실물의 ID
     * @param status      새로운 상태 문자열 (FOUND, CLAIMED, EXPIRED)
     * @param userDetails 현재 인증된 사용자의 정보
     * @return 상태가 변경된 분실물 정보가 포함된 ApiResponse
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "분실물 상태 변경", description = "분실물의 상태를 변경 (FOUND, CLAIMED, EXPIRED)")
    public ApiResponse<LostItemResponse> updateItemStatus(
            @Parameter(description = "분실물 ID") @PathVariable Long id,
            @Parameter(description = "새로운 상태") @RequestParam String status,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ApiResponse.ok(lostItemService.updateStatus(id, status, userDetails.getUsername()));
    }

    // ========== 고도화된 검색 기능 추가 ==========

    /**
     * 지역(위치)별 분실물을 검색합니다.
     * 습득 장소와 보관 장소 모두에서 검색됩니다.
     * @param region 지역명 키워드
     * @param page   페이지 번호
     * @param size   페이지 크기
     * @param sort   정렬 조건
     * @return 해당 지역의 분실물 목록 페이지가 포함된 ApiResponse
     */
    @GetMapping("/search/region")
    @Operation(summary = "지역별 분실물 검색", description = "습득 장소 또는 보관 장소에서 지역명으로 검색")
    public ApiResponse<Page<LostItemResponse>> searchByRegion(
            @Parameter(description = "지역명 키워드") @RequestParam String region,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort
    ) {
        Sort sortSpec = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        return ApiResponse.ok(lostItemService.searchByRegion(region, pageable));
    }

    /**
     * 색상별 분실물을 검색합니다.
     * @param color 색상 키워드 (대소문자 구분 없음)
     * @param page  페이지 번호
     * @param size  페이지 크기
     * @param sort  정렬 조건
     * @return 해당 색상의 분실물 목록 페이지가 포함된 ApiResponse
     */
    @GetMapping("/search/color")
    @Operation(summary = "색상별 분실물 검색", description = "분실물의 색상 정보로 검색")
    public ApiResponse<Page<LostItemResponse>> searchByColor(
            @Parameter(description = "색상 키워드") @RequestParam String color,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort
    ) {
        Sort sortSpec = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        return ApiResponse.ok(lostItemService.searchByColor(color, pageable));
    }

    /**
     * 전체 텍스트 검색을 수행합니다.
     * 제목, 설명, 카테고리, 색상, 위치 모든 필드에서 검색합니다.
     * @param text 검색할 텍스트
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param sort 정렬 조건
     * @return 검색된 분실물 목록 페이지가 포함된 ApiResponse
     */
    @GetMapping("/search/fulltext")
    @Operation(summary = "전체 텍스트 검색", description = "모든 필드(제목, 설명, 카테고리, 색상, 위치)에서 포괄적 검색")
    public ApiResponse<Page<LostItemResponse>> searchByFullText(
            @Parameter(description = "검색할 텍스트") @RequestParam String text,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort
    ) {
        Sort sortSpec = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        return ApiResponse.ok(lostItemService.searchByFullText(text, pageable));
    }

    /**
     * 특정 상태의 최근 분실물을 조회합니다.
     * @param status 분실물 상태 (FOUND, CLAIMED, EXPIRED)
     * @param limit  조회할 개수 (기본값: 10)
     * @return 해당 상태의 최근 분실물 목록이 포함된 ApiResponse
     */
    @GetMapping("/recent/status/{status}")
    @Operation(summary = "상태별 최근 분실물", description = "특정 상태의 최근 등록된 분실물 조회")
    public ApiResponse<List<LostItemResponse>> getRecentItemsByStatus(
            @Parameter(description = "분실물 상태") @PathVariable String status,
            @Parameter(description = "조회할 개수") @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.ok(lostItemService.findRecentItemsByStatus(status, limit));
    }

    /**
     * 특정 기간 동안 등록된 분실물을 조회합니다. (생성일 기준)
     * @param startDate 시작일 (yyyy-MM-dd)
     * @param endDate   종료일 (yyyy-MM-dd)
     * @param page      페이지 번호
     * @param size      페이지 크기
     * @param sort      정렬 조건
     * @return 해당 기간의 분실물 목록 페이지가 포함된 ApiResponse
     */
    @GetMapping("/search/daterange")
    @Operation(summary = "기간별 분실물 검색", description = "특정 기간 동안 등록된 분실물 조회 (생성일 기준)")
    public ApiResponse<Page<LostItemResponse>> searchByDateRange(
            @Parameter(description = "시작일 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료일 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort
    ) {
        Sort sortSpec = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        return ApiResponse.ok(lostItemService.findByDateRange(startDate, endDate, pageable));
    }

    /**
     * 현재 로그인한 사용자의 분실물 상태별 통계를 조회합니다.
     * @param userDetails 현재 인증된 사용자의 정보
     * @return [상태, 개수] 형태의 통계 목록이 포함된 ApiResponse
     */
    @GetMapping("/my/statistics")
    @Operation(summary = "내 분실물 통계", description = "현재 사용자의 분실물 상태별 통계 조회")
    public ApiResponse<List<Object[]>> getMyStatusStatistics(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ApiResponse.ok(lostItemService.getMyStatusStatistics(userDetails.getUsername()));
    }

    /**
     * "property,direction;property2,direction2" 형식의 정렬 파라미터 문자열을 파싱하여
     * Spring Data의 {@link Sort} 객체로 변환합니다.
     * @param sortParam 파싱할 정렬 파라미터 문자열
     * @return 생성된 Sort 객체
     */
    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Order.desc("createdAt"));
        }

        String[] parts = sortParam.split(";");
        List<Sort.Order> orders = new java.util.ArrayList<>();

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
            orders.add(Sort.Order.desc("createdAt"));
        }

        return Sort.by(orders);
    }
}
