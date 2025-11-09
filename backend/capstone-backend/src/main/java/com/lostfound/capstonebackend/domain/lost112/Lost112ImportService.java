package com.lostfound.capstonebackend.domain.lost112;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.lostfound.capstonebackend.common.util.RegionUtil;
import com.lostfound.capstonebackend.config.Lost112Properties;
import com.lostfound.capstonebackend.domain.lost112.dto.Lost112ImportRequest;
import com.lostfound.capstonebackend.domain.lost112.dto.Lost112ItemDto;
import com.lostfound.capstonebackend.domain.lost112.dto.PythonCollectionResult;
import com.lostfound.capstonebackend.domain.lost112.dto.PythonDataSummary;
import com.lostfound.capstonebackend.domain.lostitem.LostItem;
import com.lostfound.capstonebackend.domain.lostitem.LostItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * LOST112 외부 데이터 연동 및 동기화를 총괄하는 서비스입니다.
 * Python 스크립트를 실행하여 데이터를 수집하고, 이를 데이터베이스에 저장 및 동기화하는 역할을 담당합니다.
 */
@Service
@Slf4j
@Transactional
public class Lost112ImportService {

    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final TypeReference<List<Lost112ItemDto>> LOST112_ITEMS_TYPE = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> GENERIC_MAP_TYPE = new TypeReference<>() {};
    private static final Set<String> SUCCESS_STATUSES = Set.of("ok", "success", "done", "completed");
    private static final String DEFAULT_REGION_CODE = "01";

    private static final String DATA_DIR = "C:\\Users\\User\\Desktop\\Capstone_2025\\data";
    private static final String[] SCRIPT_CANDIDATES = {
            "lost112_collect_and_sync_fast.py",
            "fetch_lost112_paginated.py"
    };

    public final Lost112ApiService lost112ApiService;
    private final LostItemRepository lostItemRepository;
    private final Lost112TempRepository lost112TempRepository;
    private final Lost112Properties lost112Properties;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    private final boolean metricsEnabled;

    public Lost112ImportService(
            Lost112ApiService lost112ApiService,
            LostItemRepository lostItemRepository,
            Lost112TempRepository lost112TempRepository,
            Lost112Properties lost112Properties,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.lost112ApiService = lost112ApiService;
        this.lostItemRepository = lostItemRepository;
        this.lost112TempRepository = lost112TempRepository;
        this.lost112Properties = lost112Properties;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.metricsEnabled = meterRegistry != null;
    }

    /**
     * Python 스크립트를 실행하여 LOST112 데이터를 수집하고, 결과를 임시 테이블에 저장합니다.
     * @param request 데이터 수집 옵션(기간, 지역 등)
     * @return Python 스크립트 실행 결과 요약
     */
    public PythonCollectionResult collectLost112DataWithPython(Lost112ImportRequest request) {
        PythonConfig pythonConfig = createPythonConfig();
        ImportOptions options = buildImportOptions(request, pythonConfig);
        log.info("LOST112 Python 수집 요청(Temp 저장) - options: {}", options);

        PythonFetchResult fetchResult = executePythonFetch(options, pythonConfig);
        log.info("LOST112 Python 수집 결과 - 임시 저장 시작 (항목 수: {})", fetchResult.items().size());
        persistTempItems(fetchResult.items());

        String message = String.format("LOST112 Python 수집 완료: %d건 (%,dms)", fetchResult.items().size(), fetchResult.durationMillis());
        PythonCollectionResult result = buildPythonCollectionResult(fetchResult, message);
        log.info("LOST112 Python 임시 저장 완료 - 결과: {}", result);
        return result;
    }

    /**
     * Python 스크립트 실행을 위한 설정을 생성합니다.
     * @return Python 실행 관련 설정 객체
     */
    private PythonConfig createPythonConfig() {
        return new PythonConfig(
                "python",
                findPythonScript(),
                DATA_DIR,
                null,
                null,
                300,
                200000,
                Collections.emptyMap(),
                7,
                100,
                10,
                1.0,
                4,
                30.0
        );
    }

    /**
     * 지정된 경로에서 실행할 Python 스크립트 파일을 찾습니다.
     * @return 발견된 스크립트 파일의 절대 경로
     * @throws IllegalStateException 스크립트 파일을 찾지 못한 경우
     */
    private String findPythonScript() {
        for (String scriptName : SCRIPT_CANDIDATES) {
            File scriptFile = new File(DATA_DIR, scriptName);
            if (scriptFile.exists()) {
                log.info("Python 스크립트 발견: {}", scriptFile.getAbsolutePath());
                return scriptFile.getAbsolutePath();
            }
        }

        throw new IllegalStateException(
                "Python 스크립트를 찾을 수 없습니다. 위치: " + DATA_DIR +
                        ", 찾은 파일: " + String.join(", ", SCRIPT_CANDIDATES)
        );
    }

