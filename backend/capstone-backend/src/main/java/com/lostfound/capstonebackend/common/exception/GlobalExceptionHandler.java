package com.lostfound.capstonebackend.common.exception;

import com.lostfound.capstonebackend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 애플리케이션 전역에서 발생하는 예외를 처리하는 클래스입니다.
 * {@link RestControllerAdvice}를 사용하여 모든 컨트롤러에서 발생하는 예외를 가로채
 * 일관된 형식의 API 에러 응답을 생성합니다.
 */
@Hidden // Swagger 문서에 표시되지 않도록 설정
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 직접 정의한 비즈니스 예외({@link BusinessException})를 처리합니다.
     * @param e 발생한 BusinessException
     * @return ErrorCode에 정의된 상태 코드와 메시지를 담은 ResponseEntity
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode ec = e.getErrorCode();
        log.warn("BusinessException: {} - {}", ec.getCode(), e.getMessage());
        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.fail(ec.getCode(), e.getMessage()));
    }

    /**
     * {@code @Valid} 애노테이션을 사용한 DTO의 필드 유효성 검증 실패 시 발생하는 예외를 처리합니다.
     * @param e 발생한 MethodArgumentNotValidException
     * @return 400 Bad Request와 함께 각 필드의 검증 실패 메시지를 담은 ResponseEntity
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        Map<String, Object> details = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fe ->
                details.put(fe.getField(), fe.getDefaultMessage())
        );
        log.warn("MethodArgumentNotValidException: {}", details);
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE.getCode(), "입력값 검증에 실패했습니다.", details));
    }

    /**
     * 경로 변수(PathVariable)나 요청 파라미터(RequestParam)의 제약 조건 위반 시 발생하는 예외를 처리합니다.
     * @param e 발생한 ConstraintViolationException
     * @return 400 Bad Request와 함께 제약 조건 위반 정보를 담은 ResponseEntity
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        Map<String, Object> details = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(v ->
                details.put(v.getPropertyPath().toString(), v.getMessage())
        );
        log.warn("ConstraintViolationException: {}", details);
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE.getCode(), "요청 파라미터 검증에 실패했습니다.", details));
    }

    /**
     * 요청 본문(RequestBody)의 JSON 형식이 잘못되었을 때 발생하는 예외를 처리합니다.
     * @param e 발생한 HttpMessageNotReadableException
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE.getCode(), "JSON 파싱 오류: 요청 본문의 형식을 확인하세요."));
    }

    /**
     * 요청 파라미터의 타입이 일치하지 않을 때 발생하는 예외를 처리합니다.
     * @param e 발생한 MethodArgumentTypeMismatchException
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("MethodArgumentTypeMismatchException: 파라미터 '{}'에 잘못된 값 '{}'가 입력되었습니다.", e.getName(), e.getValue());
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE.getCode(), String.format("'%s' 파라미터의 타입이 올바르지 않습니다.", e.getName())));
    }

    /**
     * 데이터베이스 제약 조건(예: Unique 키 중복) 위반 시 발생하는 예외를 처리합니다.
     * @param e 발생한 DataIntegrityViolationException
     * @return 409 Conflict 응답
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException e) {
        log.error("DataIntegrityViolationException: {}", e.getMessage(), e);
        String detailedMessage = e.getMessage();
        if (e.getCause() != null) {
            detailedMessage += " | Cause: " + e.getCause().getMessage();
        }
        return ResponseEntity.status(ErrorCode.DUPLICATE_RESOURCE.getStatus())
                .body(ApiResponse.fail(ErrorCode.DUPLICATE_RESOURCE.getCode(), 
                    "데이터 제약조건 위반: " + detailedMessage));
    }

    /**
     * 위에서 처리되지 않은 모든 예외를 처리하는 최종 핸들러입니다.
     * @param e 발생한 예외
     * @return 500 Internal Server Error와 함께 서버 내부 오류 메시지를 담은 ResponseEntity
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleEtc(Exception e) {
        log.error("Unhandled exception: {}", e.getClass().getSimpleName(), e);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getDefaultMessage()));
    }
}
