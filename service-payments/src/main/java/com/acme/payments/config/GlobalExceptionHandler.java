package com.acme.payments.config;

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
 * <p>This is especially important for the payments service: any leakage of
 * server internals (Tomcat version, stack traces) could help an attacker
 * confirm whether CVE-2024-34750 remains exploitable.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status",    500,
                        "error",     "Internal Server Error"
                ));
    }
}
