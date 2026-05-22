package com.acme.api.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.Map;

/**
 * Global exception handler that prevents internal Tomcat/Spring error details
 * from leaking to clients — a complementary hardening measure recommended by
 * the JFrog {@code remediation-guide} MCP tool for CVE-2024-34750.
 *
 * <p>Stack traces and internal error messages must never be surfaced in HTTP
 * responses: they can leak server version information and assist an attacker
 * in confirming whether the patch is applied.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex, WebRequest request) {
        // Intentionally generic — do NOT include ex.getMessage() as it may
        // expose internal state (Tomcat version, class names, file paths).
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status",    500,
                        "error",     "Internal Server Error"
                ));
    }
}
