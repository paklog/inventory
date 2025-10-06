package com.paklog.inventory.infrastructure.observability;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Kafka producer interceptor that adds correlation ID to outgoing messages.
 * This allows correlation IDs to be propagated across service boundaries via Kafka.
 */
public class CorrelationIdKafkaInterceptor implements ProducerInterceptor<String, Object> {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
        // Get correlation ID from MDC
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);

        if (correlationId != null) {
            // Add correlation ID to Kafka message headers
            Headers headers = record.headers();
            headers.add(CORRELATION_ID_HEADER, correlationId.getBytes(StandardCharsets.UTF_8));
        }

        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // No action needed on acknowledgement
    }

    @Override
    public void close() {
        // No resources to clean up
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // No configuration needed
    }
}
