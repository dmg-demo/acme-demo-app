package com.acme.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;

/**
 * Servlet filter that enforces request-level resource limits as a
 * defence-in-depth measure alongside the CVE-2024-34750 Tomcat upgrade.
 *
 * <p>CVE-2024-34750 is exploited at the HTTP/2 framing layer, not at the
 * application (servlet) layer. However, capping request size and header count
 * at the application boundary provides additional protection against related
 * resource-exhaustion patterns and is a recommended hardening step from the
 * JFrog {@code remediation-guide} MCP tool for this CVE.
 *
 * <p>Ordering: {@code @Order(1)} ensures this runs before all other filters so
 * that oversized or malformed requests are rejected as early as possible.
 */
@Component
@Order(1)
public class RequestHardeningFilter extends OncePerRequestFilter {

    /** Maximum request body size: 10 MB.  Adjust per API contract requirements. */
    private static final long MAX_CONTENT_LENGTH_BYTES = 10L * 1024 * 1024;

    /** Maximum number of HTTP request headers.  RFC 9114 recommends ≤ 100. */
    private static final int MAX_HEADER_COUNT = 50;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // ── 1. Reject oversized bodies before Tomcat buffers the entire stream ──
        long contentLength = request.getContentLengthLong();
        if (contentLength > MAX_CONTENT_LENGTH_BYTES) {
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "Request body exceeds the maximum allowed size");
            return;
        }

        // ── 2. Reject requests with an abnormally large number of headers ───────
        // (a sign of header-based resource exhaustion attacks)
        int headerCount = 0;
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            headerNames.nextElement();
            if (++headerCount > MAX_HEADER_COUNT) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Too many request headers");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
