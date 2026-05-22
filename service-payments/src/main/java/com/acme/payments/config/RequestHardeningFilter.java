package com.acme.payments.config;

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
 * <p>For the payments service, overly strict limits are appropriate: the
 * service handles small JSON payloads and there is no legitimate use-case
 * for multi-megabyte bodies or dozens of custom headers.
 *
 * <p>Guidance source: JFrog {@code remediation-guide} MCP tool — CVE-2024-34750,
 * Java/Spring Boot application category.
 */
@Component
@Order(1)
public class RequestHardeningFilter extends OncePerRequestFilter {

    /** Maximum request body: 1 MB — payments payloads are small by design. */
    private static final long MAX_CONTENT_LENGTH_BYTES = 1L * 1024 * 1024;

    /** Maximum headers: 30 — stricter than the API service. */
    private static final int MAX_HEADER_COUNT = 30;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        long contentLength = request.getContentLengthLong();
        if (contentLength > MAX_CONTENT_LENGTH_BYTES) {
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "Request body exceeds the maximum allowed size");
            return;
        }

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
