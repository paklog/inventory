package com.paklog.inventory.infrastructure.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark API endpoints as deprecated with deprecation and sunset dates.
 * Automatically adds deprecation headers to responses.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiDeprecated {

    /**
     * Date when the API was deprecated (ISO 8601 format)
     * Example: "2025-10-01"
     */
    String deprecationDate();

    /**
     * Date when the API will be removed/sunset (ISO 8601 format)
     * Example: "2026-04-01"
     */
    String sunsetDate();

    /**
     * URL to migration guide or alternative API documentation
     */
    String migrationGuide() default "";

    /**
     * Version that replaces this deprecated API
     */
    String replacedBy() default "";

    /**
     * Reason for deprecation
     */
    String reason() default "";
}
