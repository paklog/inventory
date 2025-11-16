package com.paklog.inventory.infrastructure.observability;

import com.paklog.inventory.domain.repository.ProductStockRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Custom health indicator for inventory service.
 * Checks the availability of critical dependencies.
 */
@Component
public class InventoryHealthIndicator implements HealthIndicator {

    private final ProductStockRepository productStockRepository;

    public InventoryHealthIndicator(ProductStockRepository productStockRepository) {
        this.productStockRepository = productStockRepository;
    }

    @Override
    public Health health() {
        try {
            // Check if we can query the database
            List<String> skus = productStockRepository.findAllSkus();
            long count = skus.size();

            return Health.up()
                    .withDetail("productCount", count)
                    .withDetail("database", "mongodb")
                    .withDetail("status", "operational")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("database", "mongodb")
                    .withDetail("status", "unavailable")
                    .build();
        

}
}
}
