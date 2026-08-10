package com.enterprise.aiassistant.backend.common.exception;

import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDto> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {

        log.error("Unhandled exception", exception);

        ErrorCode errorCode = exception.getErrorCode();

        ErrorResponseDto response = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .error(errorCode.name())
                .message(exception.getMessage())
                .path(request.getRequestURI())
                .build();


        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }

    // @Valid @RequestBody (vd: CreateFolderRequest, RenameFolderRequest, SendMessageRequest...)
    // thất bại (@NotBlank, @NotNull, @Size, @AssertTrue, ...) — trước đây rơi vào handler
    // Exception.class chung và trả 500, giờ trả đúng 400 kèm danh sách lỗi từng field.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        log.warn("Validation failed: {}", exception.getMessage());

        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Validation failed";
        }

        ErrorResponseDto response = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_FAILED")
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // @Validated trên @RequestParam / @PathVariable (vd: @Min, @NotBlank ở mức method param).
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {

        log.warn("Constraint violation: {}", exception.getMessage());

        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));

        ErrorResponseDto response = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_FAILED")
                .message(message.isBlank() ? "Validation failed" : message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Thiếu @RequestParam bắt buộc (vd: GET /folders/search không truyền keyword).
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingParam(
            MissingServletRequestParameterException exception,
            HttpServletRequest request
    ) {

        log.warn("Missing request parameter: {}", exception.getMessage());

        ErrorResponseDto response = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("MISSING_PARAMETER")
                .message("Required parameter '" + exception.getParameterName() + "' is missing")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Thiếu @RequestPart bắt buộc (vd: POST /documents/upload thiếu part "request").
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingPart(
            MissingServletRequestPartException exception,
            HttpServletRequest request
    ) {

        log.warn("Missing request part: {}", exception.getMessage());

        ErrorResponseDto response = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("MISSING_PART")
                .message("Required part '" + exception.getRequestPartName() + "' is missing")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Sai kiểu path variable / query param (vd: GET /folders/abc thay vì /folders/1).
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {

        log.warn("Type mismatch: {}", exception.getMessage());

        String requiredType = exception.getRequiredType() != null
                ? exception.getRequiredType().getSimpleName()
                : "expected type";

        ErrorResponseDto response = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("TYPE_MISMATCH")
                .message("Parameter '" + exception.getName() + "' must be of type " + requiredType)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnknownException(
            Exception exception,
            HttpServletRequest request

    ) {
        log.error("Unhandled exception", exception);


        ErrorResponseDto response =
                ErrorResponseDto.builder()
                        .timestamp(LocalDateTime.now())
                        .status(500)
                        .error("INTERNAL_SERVER_ERROR")
                        .message("Something went wrong")
                        .path(request.getRequestURI())
                        .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
