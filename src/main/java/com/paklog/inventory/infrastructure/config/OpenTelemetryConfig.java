package com.paklog.inventory.infrastructure.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenTelemetry to ensure proper lifecycle management.
 * This prevents memory leaks by properly shutting down the BatchSpanProcessor
 * and other OpenTelemetry components when the application context is destroyed.
 */
@Configuration
public class OpenTelemetryConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenTelemetryConfig.class);

    @Autowired(required = false)
    private OpenTelemetry openTelemetry;

    /**
     * Properly shutdown OpenTelemetry SDK when the application context is destroyed.
     * This ensures that the BatchSpanProcessor worker thread is stopped gracefully,
     * preventing the "failed to stop thread" warning.
     */
    @PreDestroy
    public void shutdown() {
        if (openTelemetry instanceof OpenTelemetrySdk) {
            log.info("Shutting down OpenTelemetry SDK...");
            OpenTelemetrySdk sdk = (OpenTelemetrySdk) openTelemetry;
            sdk.close();
            log.info("OpenTelemetry SDK shutdown complete");
        }
    }
}
