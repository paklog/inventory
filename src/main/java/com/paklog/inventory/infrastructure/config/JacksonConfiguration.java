package com.paklog.inventory.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson configuration for consistent API serialization.
 *
 * Enforces snake_case naming convention for all JSON serialization/deserialization
 * across both REST APIs (OpenAPI) and event-driven APIs (AsyncAPI/CloudEvents).
 *
 * This aligns with industry standards from:
 * - Google JSON Style Guide
 * - Shopify API conventions
 * - Stripe API conventions
 * - CloudEvents specification
 */
@Configuration
public class JacksonConfiguration {

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();

        // Set snake_case as the global naming strategy for all JSON serialization
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        // Register Java 8 Date/Time module for proper LocalDateTime serialization
        objectMapper.registerModule(new JavaTimeModule());

        // Write dates as ISO-8601 strings instead of timestamps
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }
}
