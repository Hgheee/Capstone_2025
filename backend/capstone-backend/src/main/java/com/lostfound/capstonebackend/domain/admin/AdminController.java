package com.lostfound.capstonebackend.domain.admin;

import com.lostfound.capstonebackend.common.dto.ApiResponse;
import com.lostfound.capstonebackend.domain.lost112.Lost112ApiService;
import com.lostfound.capstonebackend.domain.lost112.Lost112JavaImportService;
import com.lostfound.capstonebackend.domain.lost112.Lost112JavaImportService.CollectionSummary;
import com.lostfound.capstonebackend.domain.lost112.dto.Lost112CollectRequest;
import com.lostfound.capstonebackend.domain.seoul.SeoulLostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 관리자 전용 데이터 수집 및 점검 API.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "관리자 전용", description = "관리자 전용 API (인증 필요)")
public class AdminController {

    private final Lost112JavaImportService lost112JavaImportService;
    private final Lost112ApiService lost112ApiService;
    private final SeoulLostService seoulLostService;

    /**
     * LOST112 데이터를 수집하여 임시 테이블에 저장한다.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/lost112/collect")
    @Operation(summary = "LOST112 데이터 수집", description = "LOST112 Open API에서 데이터를 가져와 임시 테이블에 업서트합니다.")
    public ApiResponse<Map<String, Object>> collectLost112(@RequestBody(required = false) Lost112CollectRequest request) {
        Lost112CollectRequest safe = Optional.ofNullable(request).orElseGet(Lost112CollectRequest::new);

        CollectionSummary summary = lost112JavaImportService.collectAndUpsert(
                safe.getStartDate(),
                safe.getEndDate(),
                safe.getRegionCode()
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("startDate", summary.startDate());
        response.put("endDate", summary.endDate());
        response.put("regionCode", Optional.ofNullable(summary.regionCode()).filter(code -> !code.isBlank()).orElse("ALL"));
        response.put("totalFetched", summary.totalFetched());
        response.put("inserted", summary.insertedCount());
        response.put("updated", summary.updatedCount());

        log.info("LOST112 수집 완료 - {}", response);
        return ApiResponse.ok(response);
    }

    /**
     * 서울 열린데이터 광장에서 제공하는 분실물 데이터를 수집한다.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/seoul/import")
    @Operation(summary = "서울 열린데이터 수집", description = "서울 열린데이터 광장 분실물 정보를 수집합니다.")
    public ResponseEntity<Map<String, Object>> importSeoulData() {
        int imported = seoulLostService.importSeoulLostItems();
        Map<String, Object> response = Map.of(
                "source", "SEOUL_LOST",
                "imported", imported
        );
        log.info("서울 열린데이터 수집 완료 - {}", response);
        return ResponseEntity.ok(response);
    }

    /**
     * LOST112와 서울 열린데이터를 모두 수집한다.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import-all")
    @Operation(summary = "전체 데이터 수집", description = "LOST112와 서울 열린데이터를 한 번에 수집합니다.")
    public ResponseEntity<Map<String, Object>> importAllSources() {
        CollectionSummary lost112Summary = lost112JavaImportService.collectAndUpsert(LocalDate.now(), LocalDate.now(), "");
        int seoulImported = seoulLostService.importSeoulLostItems();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("lost112", Map.of(
                "totalFetched", lost112Summary.totalFetched(),
                "inserted", lost112Summary.insertedCount(),
                "updated", lost112Summary.updatedCount()
        ));
        response.put("seoulLost", Map.of(
                "imported", seoulImported
        ));
        response.put("totalSources", 2);

        log.info("전체 데이터 수집 완료 - {}", response);
        return ResponseEntity.ok(response);
    }

    /**
     * LOST112 Open API 상태 확인.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/lost112/status")
    @Operation(summary = "LOST112 API 상태 확인", description = "LOST112 Open API 호출 가능 여부를 점검합니다.")
    public ApiResponse<Map<String, Object>> checkLost112Status() {
        try {
            LocalDate today = LocalDate.now();
            var page = lost112ApiService.fetchPage(today, today, "", 1, 1);

            Map<String, Object> status = Map.of(
                    "apiStatus", "정상",
                    "testDataCount", page.items().size(),
                    "totalCount", page.totalCount(),
                    "lastTestTime", java.time.LocalDateTime.now().toString()
            );
            return ApiResponse.ok(status);
        } catch (Exception e) {
            log.warn("LOST112 API 상태 점검 실패", e);
            Map<String, Object> status = Map.of(
                    "apiStatus", "오류",
                    "error", e.getMessage(),
                    "lastTestTime", java.time.LocalDateTime.now().toString()
            );
            return ApiResponse.ok(status);
        }
    }
}
