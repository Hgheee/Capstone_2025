package com.lostfound.capstonebackend.domain.admin;

import com.lostfound.capstonebackend.common.dto.ApiResponse;
import com.lostfound.capstonebackend.domain.lost112.Lost112ApiService;
import com.lostfound.capstonebackend.domain.lost112.Lost112ImportService;
import com.lostfound.capstonebackend.domain.lost112.dto.Lost112ImportRequest;
import com.lostfound.capstonebackend.domain.lost112.dto.PythonCollectionResult;
import com.lostfound.capstonebackend.domain.lost112.dto.PythonDataSummary;
import com.lostfound.capstonebackend.domain.seoul.SeoulLostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 관리자 전용 기능을 제공하는 컨트롤러입니다.
 * LOST112 데이터 수집, 동기화, 상태 확인 등 관리자 권한이 필요한 API를 포함합니다.
 * 모든 엔드포인트는 '/api/admin' 경로에 매핑되며, ADMIN 역할을 가진 사용자만 접근할 수 있습니다.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "관리자 전용", description = "관리자 전용 API (데이터 수집, 점검 등)")
public class AdminController {

    private final Lost112ImportService lost112ImportService;
    private final Lost112ApiService lost112ApiService;
    private final SeoulLostService seoulLostService;
    private final com.lostfound.capstonebackend.domain.lostitem.LostItemService lostItemService;

