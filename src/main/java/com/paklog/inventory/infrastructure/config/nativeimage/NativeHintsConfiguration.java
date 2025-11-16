package com.paklog.inventory.infrastructure.config.nativeimage;

import com.paklog.inventory.application.dto.*;
import com.paklog.inventory.domain.event.StockLevelChangedEvent;
import com.paklog.inventory.domain.model.StockLevel;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.v1.CloudEventV1;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * GraalVM Native Image runtime hints configuration.
 *
 * This class registers reflection, serialization, and resource hints
 * for classes that need to be accessible at runtime in native images.
 * Spring AOT (Ahead-of-Time) processing uses these hints during
 * native image compilation.
 */
@Configuration
@ImportRuntimeHints(NativeHintsConfiguration.InventoryRuntimeHints.class)
public class NativeHintsConfiguration {

    static class InventoryRuntimeHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // Register DTO classes for reflection (JSON serialization/deserialization)
            registerDtoHints(hints);

            // Register domain model classes
            registerDomainHints(hints);

            // Register CloudEvents classes
            registerCloudEventsHints(hints);

            // Register resources
            registerResourceHints(hints);
        }

        private void registerDtoHints(RuntimeHints hints) {
            // Application DTOs
            hints.reflection().registerType(StockLevelResponse.class,
                    builder -> builder
                            .withMembers(
                                    org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS,
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS
                            )
            );

            hints.reflection().registerType(BulkAllocationResponse.class,
                    builder -> builder
                            .withMembers(
                                    org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS,
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS
                            )
            );

            hints.reflection().registerType(AllocationResult.class,
                    builder -> builder
                            .withMembers(
                                    org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS,
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS
                            )
            );

            hints.reflection().registerType(AllocationRequestItem.class,
                    builder -> builder
                            .withMembers(
                                    org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS,
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS
                            )
            );

            hints.reflection().registerType(UpdateStockLevelRequest.class,
                    builder -> builder
                            .withMembers(
                                    org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS,
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS
                            )
            );

            hints.reflection().registerType(CreateReservationRequest.class,
                    builder -> builder
                            .withMembers(
                                    org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS,
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS
                            )
            );

            hints.reflection().registerType(InventoryHealthMetricsResponse.class,
                    builder -> builder
                            .withMembers(
                                    org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS,
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS
                            )
            );
        }

        private void registerDomainHints(RuntimeHints hints) {
            hints.reflection().registerType(StockLevel.class,
                    builder -> builder
                            .withMembers(
                                    org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS,
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS
                            )
            );

            hints.reflection().registerType(StockLevelChangedEvent.class,
                    builder -> builder
                            .withMembers(
                                    org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS,
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS
                            )
            );
        }

        private void registerCloudEventsHints(RuntimeHints hints) {
            hints.reflection().registerType(CloudEvent.class,
                    builder -> builder
                            .withMembers(
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS
                            )
            );

            hints.reflection().registerType(CloudEventV1.class,
                    builder -> builder
                            .withMembers(
                                    org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS,
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS
                            )
            );
        }

        private void registerResourceHints(RuntimeHints hints) {
            // Application configuration files
            hints.resources().registerPattern("application.yml");
            hints.resources().registerPattern("application.yaml");
            hints.resources().registerPattern("application.properties");
            hints.resources().registerPattern("application-*.yml");
            hints.resources().registerPattern("application-*.yaml");
            hints.resources().registerPattern("application-*.properties");

            // Logging configuration
            hints.resources().registerPattern("logback-spring.xml");
            hints.resources().registerPattern("logback.xml");

            // AsyncAPI and JSON schemas
            hints.resources().registerPattern("asyncapi/**");
            hints.resources().registerPattern("META-INF/**");
        }
    }
}
