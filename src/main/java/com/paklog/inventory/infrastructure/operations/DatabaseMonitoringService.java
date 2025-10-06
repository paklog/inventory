package com.paklog.inventory.infrastructure.operations;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitors MongoDB health and performance metrics.
 * Exposes metrics via Micrometer for Prometheus/Grafana.
 */
@Service
public class DatabaseMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMonitoringService.class);

    private final MongoTemplate mongoTemplate;
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong availableConnections = new AtomicLong(0);
    private final AtomicLong databaseSize = new AtomicLong(0);
    private final AtomicLong collectionCount = new AtomicLong(0);
    private final AtomicLong indexCount = new AtomicLong(0);
    private final AtomicLong slowQueryCount = new AtomicLong(0);
    private final AtomicLong unpublishedEvents = new AtomicLong(0);

    public DatabaseMonitoringService(MongoTemplate mongoTemplate, MeterRegistry meterRegistry) {
        this.mongoTemplate = mongoTemplate;

        // Register gauges for MongoDB metrics
        Gauge.builder("mongodb.connections.active", activeConnections, AtomicLong::get)
                .description("Number of active MongoDB connections")
                .register(meterRegistry);

        Gauge.builder("mongodb.connections.available", availableConnections, AtomicLong::get)
                .description("Number of available MongoDB connections")
                .register(meterRegistry);

        Gauge.builder("mongodb.database.size.bytes", databaseSize, AtomicLong::get)
                .description("Total database size in bytes")
                .register(meterRegistry);

        Gauge.builder("mongodb.collections.count", collectionCount, AtomicLong::get)
                .description("Number of collections in database")
                .register(meterRegistry);

        Gauge.builder("mongodb.indexes.count", indexCount, AtomicLong::get)
                .description("Total number of indexes")
                .register(meterRegistry);

        Gauge.builder("mongodb.queries.slow.count", slowQueryCount, AtomicLong::get)
                .description("Number of slow queries (>100ms)")
                .register(meterRegistry);

        Gauge.builder("inventory.outbox.unpublished.count", unpublishedEvents, AtomicLong::get)
                .description("Number of unpublished outbox events")
                .register(meterRegistry);
    }

    /**
     * Collect MongoDB metrics every 30 seconds
     */
    @Scheduled(fixedRate = 30000)
    public void collectMetrics() {
        try {
            collectConnectionMetrics();
            collectDatabaseMetrics();
            collectPerformanceMetrics();
            collectBusinessMetrics();
        } catch (Exception e) {
            log.error("Failed to collect MongoDB metrics", e);
        }
    }

    private void collectConnectionMetrics() {
        try {
            Document serverStatus = mongoTemplate.getDb()
                    .runCommand(new Document("serverStatus", 1));

            Document connections = serverStatus.get("connections", Document.class);
            if (connections != null) {
                activeConnections.set(connections.getInteger("current", 0));
                availableConnections.set(connections.getInteger("available", 0));

                log.debug("MongoDB connections - active: {}, available: {}",
                        activeConnections.get(), availableConnections.get());

                // Alert if connection pool is near exhaustion
                if (availableConnections.get() < 10) {
                    log.warn("MongoDB connection pool nearly exhausted! Available: {}",
                            availableConnections.get());
                }
            }
        } catch (Exception e) {
            log.error("Failed to collect connection metrics", e);
        }
    }

    private void collectDatabaseMetrics() {
        try {
            Document dbStats = mongoTemplate.getDb()
                    .runCommand(new Document("dbStats", 1).append("scale", 1));

            databaseSize.set(dbStats.get("dataSize") != null ? ((Number) dbStats.get("dataSize")).longValue() : 0L);
            collectionCount.set(dbStats.get("collections") != null ? ((Number) dbStats.get("collections")).intValue() : 0);

            log.debug("MongoDB stats - size: {} bytes, collections: {}",
                    databaseSize.get(), collectionCount.get());

            // Count total indexes across all collections
            long totalIndexes = mongoTemplate.getCollectionNames().stream()
                    .mapToLong(collName -> {
                        try {
                            return mongoTemplate.getCollection(collName).listIndexes().into(new java.util.ArrayList<>()).size();
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .sum();

            indexCount.set(totalIndexes);

        } catch (Exception e) {
            log.error("Failed to collect database metrics", e);
        }
    }

    private void collectPerformanceMetrics() {
        try {
            // Check for slow queries in profiler (if enabled)
            long slowQueries = mongoTemplate.getCollection("system.profile")
                    .countDocuments(new Document("millis", new Document("$gte", 100)));

            slowQueryCount.set(slowQueries);

            if (slowQueries > 100) {
                log.warn("High number of slow queries detected: {}", slowQueries);
            }

        } catch (Exception e) {
            // Profile collection may not exist if profiling is disabled
            log.debug("Profiling not enabled or collection not accessible");
        }
    }

    private void collectBusinessMetrics() {
        try {
            // Count unpublished outbox events
            long unpublished = mongoTemplate.getCollection("outbox_events")
                    .countDocuments(new Document("published", false));

            unpublishedEvents.set(unpublished);

            // Alert if outbox is backing up
            if (unpublished > 1000) {
                log.warn("High number of unpublished events in outbox: {}", unpublished);
            }

            if (unpublished > 10000) {
                log.error("CRITICAL: Outbox severely backed up with {} unpublished events", unpublished);
            }

        } catch (Exception e) {
            log.error("Failed to collect business metrics", e);
        }
    }

    /**
     * Check database health
     */
    public DatabaseHealthStatus checkHealth() {
        try {
            // Ping database
            Document result = mongoTemplate.getDb()
                    .runCommand(new Document("ping", 1));

            if (result.getDouble("ok") == 1.0) {
                return new DatabaseHealthStatus(
                        true,
                        "MongoDB is healthy",
                        activeConnections.get(),
                        availableConnections.get(),
                        databaseSize.get()
                );
            } else {
                return new DatabaseHealthStatus(
                        false,
                        "MongoDB ping failed",
                        0, 0, 0
                );
            }

        } catch (Exception e) {
            log.error("Database health check failed", e);
            return new DatabaseHealthStatus(
                    false,
                    "Database unreachable: " + e.getMessage(),
                    0, 0, 0
            );
        }
    }

    /**
     * Database health status DTO
     */
    public record DatabaseHealthStatus(
            boolean healthy,
            String message,
            long activeConnections,
            long availableConnections,
            long databaseSizeBytes
    ) {}
}
