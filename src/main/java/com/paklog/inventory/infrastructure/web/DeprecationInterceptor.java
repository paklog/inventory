package com.paklog.inventory.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Interceptor that adds deprecation headers to responses for deprecated API endpoints.
 * Implements standard deprecation headers as per RFC 8594.
 */
@Component
public class DeprecationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(DeprecationInterceptor.class);
    private static final DateTimeFormatter HTTP_DATE_FORMATTER =
            DateTimeFormatter.RFC_1123_DATE_TIME;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        ApiDeprecated deprecation = handlerMethod.getMethodAnnotation(ApiDeprecated.class);
        if (deprecation == null) {
            deprecation = handlerMethod.getBeanType().getAnnotation(ApiDeprecated.class);
        }

        if (deprecation != null) {
            addDeprecationHeaders(response, deprecation);
            logDeprecationWarning(request, deprecation);
        }

        return true;
    }

    private void addDeprecationHeaders(HttpServletResponse response, ApiDeprecated deprecation) {
        // Add Deprecation header (RFC 8594)
        String deprecationDate = formatDateForHeader(deprecation.deprecationDate());
        response.setHeader("Deprecation", deprecationDate);

        // Add Sunset header
        String sunsetDate = formatDateForHeader(deprecation.sunsetDate());
        response.setHeader("Sunset", sunsetDate);

        // Add Link header for migration guide if provided
        if (!deprecation.migrationGuide().isEmpty()) {
            String linkHeader = String.format("<%s>; rel=\"deprecation\"", deprecation.migrationGuide());
            response.setHeader("Link", linkHeader);
        }

        // Add custom headers for additional context
        if (!deprecation.replacedBy().isEmpty()) {
            response.setHeader("X-API-Replaced-By", deprecation.replacedBy());
        }

        // Add warning header (RFC 7234)
        String warning = String.format(
                "299 - \"This API endpoint is deprecated and will be removed on %s\"",
                deprecation.sunsetDate()
        );
        response.setHeader("Warning", warning);
    }

    private String formatDateForHeader(String isoDate) {
        try {
            LocalDate date = LocalDate.parse(isoDate);
            return date.atStartOfDay(ZoneId.of("GMT"))
                    .format(HTTP_DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("Failed to parse deprecation date: {}", isoDate, e);
            return isoDate;
        }
    }

    private void logDeprecationWarning(HttpServletRequest request, ApiDeprecated deprecation) {
        log.warn("Deprecated API called: {} {} - Deprecated: {}, Sunset: {}, Reason: {}",
                request.getMethod(),
                request.getRequestURI(),
                deprecation.deprecationDate(),
                deprecation.sunsetDate(),
                deprecation.reason());
    }
}