    /**
     * 요청(request)과 기본 설정을 조합하여 Python 스크립트 실행 옵션을 생성합니다.
     * @param request 사용자의 요청 파라미터
     * @param pythonConfig Python 기본 설정
     * @return 생성된 실행 옵션
     */
    private ImportOptions buildImportOptions(Lost112ImportRequest request, PythonConfig pythonConfig) {
        LocalDate endDate = Optional.ofNullable(request).map(Lost112ImportRequest::getEndDate).orElse(LocalDate.now());
        LocalDate startDate = Optional.ofNullable(request).map(Lost112ImportRequest::getStartDate).orElse(endDate.minusDays(Math.max(pythonConfig.defaultDayRange, 1)));

        if (startDate.isAfter(endDate)) {
            LocalDate tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }

        String regionCode = Optional.ofNullable(request).map(Lost112ImportRequest::getRegionCode).map(String::trim).filter(code -> !code.isEmpty()).orElse(DEFAULT_REGION_CODE);
        int rows = Optional.ofNullable(request).map(Lost112ImportRequest::getRowsPerPage).orElse(pythonConfig.defaultRows);
        rows = clamp(rows, 1, 100);

        int maxPages = Optional.ofNullable(request).map(Lost112ImportRequest::getMaxPages).orElse(pythonConfig.defaultMaxPages);
        maxPages = Math.max(1, maxPages);

        double sleepSec = pythonConfig.sleepSec;
        int concurrency = Math.max(1, pythonConfig.defaultConcurrency);

        Map<String, Object> requestMeta = new LinkedHashMap<>();
        requestMeta.put("requestedStartDate", startDate.toString());
        requestMeta.put("requestedEndDate", endDate.toString());
        requestMeta.put("startYmd", BASIC_DATE.format(startDate));
        requestMeta.put("endYmd", BASIC_DATE.format(endDate));
        requestMeta.put("regionCode", regionCode);
        requestMeta.put("rowsPerPage", rows);
        requestMeta.put("maxPages", maxPages);
        requestMeta.put("sleepSec", sleepSec);
        requestMeta.put("concurrency", concurrency);

        return new ImportOptions(BASIC_DATE.format(startDate), BASIC_DATE.format(endDate), regionCode, rows, maxPages, sleepSec, concurrency, requestMeta);
    }

