package com.paklog.inventory.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Multi-tier caching configuration for inventory service.
 * Implements L1 (Caffeine) and L2 (Redis) caching strategy.
 */
@Configuration
@EnableCaching
public class CacheConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);

    // Cache names
    public static final String PRODUCT_STOCK_CACHE = "productStock";
    public static final String SKU_LIST_CACHE = "skuList";
    public static final String ABC_CLASSIFICATION_CACHE = "abcClassification";
    public static final String INVENTORY_LEDGER_CACHE = "inventoryLedger";
    public static final String STOCK_LOCATION_CACHE = "stockLocation";

    /**
     * L1 Cache: Caffeine (In-Memory, Low Latency)
     * Used for frequently accessed, small datasets
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        log.info("Configuring Caffeine L1 cache manager");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                PRODUCT_STOCK_CACHE,
                SKU_LIST_CACHE,
                ABC_CLASSIFICATION_CACHE,
                STOCK_LOCATION_CACHE
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000) // Max 10K entries
                .expireAfterWrite(5, TimeUnit.MINUTES) // 5 min TTL
                .expireAfterAccess(2, TimeUnit.MINUTES) // 2 min idle
                .recordStats() // Enable cache statistics
        );

        log.info("Caffeine cache configured: max=10000, writeExpiry=5min, accessExpiry=2min");
        return cacheManager;
    }

    /**
     * L2 Cache: Redis (Distributed, Shared)
     * Used for larger datasets, cross-instance caching
     */
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Configuring Redis L2 cache manager");

        // Default configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // 10 min default TTL
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()
                        )
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()
                        )
                )
                .disableCachingNullValues();

        // Cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // Product stock: 5 min TTL (frequently updated)
        cacheConfigs.put(PRODUCT_STOCK_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // SKU list: 30 min TTL (rarely changes)
        cacheConfigs.put(SKU_LIST_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // ABC classification: 1 hour TTL (changes infrequently)
        cacheConfigs.put(ABC_CLASSIFICATION_CACHE,
                defaultConfig.entryTtl(Duration.ofHours(1)));

        // Inventory ledger: 15 min TTL (append-only, can cache longer)
        cacheConfigs.put(INVENTORY_LEDGER_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Stock locations: 30 min TTL (rarely changes)
        cacheConfigs.put(STOCK_LOCATION_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();

        log.info("Redis cache configured with {} cache definitions", cacheConfigs.size());
        return cacheManager;
    }
}
