package com.swp391.eyewear_management_backend.exception;

import com.swp391.eyewear_management_backend.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String MIN_ATTRIBUTE = "min";
    private static final String MAX_ATTRIBUTE = "max";

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse> handleAppException(AppException ex) {
        ErrorCode ec = ex.getErrorCode();
        String message = ex.getMessage() != null ? ex.getMessage() : ec.getMessage();

        ApiResponse body = ApiResponse.builder()
                .code(ec.getCode())
                .message(message)
                .build();

        return ResponseEntity.status(ec.getHttpStatusCode()).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDenied(AccessDeniedException ex) {
        ErrorCode ec = ErrorCode.UNAUTHORIZED;

        ApiResponse body = ApiResponse.builder()
                .code(ec.getCode())
                .message(ec.getMessage())
                .build();

        return ResponseEntity.status(ec.getHttpStatusCode()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();

        ErrorCode ec = ErrorCode.INVALID_KEY;
        Map<String, Object> attrs = null;

        if (fieldError != null) {
            String enumKey = fieldError.getDefaultMessage();
            try {
                ec = ErrorCode.valueOf(enumKey);
            } catch (IllegalArgumentException ignore) {
                log.warn("Unknown validation key: {}", enumKey);
            }
            attrs = extractMinMax(fieldError.getArguments());
        }

        String msg = (attrs != null) ? mapAttribute(ec.getMessage(), attrs) : ec.getMessage();

        ApiResponse body = ApiResponse.builder()
                .code(ec.getCode())
                .message(msg)
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("DataIntegrityViolationException", ex);

        String detail = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : "Database constraint violation";

        ApiResponse body = ApiResponse.builder()
                .code(9999)
                .message(detail)
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiResponse> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        ApiResponse body = ApiResponse.builder()
                .code(406)
                .message("Header Accept phai ho tro application/json")
                .build();

        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(body);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse> handleNotSupported(HttpMediaTypeNotSupportedException ex) {
        ApiResponse body = ApiResponse.builder()
                .code(415)
                .message("Header Content-Type phai la application/json")
                .build();

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
    }

    @ExceptionHandler({ClientAbortException.class, AsyncRequestNotUsableException.class})
    public void handleClientAbort(Exception ex) {
        // Browser refresh, route change, or closed tab can abort the socket while
        // Spring is still flushing JSON. This is not a business failure.
        log.warn("Client disconnected before response completed: {}", ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<ApiResponse> handleMessageNotWritable(HttpMessageNotWritableException ex) {
        // Keep client-abort noise out of the generic 500 handler, but still surface
        // real JSON serialization problems as server errors.
        if (isClientAbort(ex)) {
            log.warn("Response write interrupted because the client disconnected: {}", ex.getMessage());
            return ResponseEntity.noContent().build();
        }

        log.error("HttpMessageNotWritableException", ex);
        ErrorCode ec = ErrorCode.UNCATEGORIZED_EXCEPTION;
        ApiResponse body = ApiResponse.builder()
                .code(ec.getCode())
                .message(ex.getMessage() != null ? ex.getMessage() : ec.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleException(Exception ex) {
        log.error("Unhandled exception", ex);

        ErrorCode ec = ErrorCode.UNCATEGORIZED_EXCEPTION;
        ApiResponse body = ApiResponse.builder()
                .code(ec.getCode())
                .message(ex.getMessage() != null ? ex.getMessage() : ec.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private Map<String, Object> extractMinMax(Object[] args) {
        if (args == null) {
            return null;
        }

        Integer min = null;
        Integer max = null;
        for (Object arg : args) {
            if (arg instanceof Integer integerValue) {
                if (min == null) {
                    min = integerValue;
                } else if (max == null) {
                    max = integerValue;
                }
            }
        }

        if (min == null && max == null) {
            return null;
        }

        Map<String, Object> map = new HashMap<>();
        map.put(MIN_ATTRIBUTE, min);
        map.put(MAX_ATTRIBUTE, max);
        return map;
    }

    private String mapAttribute(String message, Map<String, Object> attributes) {
        String result = message;

        if (attributes.get(MIN_ATTRIBUTE) != null) {
            result = result.replace("{" + MIN_ATTRIBUTE + "}", String.valueOf(attributes.get(MIN_ATTRIBUTE)));
        }
        if (attributes.get(MAX_ATTRIBUTE) != null) {
            result = result.replace("{" + MAX_ATTRIBUTE + "}", String.valueOf(attributes.get(MAX_ATTRIBUTE)));
        }

        return result;
    }

    private boolean isClientAbort(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ClientAbortException || current instanceof AsyncRequestNotUsableException) {
                return true;
            }

            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("connection was aborted")) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}
