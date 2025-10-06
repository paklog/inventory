package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.connection.ConnectionPoolSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

import java.util.concurrent.TimeUnit;

/**
 * MongoDB connection pool configuration for optimal performance.
 * Configures connection pool size, timeouts, and monitoring.
 */
@Configuration
public class MongoConnectionPoolConfiguration extends AbstractMongoClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MongoConnectionPoolConfiguration.class);

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.pool.min-size:10}")
    private int minConnectionPoolSize;

    @Value("${spring.data.mongodb.pool.max-size:100}")
    private int maxConnectionPoolSize;

    @Value("${spring.data.mongodb.pool.max-wait-time-ms:5000}")
    private long maxWaitTimeMs;

    @Value("${spring.data.mongodb.pool.max-connection-idle-time-ms:60000}")
    private long maxConnectionIdleTimeMs;

    @Value("${spring.data.mongodb.pool.max-connection-life-time-ms:1800000}")
    private long maxConnectionLifeTimeMs;

    @Value("${spring.data.mongodb.pool.maintenance-frequency-ms:10000}")
    private long maintenanceFrequencyMs;

    @Override
    protected String getDatabaseName() {
        ConnectionString connectionString = new ConnectionString(mongoUri);
        return connectionString.getDatabase();
    }

    @Override
    protected void configureClientSettings(MongoClientSettings.Builder builder) {
        log.info("Configuring MongoDB connection pool with settings:");
        log.info("  - Min pool size: {}", minConnectionPoolSize);
        log.info("  - Max pool size: {}", maxConnectionPoolSize);
        log.info("  - Max wait time: {}ms", maxWaitTimeMs);
        log.info("  - Max idle time: {}ms", maxConnectionIdleTimeMs);
        log.info("  - Max lifetime: {}ms", maxConnectionLifeTimeMs);

        ConnectionPoolSettings poolSettings = ConnectionPoolSettings.builder()
                .minSize(minConnectionPoolSize)
                .maxSize(maxConnectionPoolSize)
                .maxWaitTime(maxWaitTimeMs, TimeUnit.MILLISECONDS)
                .maxConnectionIdleTime(maxConnectionIdleTimeMs, TimeUnit.MILLISECONDS)
                .maxConnectionLifeTime(maxConnectionLifeTimeMs, TimeUnit.MILLISECONDS)
                .maintenanceFrequency(maintenanceFrequencyMs, TimeUnit.MILLISECONDS)
                // Enable connection pool monitoring
                .addConnectionPoolListener(new MongoConnectionPoolMonitor())
                .build();

        builder.applyConnectionString(new ConnectionString(mongoUri))
                .applyToConnectionPoolSettings(poolBuilder ->
                    poolBuilder
                        .minSize(poolSettings.getMinSize())
                        .maxSize(poolSettings.getMaxSize())
                        .maxWaitTime(poolSettings.getMaxWaitTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
                        .maxConnectionIdleTime(poolSettings.getMaxConnectionIdleTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
                        .maxConnectionLifeTime(poolSettings.getMaxConnectionLifeTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
                        .maintenanceFrequency(poolSettings.getMaintenanceFrequency(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
                        .addConnectionPoolListener(poolSettings.getConnectionPoolListeners().get(0))
                );

        log.info("MongoDB connection pool configured successfully");
    }
}
