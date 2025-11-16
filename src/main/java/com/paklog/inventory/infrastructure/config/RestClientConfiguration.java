package com.paklog.inventory.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for REST client components.
 * Configures RestTemplate with proper timeouts and error handling.
 */
@Configuration
public class RestClientConfiguration {

    @Value("${product-catalog.connection.timeout:5000}")
    private int connectionTimeout;

    @Value("${product-catalog.read.timeout:10000}")
    private int readTimeout;

    /**
     * Creates a RestTemplate bean configured with appropriate timeouts.
     * Uses the application's ObjectMapper for consistent JSON serialization.
     *
     * @param builder RestTemplateBuilder provided by Spring Boot
     * @param objectMapper The application's configured ObjectMapper
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(connectionTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();

        // Configure RestTemplate to use the application's ObjectMapper
        restTemplate.getMessageConverters().stream()
                .filter(converter -> converter instanceof MappingJackson2HttpMessageConverter)
                .map(converter -> (MappingJackson2HttpMessageConverter) converter)
                .forEach(converter -> converter.setObjectMapper(objectMapper));

        return restTemplate;
    }
}