    /**
     * 설정된 옵션을 기반으로 Python 데이터 수집 스크립트를 실행하고, 그 결과를 파싱하여 반환합니다.
     * @param options 스크립트 실행 옵션
     * @param pythonConfig Python 설정
     * @return 스크립트 실행 결과 (수집된 아이템, 메타 정보 등)
     * @throws IllegalStateException 스크립트 실행 실패 또는 결과 파싱 실패 시
     */
    private PythonFetchResult executePythonFetch(ImportOptions options, PythonConfig pythonConfig) {
        String scriptPath = pythonConfig.scriptPath;
        File workDir = new File(pythonConfig.workDir);

        if (!workDir.exists()) throw new IllegalStateException("작업 디렉토리가 존재하지 않습니다: " + workDir.getAbsolutePath());
        if (!new File(scriptPath).exists()) throw new IllegalStateException("Python 스크립트가 존재하지 않습니다: " + scriptPath);

        long timeoutSec = pythonConfig.timeoutSec;
        int maxOutputChars = pythonConfig.maxOutputChars;
        String scriptName = new File(scriptPath).getName();

        List<String> command = new ArrayList<>();
        command.add(pythonConfig.executable);
        command.add(scriptPath);
        command.add("--start-ymd");
        command.add(options.startYmd());
        command.add("--end-ymd");
        command.add(options.endYmd());
        command.add("--region-code");
        command.add(options.regionCode());
        command.add("--rows-per-page");
        command.add(Integer.toString(options.rows()));
        command.add("--max-pages");
        command.add(Integer.toString(options.maxPages()));
        command.add("--concurrency");
        command.add(Integer.toString(options.concurrency()));
        if (pythonConfig.requestTimeoutSec() > 0) {
            command.add("--timeout");
            command.add(Double.toString(pythonConfig.requestTimeoutSec()));
        }

        log.info("LOST112 Python 실행 시작 - script={}, startYmd={}, endYmd={}, maxPages={}, concurrency={}", scriptName, options.startYmd(), options.endYmd(), options.maxPages(), options.concurrency());
        log.debug("실행 명령어: {}", String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workDir);

        Map<String, String> environment = processBuilder.environment();
        environment.putIfAbsent("PYTHONUNBUFFERED", "1");
        environment.putIfAbsent("PYTHONIOENCODING", StandardCharsets.UTF_8.name());
        environment.putAll(pythonConfig.environment);

        ExecutorService executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("lost112-python-stream-" + thread.getId());
            thread.setDaemon(true);
            return thread;
        });

        Timer.Sample timerSample = metricsEnabled ? Timer.start(meterRegistry) : null;
        long wallStartNanos = System.nanoTime();
        boolean success = false;
        int itemCount = 0;

        try {
            Process process = processBuilder.start();
            Future<StreamCapture> stdoutFuture = executor.submit(() -> readStream(process.getInputStream(), maxOutputChars));
            Future<StreamCapture> stderrFuture = executor.submit(() -> readStream(process.getErrorStream(), maxOutputChars));

            boolean finished = process.waitFor(timeoutSec, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Python 스크립트가 " + timeoutSec + "초 내로 종료되지 않았습니다.");
            }

            int exitCode = process.exitValue();
            StreamCapture stdoutCapture = stdoutFuture.get();
            StreamCapture stderrCapture = stderrFuture.get();

            String stdout = stdoutCapture.content();
            String stderr = stderrCapture.content();

            if (stdoutCapture.truncated()) log.warn("LOST112 Python STDOUT이 {} 문자에서 잘렸습니다.", maxOutputChars);
            if (stderrCapture.truncated()) log.warn("LOST112 Python STDERR이 {} 문자에서 잘렸습니다.", maxOutputChars);
            if (stderr != null && !stderr.isBlank()) log.warn("LOST112 Python STDERR 출력: {}", abbreviate(stderr, 200));

            if (exitCode != 0) {
                if (metricsEnabled) meterRegistry.counter("lost112.python.errors", "script", scriptName, "stage", "nonZeroExit").increment();
                throw new IllegalStateException(String.format("Python 스크립트가 비정상 종료되었습니다. exitCode=%d, stderr=%s", exitCode, stderr));
            }

            if (stdout == null || stdout.isBlank()) {
                if (metricsEnabled) meterRegistry.counter("lost112.python.errors", "script", scriptName, "stage", "emptyStdout").increment();
                throw new IllegalStateException("Python 스크립트가 JSON 응답을 반환하지 않았습니다.");
            }

            JsonNode root = parsePythonJson(stdout);
            ensureSuccessfulPythonResult(root, stderr);

            NodeLookupResult itemsLookup = findFirstNode(root, "items", "data.items", "payload.items", "result.items", "data.payload.items", "payload.data.items", "response.items");
            List<Lost112ItemDto> items = parseItems(itemsLookup);
            itemCount = items.size();
            log.info("LOST112 Python 응답 파싱 완료 - itemsPath={}, itemCount={}", itemsLookup.path() == null ? "<unknown>" : itemsLookup.path(), itemCount);

            Map<String, Object> meta = extractMeta(root);
            if (meta.containsKey("durationSec")) log.info("LOST112 Python 실행 소요 시간: {}초", meta.get("durationSec"));

            meta = meta == null ? new LinkedHashMap<>() : new LinkedHashMap<>(meta);
            long wallElapsedNanosSuccess = System.nanoTime() - wallStartNanos;
            long durationMillis = TimeUnit.NANOSECONDS.toMillis(wallElapsedNanosSuccess);
            meta.putIfAbsent("javaDurationMillis", durationMillis);
            meta.putIfAbsent("concurrency", options.concurrency());
            meta.putIfAbsent("script", scriptName);
            if (itemsLookup.path() != null) meta.putIfAbsent("itemsPath", itemsLookup.path());
            meta.putIfAbsent("itemsCount", itemCount);

            String stderrPreview = null;
            if (stderr != null && !stderr.isBlank()) {
                stderrPreview = abbreviate(stderr, 200);
                meta.put("pythonStderr", abbreviate(stderr, 2000));
            }

            success = true;
            return new PythonFetchResult(items, Collections.unmodifiableMap(meta), stdout.trim(), stderrPreview, itemsLookup.path(), options.concurrency(), durationMillis);

        } catch (IOException e) {
            log.error("LOST112 Python 실행 I/O 실패 - script={}, message={}", scriptPath, e.getMessage(), e);
            if (metricsEnabled) meterRegistry.counter("lost112.python.errors", "script", scriptName, "stage", "io").increment();
            throw new IllegalStateException("Python 스크립트 실행 중 I/O 예외가 발생했습니다.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("LOST112 Python 실행이 인터럽트되었습니다 - script={}", scriptPath, e);
            if (metricsEnabled) meterRegistry.counter("lost112.python.errors", "script", scriptName, "stage", "interrupted").increment();
            throw new IllegalStateException("Python 스크립트 실행이 인터럽트되었습니다.", e);
        } catch (ExecutionException e) {
            log.error("LOST112 Python 출력 처리 실패 - script={}, message={}", scriptPath, e.getMessage(), e);
            if (metricsEnabled) meterRegistry.counter("lost112.python.errors", "script", scriptName, "stage", "execution").increment();
            throw new IllegalStateException("Python 스크립트 출력 처리 중 예외가 발생했습니다.", e.getCause());
        } finally {
            executor.shutdownNow();
            long wallElapsedNanos = System.nanoTime() - wallStartNanos;
            if (metricsEnabled && timerSample != null) {
                Timer timer = meterRegistry.timer("lost112.python.execution", "script", scriptName, "success", Boolean.toString(success));
                timerSample.stop(timer);
                meterRegistry.counter("lost112.python.calls", "script", scriptName, "success", Boolean.toString(success)).increment();
                if (success) {
                    meterRegistry.summary("lost112.python.items", "script", scriptName).record(itemCount);
                }
            }
        }
    }

    /** Python 실행 관련 설정을 담는 레코드 */
    private record PythonConfig(String executable, String scriptPath, String workDir, String outputDir, String envFile, long timeoutSec, int maxOutputChars, Map<String, String> environment, int defaultDayRange, int defaultRows, int defaultMaxPages, double sleepSec, int defaultConcurrency, double requestTimeoutSec) {}

    /**
     * Python 스크립트로 데이터를 수집하고, 즉시 메인 분실물 테이블과 동기화합니다.
     * @param request 수집 및 동기화 옵션
     * @return 동기화 처리 결과
     */
    public Map<String, Object> collectWithPythonAndSync(Lost112ImportRequest request) {
        PythonConfig pythonConfig = createPythonConfig();
        ImportOptions options = buildImportOptions(request, pythonConfig);
        log.info("LOST112 Python 수집+동기화 요청 - options: {}", options);

        PythonFetchResult fetchResult = executePythonFetch(options, pythonConfig);
        log.info("LOST112 Python 수집 결과 - 임시 저장 시작 (항목 수: {})", fetchResult.items().size());
        persistTempItems(fetchResult.items());

        Map<String, Object> mergedMeta = mergeMeta(fetchResult.meta(), options);
        Lost112ImportResult syncResult = saveLostItems(fetchResult.items(), mergedMeta);
        PythonCollectionResult pythonResult = buildPythonCollectionResult(fetchResult, String.format("LOST112 Python 수집 및 동기화 완료: 총 수집 %d건 (%,dms)", fetchResult.items().size(), fetchResult.durationMillis()));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("pythonResult", pythonResult);
        response.put("durationMillis", pythonResult.getDurationMillis());
        response.put("concurrency", pythonResult.getConcurrency());
        response.put("itemsPath", pythonResult.getItemsPath());
        response.put("stderrPreview", pythonResult.getStderrPreview());
        response.put("meta", Collections.unmodifiableMap(new LinkedHashMap<>(mergedMeta)));
        response.put("syncResult", Map.of("totalFetched", syncResult.getTotalFetched(), "newlyCreated", syncResult.getNewlyCreated(), "duplicatesSkipped", syncResult.getDuplicatesSkipped(), "meta", syncResult.getMeta()));

        log.info("LOST112 Python 수집+동기화 결과 - totalFetched: {}, 신규: {}, 중복: {}, durationMillis={}, concurrency={}", syncResult.getTotalFetched(), syncResult.getNewlyCreated(), syncResult.getDuplicatesSkipped(), pythonResult.getDurationMillis(), pythonResult.getConcurrency());

        return response;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        if (maxLength <= 3) return value.substring(0, Math.max(0, maxLength));
        return value.substring(0, maxLength - 3) + "...";
    }

    private JsonNode parsePythonJson(String stdout) {
        try {
            return objectMapper.readTree(stdout.trim());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Python 스크립트가 유효한 JSON을 반환하지 않았습니다.", e);
        }
    }

    private void ensureSuccessfulPythonResult(JsonNode root, String stderr) {
        if (root == null || root.isNull()) throw new IllegalStateException("Python 스크립트가 빈 JSON을 반환했습니다.");
        if (!root.isObject()) throw new IllegalStateException("Python 스크립트 응답이 JSON 객체가 아닙니다: " + root.getNodeType());

        boolean successFlag = !root.has("success") || root.path("success").asBoolean(true);
        String statusText = root.path("status").asText("").trim();
        boolean statusOk = statusText.isEmpty() || SUCCESS_STATUSES.contains(statusText.toLowerCase(Locale.ROOT));
        boolean hasErrorField = hasExplicitError(root);

        if (successFlag && statusOk && !hasErrorField) return;

        throw new IllegalStateException(buildPythonErrorMessage(root, stderr, successFlag, statusText));
    }

    private boolean hasExplicitError(JsonNode root) {
        JsonNode errorNode = root.get("error");
        if (errorNode != null && !errorNode.isNull() && !nodeToString(errorNode).isBlank()) return true;
        JsonNode errorsNode = root.get("errors");
        if (errorsNode != null && !errorsNode.isNull()) {
            if (errorsNode.isArray()) return errorsNode.size() > 0;
            if (errorsNode.isObject()) return errorsNode.size() > 0;
            return !nodeToString(errorsNode).isBlank();
        }
        return false;
    }

    private String buildPythonErrorMessage(JsonNode root, String stderr, boolean successFlag, String statusText) {
        List<String> parts = new ArrayList<>();
        parts.add("success=" + successFlag);
        if (!statusText.isBlank()) parts.add("status=" + statusText);
        String errorDetail = extractErrorDetails(root);
        if (!errorDetail.isBlank()) parts.add("error=" + errorDetail);
        if (stderr != null && !stderr.isBlank()) log.warn("LOST112 Python STDERR 출력: {}", abbreviate(stderr, 200));
        log.warn("LOST112 Python 응답 실패 - {}", String.join(", ", parts));
        return "Python 스크립트 응답이 실패 상태입니다: " + String.join(", ", parts);
    }

    private String extractErrorDetails(JsonNode root) {
        JsonNode errorNode = root.get("error");
        if (errorNode != null && !errorNode.isNull()) return nodeToString(errorNode);
        JsonNode errorsNode = root.get("errors");
        if (errorsNode != null && !errorsNode.isNull()) {
            if (errorsNode.isArray() && errorsNode.size() > 0) return nodeToString(errorsNode.get(0));
            return nodeToString(errorsNode);
        }
        JsonNode messageNode = root.get("message");
        if (messageNode != null && messageNode.isTextual()) return messageNode.asText();
        return "";
    }

    private String nodeToString(JsonNode node) {
        if (node == null || node.isNull()) return "";
        if (node.isTextual()) return node.asText();
        return node.toString();
    }

    private NodeLookupResult findFirstNode(JsonNode root, String... pathCandidates) {
        if (root == null) return new NodeLookupResult(null, MissingNode.getInstance());
        for (String candidate : pathCandidates) {
            if (candidate == null || candidate.isBlank()) continue;
            JsonNode current = root;
            boolean missing = false;
            for (String part : candidate.split("\\.")) {
                current = current.path(part);
                if (current.isMissingNode() || current.isNull()) {
                    missing = true;
                    break;
                }
            }
            if (!missing) return new NodeLookupResult(candidate, current);
        }
        return new NodeLookupResult(null, MissingNode.getInstance());
    }

    private Map<String, Object> extractMeta(JsonNode root) {
        Map<String, Object> meta = new LinkedHashMap<>();
        NodeLookupResult metaLookup = findFirstNode(root, "meta", "data.meta", "payload.meta", "result.meta", "response.meta");
        if (!metaLookup.node().isMissingNode()) {
            meta.putAll(convertNodeToMap(metaLookup.node()));
        }
        return meta;
    }

    private Map<String, Object> convertNodeToMap(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return Collections.emptyMap();
        if (node.isObject()) return new LinkedHashMap<>(objectMapper.convertValue(node, GENERIC_MAP_TYPE));
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("value", node.isValueNode() ? node.asText() : node.toString());
        return fallback;
    }

    private List<Lost112ItemDto> parseItems(NodeLookupResult lookup) {
        JsonNode itemsNode = lookup.node();
        if (itemsNode == null || itemsNode.isMissingNode() || !itemsNode.isArray()) {
            log.warn("LOST112 Python 응답에서 items 배열을 찾지 못해 빈 목록으로 처리합니다.");
            return Collections.emptyList();
        }
        try {
            List<Lost112ItemDto> items = objectMapper.readerFor(LOST112_ITEMS_TYPE).readValue(itemsNode);
            log.debug("LOST112 Python 응답 items 파싱 성공 - count={}", items.size());
            return items;
        } catch (IOException e) {
            throw new IllegalStateException("Python 스크립트 응답의 items 배열을 파싱하지 못했습니다.", e);
        }
    }

    private Map<String, Object> mergeMeta(Map<String, Object> scriptMeta, ImportOptions options) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (options.requestMeta() != null) merged.putAll(options.requestMeta());
        if (scriptMeta != null && !scriptMeta.isEmpty()) merged.putAll(scriptMeta);
        return merged;
    }

    private StreamCapture readStream(InputStream inputStream, int maxChars) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            boolean first = true;
            boolean truncated = false;
            while ((line = reader.readLine()) != null) {
                if (!first) builder.append('\n');
                else first = false;
                builder.append(line);
                if (builder.length() > maxChars) {
                    builder.setLength(maxChars);
                    truncated = true;
                    break;
                }
            }
            return new StreamCapture(builder.toString(), truncated);
        }
    }

    private PythonCollectionResult buildPythonCollectionResult(PythonFetchResult fetchResult, String message) {
        return PythonCollectionResult.builder().success(true).output(fetchResult.rawOutput()).message(message).totalCollected(fetchResult.items().size()).executedAt(LocalDateTime.now()).meta(fetchResult.meta()).itemsPath(fetchResult.itemsPath()).stderrPreview(fetchResult.stderrPreview()).concurrency(fetchResult.concurrency()).durationMillis(fetchResult.durationMillis()).build();
    }

    /**
     * DTO 리스트를 임시 테이블(Lost112TempEntity)에 저장합니다.
     * 저장 전, 기존 데이터를 모두 삭제합니다.
     * @param items 저장할 Lost112ItemDto 리스트
     */
    private void persistTempItems(List<Lost112ItemDto> items) {
        log.info("LOST112 임시 테이블 초기화 후 {}건 저장 예정", items == null ? 0 : items.size());
        lost112TempRepository.deleteAllInBatch();
        if (items == null || items.isEmpty()) return;
        List<Lost112TempEntity> entities = items.stream().map(this::convertToTempEntity).toList();
        lost112TempRepository.saveAll(entities);
    }

    /**
     * Lost112ItemDto를 임시 테이블 저장을 위한 Lost112TempEntity로 변환합니다.
     * @param dto 원본 DTO
     * @return 변환된 엔티티
     */
    private Lost112TempEntity convertToTempEntity(Lost112ItemDto dto) {
        return Lost112TempEntity.builder().itemId(cleanString(dto.getAtcId())).title(cleanString(dto.getFdPrdtNm())).foundDate(parseDate(dto.getFdYmd())).storagePlace(cleanString(dto.getDepPlace())).imageUrl(cleanString(dto.getFdFilePathImg())).color(cleanString(dto.getClrNm())).description(cleanString(dto.getFdSbjt())).category(mapCategory(dto.getPrdtClNm())).subcategory(null).categoryRaw(cleanString(dto.getPrdtClNm())).build();
    }

    /**
     * Lost112TempEntity를 Lost112ItemDto로 변환합니다.
     * @param entity 원본 엔티티
     * @return 변환된 DTO
     */
    private Lost112ItemDto convertFromTempEntity(Lost112TempEntity entity) {
        Lost112ItemDto dto = new Lost112ItemDto();
        dto.setAtcId(entity.getItemId());
        dto.setFdPrdtNm(entity.getTitle());
        dto.setFdSbjt(entity.getDescription());
        dto.setFdYmd(entity.getFoundDate() != null ? entity.getFoundDate().format(BASIC_DATE) : null);
        dto.setPrdtClNm(Optional.ofNullable(entity.getCategoryRaw()).orElse(entity.getCategory()));
        dto.setClrNm(entity.getColor());
        dto.setDepPlace(entity.getStoragePlace());
        dto.setFdFilePathImg(entity.getImageUrl());
        return dto;
    }

    /**
     * API나 스크립트로부터 받은 분실물 데이터 리스트를 메인 데이터베이스(lost_item)에 저장합니다.
     * 저장 전, 외부 ID(atcId)를 기준으로 중복 여부를 확인하여 중복된 데이터는 건너뜁니다.
     * @param apiItems 저장할 분실물 DTO 리스트
     * @param meta 관련 메타 데이터
     * @return 저장 결과 (총 수집 건수, 신규 저장 건수, 중복 건너뜀 건수)
     */
    private Lost112ImportResult saveLostItems(List<Lost112ItemDto> apiItems, Map<String, Object> meta) {
        Map<String, Object> metaToStore = meta == null ? Collections.emptyMap() : meta;

        if (apiItems == null || apiItems.isEmpty()) {
            log.warn("LOST112에서 수신한 데이터가 없어 저장을 건너뜁니다.");
            return new Lost112ImportResult(0, 0, 0, metaToStore);
        }

        int totalFetched = apiItems.size();
        int newlyCreated = 0;
        int duplicatesSkipped = 0;

        for (Lost112ItemDto apiItem : apiItems) {
            if (apiItem == null) continue;
            try {
                if (apiItem.getAtcId() != null) {
                    if (lostItemRepository.findByExternalId(apiItem.getAtcId()).isPresent()) {
                        duplicatesSkipped++;
                        continue;
                    }
                }

                LostItem lostItem = convertToLostItem(apiItem);
                lostItemRepository.save(lostItem);
                newlyCreated++;

                if (newlyCreated % 50 == 0) log.info("LOST112 데이터 저장 진행 상황: {}건 저장 완료", newlyCreated);
            } catch (Exception e) {
                log.error("LOST112 데이터 변환/저장 중 오류 atcId={}", apiItem.getAtcId(), e);
            }
        }

        log.info("LOST112 데이터 저장 완료 - 총 수집: {}건, 신규 저장: {}건, 중복 건너뜀: {}건", totalFetched, newlyCreated, duplicatesSkipped);

        return new Lost112ImportResult(totalFetched, newlyCreated, duplicatesSkipped, metaToStore);
    }

    public Lost112ImportResult importLost112Data() {
        log.info("LOST112 데이터 수집 시작 (Spring WebClient)");
        List<Lost112ItemDto> apiItems = lost112ApiService.fetchAllLostItems();
        return saveLostItems(apiItems, Collections.emptyMap());
    }

    public Lost112ImportResult importLost112Data(Lost112ImportRequest request) {
        PythonConfig pythonConfig = createPythonConfig();
        ImportOptions options = buildImportOptions(request, pythonConfig);
        log.info("LOST112 Python 스크립트를 통한 데이터 수집 시작: {}", options);
        PythonFetchResult fetchResult = executePythonFetch(options, pythonConfig);
        log.info("LOST112 Python 수집 WebClient 저장 단계 진입 - 항목 수: {}", fetchResult.items().size());
        Map<String, Object> mergedMeta = mergeMeta(fetchResult.meta(), options);
        log.info("LOST112 Python 스크립트 결과 - items: {}, meta: {}", fetchResult.items().size(), mergedMeta);
        return saveLostItems(fetchResult.items(), mergedMeta);
    }

    /**
     * 임시 테이블에 저장된 Python 수집 데이터를 페이지네이션하여 조회합니다.
     * @param fromDate 조회 시작 날짜 (선택 사항)
     * @param pageable 페이지네이션 정보
     * @return 데이터 요약 정보 및 DTO 리스트
     */
    @Transactional(readOnly = true)
    public PythonDataSummary getPythonData(LocalDate fromDate, Pageable pageable) {
        Page<Lost112TempEntity> page;
        if (fromDate != null) {
            page = lost112TempRepository.findByFoundDateAfter(fromDate.minusDays(1), pageable);
        } else {
            page = lost112TempRepository.findAllOrderByCreatedAtDesc(pageable);
        }

        List<Lost112ItemDto> items = page.getContent().stream().map(this::convertFromTempEntity).toList();

        return PythonDataSummary.builder().totalCount(page.getTotalElements()).currentPageCount(page.getNumberOfElements()).pageNumber(page.getNumber()).pageSize(page.getSize()).items(items).build();
    }

    /**
     * Lost112ItemDto를 메인 테이블 저장을 위한 LostItem 엔티티로 변환합니다.
     * @param dto 원본 DTO
     * @return 변환된 엔티티
     */
    private LostItem convertToLostItem(Lost112ItemDto dto) {
        String title = cleanString(dto.getFdPrdtNm());
        String location = cleanString(dto.getDepPlace());
        String storageLocation = cleanString(dto.getDepPlace());
        
        // 지역 정보 추출 (title 포함)
        String region = RegionUtil.extractRegionFromAll(title, location, storageLocation);
        
        return LostItem.builder()
                .title(title)
                .description(cleanString(dto.getFdSbjt()))
                .category(mapCategory(dto.getPrdtClNm()))
                .location(location)
                .region(region)
                .foundDate(parseDate(dto.getFdYmd()))
                .color(cleanString(dto.getClrNm()))
                .storageLocation(storageLocation)
                .imagePath(cleanString(dto.getFdFilePathImg()))
                .externalId(dto.getAtcId())
                .dataSource(LostItem.DataSource.LOST112)
                .status(LostItem.Status.FOUND)
                .owner(null)
                .build();
    }

    private String cleanString(String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) return null;
        return value.trim();
    }

    private String mapCategory(String lost112Category) {
        if (lost112Category == null || lost112Category.trim().isEmpty()) return "기타";

        String category = lost112Category.trim();

        if (category.contains("전자") || category.contains("휴대폰") || category.contains("모바일") || category.contains("노트북") || category.contains("컴퓨터") || category.contains("가전제품")) {
            return "전자기기";
        } else if (category.contains("의류") || category.contains("모자") || category.contains("신발")) {
            return "의류/잡화";
        } else if (category.contains("의료") || category.contains("약") || category.contains("의약품")) {
            return "의료/약품";
        } else if (category.contains("귀금속") || category.contains("시계") || category.contains("장신구") || category.contains("주얼리") || category.contains("보석류")) {
            return "귀중품";
        } else if (category.contains("서적") || category.contains("책")) {
            return "서류/도서";
        } else if (category.contains("카드") || category.contains("교통카드") || category.contains("신용카드")) {
            return "카드/교통";
        } else if (category.contains("가방") || category.contains("지갑")) {
            return "가방/지갑";
        } else if (category.contains("열쇠") || category.contains("키")) {
            return "열쇠";
        } else {
            return "기타";
        }
    }

    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) return null;

        try {
            String cleaned = dateString.trim();
            if (cleaned.length() == 8) return LocalDate.parse(cleaned, DateTimeFormatter.ofPattern("yyyyMMdd"));
            return null;
        } catch (DateTimeParseException e) {
            log.warn("날짜 파싱 실패: {}", dateString);
            return null;
        }
    }

    /** Python 스크립트 실행 옵션을 담는 레코드 */
    private record ImportOptions(String startYmd, String endYmd, String regionCode, int rows, int maxPages, double sleepSec, int concurrency, Map<String, Object> requestMeta) {}

    /** Python 스크립트 실행 결과를 담는 레코드 */
    private record PythonFetchResult(List<Lost112ItemDto> items, Map<String, Object> meta, String rawOutput, String stderrPreview, String itemsPath, int concurrency, long durationMillis) {}

    /** JSON 노드 탐색 결과를 담는 레코드 */
    private record NodeLookupResult(String path, JsonNode node) {}

    /** 외부 프로세스의 스트림 출력 결과를 담는 레코드 */
    private record StreamCapture(String content, boolean truncated) {}

    /** 데이터 임포트 결과를 요약하는 클래스 */
    public static class Lost112ImportResult {
        private final int totalFetched;
        private final int newlyCreated;
        private final int duplicatesSkipped;
        private final Map<String, Object> meta;

        public Lost112ImportResult(int totalFetched, int newlyCreated, int duplicatesSkipped) {
            this(totalFetched, newlyCreated, duplicatesSkipped, Collections.emptyMap());
        }

        public Lost112ImportResult(int totalFetched, int newlyCreated, int duplicatesSkipped, Map<String, Object> meta) {
            this.totalFetched = totalFetched;
            this.newlyCreated = newlyCreated;
            this.duplicatesSkipped = duplicatesSkipped;
            if (meta == null || meta.isEmpty()) {
                this.meta = Collections.emptyMap();
            } else {
                this.meta = Collections.unmodifiableMap(new LinkedHashMap<>(meta));
            }
        }

        public int getTotalFetched() { return totalFetched; }
        public int getNewlyCreated() { return newlyCreated; }
        public int getDuplicatesSkipped() { return duplicatesSkipped; }
        public Map<String, Object> getMeta() { return meta; }

        @Override
        public String toString() {
            String summary = String.format("총 수집: %d건, 신규 저장: %d건, 중복 건너뜀: %d건", totalFetched, newlyCreated, duplicatesSkipped);
            if (!meta.isEmpty()) summary += ", meta=" + meta;
            return summary;
        }
    }
}
