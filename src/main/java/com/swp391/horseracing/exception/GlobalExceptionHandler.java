package com.swp391.horseracing.exception;

import com.swp391.horseracing.dto.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException exception){
        ErrorCode errorCode = exception.getErrorCode();

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(apiResponse);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception exception){
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(apiResponse);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleValidationException(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.<List<String>>builder()
                        .code(ErrorCode.INVALID_REQUEST.getCode())
                        .message("Validation failed")
                        .result(errors)
                        .build());
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> handleMessageNotReadableException(
            HttpMessageNotReadableException exception){
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;

        String detail = "Invalid request format. Please check your input data.";

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.<String>builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .result(detail)
                        .build());
    }

}
