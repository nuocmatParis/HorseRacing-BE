package com.swp391.horseracing.exception;

import com.swp391.horseracing.dto.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException exception){
        ErrorCode errorCode = exception.getErrorCode();

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(exception.getMessage())
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
    public ResponseEntity<ApiResponse<List<ValidationErrorResponse>>> handleValidationException(MethodArgumentNotValidException exception){
        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;

        List<ValidationErrorResponse> validationErrorResponses = new ArrayList<>();

        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();

        for(FieldError fieldError : fieldErrors){
            ValidationErrorResponse error = ValidationErrorResponse.builder()
                    .field(fieldError.getField())
                    .message(fieldError.getDefaultMessage())
                    .build();

            validationErrorResponses.add(error);
        }

        ApiResponse<List<ValidationErrorResponse>> apiResponse = ApiResponse
                .<List<ValidationErrorResponse>>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .result(validationErrorResponses)
                .build();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(apiResponse);
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadableException(
            HttpMessageNotReadableException exception){
        ErrorCode errorCode = ErrorCode.ROLE_NOT_FOUND;

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(apiResponse);
    }

    // Bắt 401
    @ExceptionHandler(value = AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException exception){
        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(apiResponse);
    }

    // Bắt 403
    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException exception){
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(apiResponse);
    }

}