    /**
     * LOST112 API를 통해 분실물 데이터를 수집하고 데이터베이스에 저장합니다.
     * 요청 본문을 통해 데이터 수집 기간, 지역, 페이지 수 등을 지정할 수 있습니다.
     *
     * @param userDetails 인증된 관리자 정보. API 호출 권한을 확인하고 로그에 기록하는 데 사용됩니다.
     * @param request     데이터 수집 옵션(기간, 지역 등)을 담은 DTO. 선택 사항입니다.
     * @return 데이터 수집 결과를 담은 ApiResponse 객체. 성공 여부, 처리 건수 등의 정보를 포함합니다.
     */
    @PostMapping("/import/lost112")
    @Operation(summary = "LOST112 데이터 수집 (자동 경로)",
            description = "LOST112 API 또는 Python 스크립트를 사용해 분실물 데이터를 수집하고 DB에 저장합니다."
                    + "  요청 본문이 없으면 기본 설정으로 수행되며, 요청 본문을 통해 기간/지역/페이지 수 등을 조정할 수 있습니다.")
    public ApiResponse<Map<String, Object>> importLost112Data(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) Lost112ImportRequest request
    ) {
        log.info("LOST112 데이터 수집 요청 - 사용자: {}, 요청 파라미터: {}", userDetails.getUsername(), request);

        try {
            // LOST112 API를 호출해 지정된 조건으로 데이터를 수집합니다.
            Lost112ImportService.Lost112ImportResult result = lost112ImportService.importLost112Data(request);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "LOST112 데이터 수집이 완료되었습니다.");
            response.put("totalFetched", result.getTotalFetched());
            response.put("newlyCreated", result.getNewlyCreated());
            response.put("duplicatesSkipped", result.getDuplicatesSkipped());
            response.put("summary", result.toString());
            if (!result.getMeta().isEmpty()) {
                response.put("meta", result.getMeta());
            }

            log.info("LOST112 데이터 수집 완료 - 사용자: {}, 결과: {}", userDetails.getUsername(), result);
            return ApiResponse.ok(response);

        } catch (Exception e) {
            log.error("LOST112 데이터 수집 중 오류 - 사용자: {}, 요청: {}", userDetails.getUsername(), request, e);

            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "LOST112 데이터 수집 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("totalFetched", 0);
            errorResponse.put("newlyCreated", 0);
            errorResponse.put("duplicatesSkipped", 0);
            if (request != null) {
                errorResponse.put("requestedOptions", request);
            }

            return ApiResponse.ok(errorResponse);
        }
    }

    /**
     * Python 스크립트를 실행하여 LOST112 분실물 데이터를 수집하고 임시 테이블에 저장합니다.
     * NullPointerException 완전 해결 + PowerShell과 동일한 실행 환경 구현
     *
     * @param userDetails 인증된 관리자 정보.
     * @param request     Python 스크립트 실행 옵션(기간, 지역 등)을 담은 DTO. 선택 사항입니다.
     * @return 스크립트 실행 결과를 담은 ApiResponse 객체. 성공 여부, 수집 건수, 실행 시간 등의 정보를 포함합니다.
     */
    @PostMapping("/lost112/collect-python")
    @Operation(summary = "Python 스크립트 실행으로 LOST112 수집",
            description = "관리자가 Python 스크립트를 직접 실행해 LOST112 데이터를 임시 테이블에 적재합니다."
                    + "  요청 본문에서 기간, 지역 코드, 페이지 크기 등을 선택적으로 지정할 수 있습니다.")
    public ApiResponse<Map<String, Object>> collectLost112WithPython(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false)
            @Parameter(description = "수집 기간/지역/페이지 옵션. 생략 시 기본 설정 사용") Lost112ImportRequest request
    ) {
        log.info("=== LOST112 Python 수집 시작 ===");
        log.info("요청 사용자: {}", userDetails.getUsername());
        log.info("요청 객체: {}", request);

        try {
            // 기본값 설정
            String startDate = "2025-09-23";
            String endDate = "2025-09-23";
            Integer maxPages = 1;
            Integer rowsPerPage = 5;
            Integer timeoutSeconds = 60;

            // request 완전 안전 처리
            if (request != null) {
                log.info("Request 객체 존재 - 안전한 필드 추출 시작");

                // startDate 완전 안전 처리
                try {
                    Object startDateField = request.getStartDate();
                    if (startDateField != null) {
                        if (startDateField instanceof LocalDate) {
                            startDate = ((LocalDate) startDateField).toString();
                            log.info("LocalDate startDate: {}", startDate);
                        } else if (startDateField instanceof String) {
                            startDate = (String) startDateField;
                            log.info("String startDate: {}", startDate);
                        } else {
                            startDate = String.valueOf(startDateField);
                            log.info("valueOf startDate: {}", startDate);
                        }
                    }
                } catch (Exception e) {
                    log.warn("startDate 처리 중 오류, 기본값 유지: {}", e.getMessage());
                }

                // endDate 완전 안전 처리
                try {
                    Object endDateField = request.getEndDate();
                    if (endDateField != null) {
                        if (endDateField instanceof LocalDate) {
                            endDate = ((LocalDate) endDateField).toString();
                            log.info("LocalDate endDate: {}", endDate);
                        } else if (endDateField instanceof String) {
                            endDate = (String) endDateField;
                            log.info("String endDate: {}", endDate);
                        } else {
                            endDate = String.valueOf(endDateField);
                            log.info("valueOf endDate: {}", endDate);
                        }
                    }
                } catch (Exception e) {
                    log.warn("endDate 처리 중 오류, 기본값 유지: {}", e.getMessage());
                }

                // maxPages 안전 처리
                try {
                    if (request.getMaxPages() != null) {
                        maxPages = request.getMaxPages();
                        log.info("maxPages: {}", maxPages);
                    }
                } catch (Exception e) {
                    log.warn("maxPages 처리 중 오류, 기본값 유지: {}", e.getMessage());
                }

                // rowsPerPage 안전 처리
                try {
                    if (request.getRowsPerPage() != null) {
                        rowsPerPage = request.getRowsPerPage();
                        log.info("rowsPerPage: {}", rowsPerPage);
                    }
                } catch (Exception e) {
                    log.warn("rowsPerPage 처리 중 오류, 기본값 유지: {}", e.getMessage());
                }
            }

            // 날짜 형식 변환
            String startYmd = startDate.replace("-", "");
            String endYmd = endDate.replace("-", "");

            log.info("최종 파라미터: startDate={}, endDate={}, maxPages={}, rowsPerPage={}",
                    startDate, endDate, maxPages, rowsPerPage);

            // 작업 디렉토리 설정
            File workingDir = new File("C:\\Users\\User\\Desktop\\Capstone_2025\\backend\\capstone-backend\\data");
            File pythonScript = new File(workingDir, "lost112_collect_and_sync_fast.py");

            log.info("=== 파일 시스템 확인 ===");
            log.info("작업 디렉토리: {} (존재: {})", workingDir.getAbsolutePath(), workingDir.exists());
            log.info("Python 스크립트: {} (존재: {})", pythonScript.getAbsolutePath(), pythonScript.exists());

            if (!workingDir.exists()) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("success", false);
                error.put("message", "작업 디렉토리가 존재하지 않습니다: " + workingDir.getAbsolutePath());
                error.put("errorType", "DirectoryNotFound");
                error.put("totalCollected", 0);
                return ApiResponse.ok(error);
            }

            if (!pythonScript.exists()) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("success", false);
                error.put("message", "Python 스크립트가 존재하지 않습니다: " + pythonScript.getAbsolutePath());
                error.put("errorType", "FileNotFound");
                error.put("totalCollected", 0);
                return ApiResponse.ok(error);
            }

            // ProcessBuilder 구성
            ProcessBuilder pb = new ProcessBuilder(
                    "python", "lost112_collect_and_sync_fast.py",
                    "--start-ymd", startYmd,
                    "--end-ymd", endYmd,
                    "--max-pages", String.valueOf(maxPages),
                    "--rows-per-page", String.valueOf(rowsPerPage),
                    "--timeout", String.valueOf(timeoutSeconds)
            );

            pb.directory(workingDir);

            // 환경변수 완전 복사 (PowerShell과 동일한 환경)
            Map<String, String> env = pb.environment();
            env.clear();
            env.putAll(System.getenv()); // 시스템 환경변수 모두 상속
            env.put("PYTHONIOENCODING", "utf-8");
            env.put("PYTHONUNBUFFERED", "1");

            log.info("=== ProcessBuilder 설정 완료 ===");
            log.info("실행 명령어: {}", String.join(" ", pb.command()));
            log.info("작업 디렉토리: {}", pb.directory().getAbsolutePath());
            log.info("환경변수 개수: {}", env.size());

            // Python 프로세스 실행
            log.info("Python 프로세스 실행 시작...");
            long startTime = System.currentTimeMillis();

            Process process = pb.start();

            // 출력 읽기
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread stdoutThread = new Thread(() -> {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.append(line).append("\n");
                        log.info("STDOUT: {}", line);
                    }
                } catch (Exception e) {
                    log.error("stdout 읽기 오류", e);
                }
            });

            Thread stderrThread = new Thread(() -> {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getErrorStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                        log.warn("STDERR: {}", line);
                    }
                } catch (Exception e) {
                    log.error("stderr 읽기 오류", e);
                }
            });

            stdoutThread.start();
            stderrThread.start();

            // 프로세스 완료 대기
            boolean finished = process.waitFor(120, TimeUnit.SECONDS);

            stdoutThread.join(5000);
            stderrThread.join(5000);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            int exitCode = finished ? process.exitValue() : -1;

            log.info("=== Python 실행 완료 ===");
            log.info("프로세스 완료: {}", finished);
            log.info("exitCode: {}", exitCode);
            log.info("실행 시간: {}ms", duration);

            if (!finished) {
                process.destroyForcibly();
                Map<String, Object> timeout = new LinkedHashMap<>();
                timeout.put("success", false);
                timeout.put("message", "Python 스크립트 실행 시간 초과");
                timeout.put("errorType", "TimeoutException");
                timeout.put("totalCollected", 0);
                timeout.put("durationMillis", duration);
                return ApiResponse.ok(timeout);
            }

            // 응답 생성
            Map<String, Object> response = new LinkedHashMap<>();
            String stdoutStr = stdout.toString();
            String stderrStr = stderr.toString();

            if (exitCode == 0 && stdoutStr.contains("\"success\": true")) {
                log.info("🎉 Python 스크립트 성공!");
                response.put("success", true);
                response.put("message", "Python 데이터 수집이 성공적으로 완료되었습니다.");

                int totalCollected = 1;
                if (stdoutStr.contains("\"upserted\":")) {
                    try {
                        String[] parts = stdoutStr.split("\"upserted\":");
                        if (parts.length > 1) {
                            String numberPart = parts[1].split(",")[0].trim();
                            totalCollected = Integer.parseInt(numberPart);
                        }
                    } catch (Exception e) {
                        log.warn("수집 건수 파싱 실패", e);
                    }
                }

                response.put("totalCollected", totalCollected);
                response.put("pythonOutput", stdoutStr);

            } else {
                log.error("❌ Python 스크립트 실행 실패");
                response.put("success", false);
                response.put("message", "Python 스크립트 실행 실패 - exitCode=" + exitCode);
                response.put("errorType", "PythonExecutionError");
                response.put("totalCollected", 0);
                response.put("stdout", stdoutStr);
                response.put("stderr", stderrStr);
                response.put("exitCode", exitCode);
            }

            response.put("executedAt", java.time.LocalDateTime.now().toString());
            response.put("durationMillis", duration);
            response.put("requestedOptions", Map.of(
                    "startDate", startDate,
                    "endDate", endDate,
                    "maxPages", maxPages,
                    "rowsPerPage", rowsPerPage
            ));

            log.info("=== 최종 응답 완료 - success: {} ===", response.get("success"));
            return ApiResponse.ok(response);

        } catch (Exception e) {
            log.error("=== Python 실행 중 Java 예외 발생 ===", e);
            log.error("예외 타입: {}", e.getClass().getSimpleName());
            log.error("예외 메시지: {}", e.getMessage());
            log.error("스택트레이스 첫 줄: {}", e.getStackTrace().length > 0 ? e.getStackTrace()[0] : "없음");

            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", "Java에서 Python 실행 중 예외 발생: " + e.getMessage());
            error.put("errorType", e.getClass().getSimpleName());
            error.put("totalCollected", 0);
            error.put("javaException", e.toString());
            error.put("stackTrace", e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "없음");

            return ApiResponse.ok(error);
        }
    }

    /**
     * Python 스크립트를 통해 임시 테이블에 수집된 분실물 데이터를 조회합니다.
     *
     * @param page     조회할 페이지 번호 (0부터 시작).
     * @param size     페이지 당 항목 수.
     * @param fromDate 조회할 데이터의 시작 날짜. 이 날짜 이후의 데이터만 필터링됩니다. (선택 사항)
     * @return 임시 테이블의 데이터 요약 및 페이지네이션된 목록을 담은 ApiResponse 객체.
     */
    @GetMapping("/lost112/python-data")
    @Operation(summary = "Python 수집 데이터 조회",
            description = "임시 테이블(lost_items_temp)에 저장된 데이터를 페이지네이션하여 조회합니다."
                    + "  fromDate를 지정하면 해당 날짜 이후 데이터만 필터링합니다.")
    public ApiResponse<PythonDataSummary> getPythonCollectedData(
            @RequestParam(defaultValue = "0")
            @Parameter(description = "조회할 페이지 번호(0부터 시작)") int page,
            @RequestParam(defaultValue = "20")
            @Parameter(description = "페이지 당 데이터 수") int size,
            @RequestParam(name = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "이 날짜 이후의 습득 데이터만 조회") LocalDate fromDate
    ) {
        log.info("LOST112 Python 데이터 조회 요청 - page: {}, size: {}, fromDate: {}", page, size, fromDate);
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        PythonDataSummary summary = lost112ImportService.getPythonData(fromDate, pageable);
        log.info("LOST112 Python 데이터 조회 응답 - 총 건수: {}, 현재 페이지 건수: {}", summary.getTotalCount(), summary.getCurrentPageCount());
        return ApiResponse.ok(summary);
    }

    /**
     * Python 스크립트로 데이터를 수집하고, 즉시 메인 분실물 테이블과 동기화합니다.
     * 이 과정에서 중복된 데이터는 건너뛰고 새로운 데이터만 추가됩니다.
     *
     * @param userDetails 인증된 관리자 정보.
     * @param request     수집 및 동기화 옵션을 담은 DTO. 선택 사항입니다.
     * @return 동기화 처리 결과를 담은 ApiResponse 객체.
     */
    @PostMapping("/lost112/collect-and-sync")
    @Operation(summary = "Python 수집 후 메인 테이블 동기화",
            description = "Python 스크립트를 실행해 임시 테이블에 적재한 뒤, 즉시 메인 분실물 테이블과 동기화합니다."
                    + "  중복 데이터는 자동으로 건너뛰며, 신규 데이터만 저장합니다.")
    public ApiResponse<Map<String, Object>> collectAndSyncLost112(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false)
            @Parameter(description = "수집 및 동기화 옵션. 생략 시 기본 설정 사용") Lost112ImportRequest request
    ) {
        log.info("LOST112 Python 수집+동기화 요청 - 사용자: {}, 요청: {}", userDetails.getUsername(), request);
        try {
            // Python 수집 결과를 DB와 즉시 동기화하여 최신 상태를 맞춥니다.
            Map<String, Object> result = lost112ImportService.collectWithPythonAndSync(request);
            log.info("LOST112 Python 수집+동기화 컨트롤러 응답 - keys: {}", result.keySet());
            return ApiResponse.ok(result);
        } catch (Exception e) {
            log.error("LOST112 Python 수집+동기화 오류 - 사용자: {}, 요청: {}", userDetails.getUsername(), request, e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("message", "Python 수집 및 동기화 중 오류가 발생했습니다: " + e.getMessage());
            error.put("errorType", e.getClass().getSimpleName());
            if (request != null) {
                error.put("requestedOptions", request);
            }
            return ApiResponse.ok(error);
        }
    }

    /**
     * LOST112 공공 데이터 API의 현재 상태를 확인합니다.
     * 테스트 API를 호출하여 정상 작동 여부와 응답 시간을 확인합니다.
     *
     * @return API 상태(정상/오류), 테스트 결과 등을 담은 ApiResponse 객체.
     */
    @GetMapping("/lost112/status")
    @Operation(summary = "LOST112 API 상태 확인",
            description = "LOST112 공공 데이터 API 호출이 정상인지 여부와 샘플 데이터 수를 확인합니다.")
    public ApiResponse<Map<String, Object>> checkLost112Status() {
        try {
            var testResult = lost112ApiService.fetchLostItems(1, 1);

            Map<String, Object> status = Map.of(
                    "apiStatus", "정상",
                    "testDataCount", testResult.size(),
                    "lastTestTime", java.time.LocalDateTime.now().toString()
            );

            return ApiResponse.ok(status);

        } catch (Exception e) {
            Map<String, Object> status = Map.of(
                    "apiStatus", "오류",
                    "error", e.getMessage(),
                    "lastTestTime", java.time.LocalDateTime.now().toString()
            );

            return ApiResponse.ok(status);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/seoul/import")
    public ResponseEntity<Map<String, Object>> importSeoulData() {
        int imported = seoulLostService.importSeoulLostItems();
        return ResponseEntity.ok(Map.of(
                "source", "SEOUL_LOST",
                "imported", imported
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import-all")
    public ResponseEntity<Map<String, Object>> importAllSources() {
        Map<String, Integer> results = new HashMap<>();

        try {
            var lost112Result = lost112ImportService.importLost112Data();
            results.put("LOST112", lost112Result.getNewlyCreated());
        } catch (Exception e) {
            log.error("LOST112 통합 수집 실패", e);
            results.put("LOST112", 0);
        }

        try {
            results.put("SEOUL_LOST", seoulLostService.importSeoulLostItems());
        } catch (Exception e) {
            log.error("서울시 분실물 통합 수집 실패", e);
            results.put("SEOUL_LOST", 0);
        }

        int total = results.values().stream().mapToInt(Integer::intValue).sum();
        Map<String, Object> response = new LinkedHashMap<>();
        results.forEach(response::put);
        response.put("total", total);
        return ResponseEntity.ok(response);
    }

    /**
     * LOST112 데이터를 지역별로 수집합니다 (개선된 버전)
     * @param startYmd 시작 날짜 (YYYYMMDD)
     * @param endYmd 종료 날짜 (YYYYMMDD)
     * @param regions 수집할 지역 (쉼표로 구분, 예: "서울,경기,부산")
     * @return 수집 결과
     */
    @PostMapping("/import/lost112-by-region")
    @Operation(summary = "LOST112 지역별 데이터 수집", 
               description = "지역 코드와 날짜 범위를 지정하여 LOST112 데이터를 수집합니다.")
    public ApiResponse<Map<String, Object>> importLost112ByRegion(
            @RequestParam(required = false) String startYmd,
            @RequestParam(required = false) String endYmd,
            @RequestParam(required = false, defaultValue = "서울,경기,부산,인천") String regions) {
        
        log.info("LOST112 지역별 데이터 수집 요청 - 날짜: {} ~ {}, 지역: {}", startYmd, endYmd, regions);
        
        try {
            // 날짜 기본값 설정 (최근 7일)
            if (startYmd == null || startYmd.isEmpty()) {
                startYmd = java.time.LocalDate.now().minusDays(7).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
            }
            if (endYmd == null || endYmd.isEmpty()) {
                endYmd = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
            }
            
            // 지역명을 코드로 변환
            List<String> regionCodes = new ArrayList<>();
            for (String region : regions.split(",")) {
                String trimmed = region.trim();
                String code = com.lostfound.capstonebackend.common.util.Lost112RegionCodes.getCodeByName(trimmed);
                if (code != null) {
                    regionCodes.add(code);
                } else {
                    log.warn("알 수 없는 지역: {}", trimmed);
                }
            }
            
            if (regionCodes.isEmpty()) {
                Map<String, Object> errorResponse = new LinkedHashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "유효한 지역이 없습니다: " + regions);
                return ApiResponse.ok(errorResponse);
            }
            
            // 데이터 수집
            List<com.lostfound.capstonebackend.domain.lost112.dto.Lost112ItemDto> items = 
                    lost112ImportService.lost112ApiService.fetchAllRegions(regionCodes, startYmd, endYmd);
            
            log.info("LOST112 지역별 수집 완료 - 총 {}개 아이템", items.size());
            
            // 데이터 저장은 기존 로직 활용
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "LOST112 지역별 데이터 수집이 완료되었습니다.");
            response.put("totalFetched", items.size());
            response.put("regions", regionCodes);
            response.put("startYmd", startYmd);
            response.put("endYmd", endYmd);
            response.put("timestamp", java.time.LocalDateTime.now().toString());
            
            return ApiResponse.ok(response);
            
        } catch (Exception e) {
            log.error("LOST112 지역별 수집 실패", e);
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "데이터 수집 중 오류가 발생했습니다: " + e.getMessage());
            return ApiResponse.ok(errorResponse);
        }
    }

    /**
     * 지역 정보 통계를 조회합니다.
     * @return 지역별 분실물 통계
     */
    @GetMapping("/region-stats")
    @Operation(summary = "지역 정보 통계", description = "전체 데이터의 지역 정보 분포를 확인합니다.")
    public ApiResponse<Map<String, Object>> getRegionStats() {
        try {
            long totalCount = lostItemService.getTotalCount();
            long withRegion = lostItemService.getCountWithRegion();
            long withoutRegion = totalCount - withRegion;
            
            Map<String, Long> regionDistribution = lostItemService.getRegionDistribution();
            
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalCount", totalCount);
            stats.put("withRegion", withRegion);
            stats.put("withoutRegion", withoutRegion);
            stats.put("regionCoverage", totalCount > 0 ? (double) withRegion / totalCount * 100 : 0);
            stats.put("regionDistribution", regionDistribution);
            
            return ApiResponse.ok(stats);
        } catch (Exception e) {
            log.error("지역 통계 조회 실패", e);
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "지역 통계 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ApiResponse.ok(errorResponse);
        }
    }

    /**
     * 기존 분실물 데이터의 region 필드를 업데이트합니다.
     * location이나 storageLocation에서 지역 정보를 추출하여 저장합니다.
     * 
     * @param userDetails 인증된 관리자 정보
     * @return 업데이트 결과를 담은 ApiResponse 객체
     */
    @PostMapping("/update-regions")
    @Operation(summary = "지역 정보 추출 및 업데이트",
            description = "기존 분실물 데이터에서 location/storageLocation 필드로부터 지역 정보를 추출하여 region 필드에 저장합니다.")
    public ApiResponse<Map<String, Object>> updateRegions() {
        log.info("분실물 지역 정보 업데이트 요청");

        try {
            int updatedCount = lostItemService.updateAllRegions();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "지역 정보 업데이트가 완료되었습니다.");
            response.put("updatedCount", updatedCount);
            response.put("timestamp", java.time.LocalDateTime.now().toString());

            log.info("지역 정보 업데이트 완료 - 업데이트된 항목 수: {}", updatedCount);
            return ApiResponse.ok(response);

        } catch (Exception e) {
            log.error("지역 정보 업데이트 실패", e);
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "지역 정보 업데이트 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            return ApiResponse.ok(errorResponse);
        }
    }

    /**
     * 특정 ID 범위의 분실물 데이터의 region 필드를 업데이트합니다.
     * 
     * @param startId 시작 ID
     * @param endId 종료 ID
     * @param userDetails 인증된 관리자 정보
     * @return 업데이트 결과를 담은 ApiResponse 객체
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/update-regions/range")
    @Operation(summary = "ID 범위별 지역 정보 업데이트",
            description = "특정 ID 범위의 분실물 데이터에서 지역 정보를 추출하여 업데이트합니다.")
    public ApiResponse<Map<String, Object>> updateRegionsByRange(
            @Parameter(description = "시작 ID") @RequestParam Long startId,
            @Parameter(description = "종료 ID") @RequestParam Long endId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("분실물 지역 정보 범위 업데이트 요청 - 관리자: {}, 범위: {}-{}", 
                userDetails.getUsername(), startId, endId);

        try {
            int updatedCount = lostItemService.updateRegionsByIdRange(startId, endId);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", String.format("ID %d-%d 범위의 지역 정보 업데이트가 완료되었습니다.", startId, endId));
            response.put("updatedCount", updatedCount);
            response.put("startId", startId);
            response.put("endId", endId);
            response.put("timestamp", java.time.LocalDateTime.now().toString());

            log.info("지역 정보 범위 업데이트 완료 - 업데이트된 항목 수: {}", updatedCount);
            return ApiResponse.ok(response);

        } catch (Exception e) {
            log.error("지역 정보 범위 업데이트 실패", e);
            Map<String, Object> errorResponse = new LinkedHashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "지역 정보 업데이트 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
            return ApiResponse.ok(errorResponse);
        }
    }
}
