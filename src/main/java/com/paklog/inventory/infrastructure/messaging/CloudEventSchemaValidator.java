package com.paklog.inventory.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.paklog.inventory.domain.event.CloudEventType;
import com.paklog.inventory.domain.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Validates domain events against their CloudEvents JSON schemas.
 * Ensures all published events conform to their documented schemas.
 */
@Component
public class CloudEventSchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(CloudEventSchemaValidator.class);
    private static final String SCHEMA_PATH = "classpath:asyncapi/cloudevents/jsonschema/*.json";

    private final Map<String, JsonSchema> schemaCache = new HashMap<>();
    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;

    public CloudEventSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        loadSchemas();
    }

    /**
     * Validate a domain event's data against its JSON schema
     */
    public void validate(DomainEvent event) {
        String eventType = event.getEventType();
        JsonSchema schema = schemaCache.get(eventType);

        if (schema == null) {
            log.warn("No schema found for event type: {}. Skipping validation.", eventType);
            return;
        }

        try {
            // Convert event data to JsonNode
            Map<String, Object> eventData = event.getEventData();
            JsonNode jsonNode = objectMapper.valueToTree(eventData);

            // Validate against schema
            Set<ValidationMessage> errors = schema.validate(jsonNode);

            if (!errors.isEmpty()) {
                String errorMessages = errors.stream()
                        .map(ValidationMessage::getMessage)
                        .reduce((a, b) -> a + "; " + b)
                        .orElse("Unknown validation error");

                throw new CloudEventValidationException(
                        String.format("Event validation failed for type '%s': %s", eventType, errorMessages)
                );
            }

            log.debug("Event validation successful for type: {}", eventType);

        } catch (CloudEventValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating event of type: {}", eventType, e);
            throw new CloudEventValidationException(
                    String.format("Failed to validate event of type '%s'", eventType), e
            );
        }
    }

    /**
     * Load all JSON schemas from classpath on startup
     */
    private void loadSchemas() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(SCHEMA_PATH);

            log.info("Loading {} CloudEvent schemas from path: {}", resources.length, SCHEMA_PATH);

            for (Resource resource : resources) {
                try (InputStream inputStream = resource.getInputStream()) {
                    JsonNode schemaNode = objectMapper.readTree(inputStream);
                    JsonNode idNode = schemaNode.get("$id");

                    if (idNode == null) {
                        log.warn("Schema file {} missing $id field, skipping", resource.getFilename());
                        continue;
                    }

                    String schemaId = idNode.asText();

                    // Create schema without strict URI validation for $id
                    JsonSchema schema = schemaFactory.getSchema(schemaNode);
                    schemaCache.put(schemaId, schema);

                    log.info("Loaded schema: {} from {}", schemaId, resource.getFilename());
                } catch (com.networknt.schema.InvalidSchemaException e) {
                    // If $id is not a valid URI, log warning and skip validation for this schema
                    log.warn("Schema file {} has invalid $id format, validation will be skipped: {}",
                            resource.getFilename(), e.getMessage());
                } catch (Exception e) {
                    log.error("Failed to load schema from: {}", resource.getFilename(), e);
                }
            }

            log.info("Successfully loaded {} CloudEvent schemas", schemaCache.size());

        } catch (Exception e) {
            log.error("Failed to load CloudEvent schemas from path: {}", SCHEMA_PATH, e);
            // Don't throw exception - allow application to start even if schemas aren't loaded
            // This is important for testing environments
            log.warn("CloudEvent schema validation will be skipped if schemas failed to load");
        }
    }

    /**
     * Check if a schema exists for the given event type
     */
    public boolean hasSchema(String eventType) {
        return schemaCache.containsKey(eventType);
    }

    /**
     * Get the number of loaded schemas
     */
    public int getSchemaCount() {
        return schemaCache.size();
    }

    /**
     * Exception thrown when CloudEvent validation fails
     */
    public static class CloudEventValidationException extends RuntimeException {
        public CloudEventValidationException(String message) {
            super(message);
        }

        public CloudEventValidationException(String message, Throwable cause) {
            super(message, cause);
        

}
}
}
