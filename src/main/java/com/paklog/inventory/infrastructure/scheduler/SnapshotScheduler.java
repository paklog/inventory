package com.paklog.inventory.infrastructure.scheduler;

import com.paklog.inventory.application.service.SnapshotService;
import com.paklog.inventory.domain.model.SnapshotReason;
import com.paklog.inventory.domain.model.SnapshotType;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Scheduled jobs for automated inventory snapshots.
 * Implements retention policy with daily, monthly, quarterly, and yearly snapshots.
 */
@Component
public class SnapshotScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SnapshotScheduler.class);

    private final SnapshotService snapshotService;
    private final ProductStockRepository productStockRepository;

    public SnapshotScheduler(SnapshotService snapshotService,
                            ProductStockRepository productStockRepository) {
        this.snapshotService = snapshotService;
        this.productStockRepository = productStockRepository;
    }

    /**
     * Create daily snapshots at midnight
     * Runs every day at 00:00:00
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void createDailySnapshots() {
        logger.info("Starting daily snapshot job");

        long startTime = System.currentTimeMillis();
        int snapshotCount = 0;

        try {
            // Get all active SKUs
            var allStock = productStockRepository.findAll();

            for (var stock : allStock) {
                try {
                    snapshotService.createSnapshot(
                        stock.getSku(),
                        SnapshotType.DAILY,
                        SnapshotReason.SCHEDULED,
                        "SYSTEM"
                    );
                    snapshotCount++;
                } catch (Exception e) {
                    logger.error("Failed to create daily snapshot for SKU={}: {}",
                            stock.getSku(), e.getMessage());
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Daily snapshot job completed: {} snapshots created in {}ms",
                    snapshotCount, duration);

        } catch (Exception e) {
            logger.error("Daily snapshot job failed", e);
        }
    }

    /**
     * Create month-end snapshots
     * Runs on last day of month at 23:59:00
     */
    @Scheduled(cron = "0 59 23 L * *")  // L = last day of month
    public void createMonthEndSnapshots() {
        logger.info("Starting month-end snapshot job");

        long startTime = System.currentTimeMillis();
        int snapshotCount = 0;

        try {
            var allStock = productStockRepository.findAll();

            for (var stock : allStock) {
                try {
                    snapshotService.createSnapshot(
                        stock.getSku(),
                        SnapshotType.MONTH_END,
                        SnapshotReason.FINANCIAL_REPORTING,
                        "SYSTEM"
                    );
                    snapshotCount++;
                } catch (Exception e) {
                    logger.error("Failed to create month-end snapshot for SKU={}: {}",
                            stock.getSku(), e.getMessage());
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Month-end snapshot job completed: {} snapshots created in {}ms",
                    snapshotCount, duration);

        } catch (Exception e) {
            logger.error("Month-end snapshot job failed", e);
        }
    }

    /**
     * Create quarter-end snapshots
     * Runs on last day of quarter (Mar 31, Jun 30, Sep 30, Dec 31) at 23:59:00
     */
    @Scheduled(cron = "0 59 23 L 3,6,9,12 *")
    public void createQuarterEndSnapshots() {
        logger.info("Starting quarter-end snapshot job");

        long startTime = System.currentTimeMillis();
        int snapshotCount = 0;

        try {
            var allStock = productStockRepository.findAll();

            for (var stock : allStock) {
                try {
                    snapshotService.createSnapshot(
                        stock.getSku(),
                        SnapshotType.QUARTER_END,
                        SnapshotReason.FINANCIAL_REPORTING,
                        "SYSTEM"
                    );
                    snapshotCount++;
                } catch (Exception e) {
                    logger.error("Failed to create quarter-end snapshot for SKU={}: {}",
                            stock.getSku(), e.getMessage());
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Quarter-end snapshot job completed: {} snapshots created in {}ms",
                    snapshotCount, duration);

        } catch (Exception e) {
            logger.error("Quarter-end snapshot job failed", e);
        }
    }

    /**
     * Create year-end snapshots
     * Runs on December 31 at 23:59:00
     */
    @Scheduled(cron = "0 59 23 31 12 *")
    public void createYearEndSnapshots() {
        logger.info("Starting year-end snapshot job");

        long startTime = System.currentTimeMillis();
        int snapshotCount = 0;

        try {
            var allStock = productStockRepository.findAll();

            for (var stock : allStock) {
                try {
                    snapshotService.createSnapshot(
                        stock.getSku(),
                        SnapshotType.YEAR_END,
                        SnapshotReason.FINANCIAL_REPORTING,
                        "SYSTEM"
                    );
                    snapshotCount++;
                } catch (Exception e) {
                    logger.error("Failed to create year-end snapshot for SKU={}: {}",
                            stock.getSku(), e.getMessage());
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Year-end snapshot job completed: {} snapshots created in {}ms",
                    snapshotCount, duration);

        } catch (Exception e) {
            logger.error("Year-end snapshot job failed", e);
        }
    }

    /**
     * Cleanup old snapshots based on retention policy
     * Runs weekly on Sunday at 02:00:00
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    public void cleanupOldSnapshots() {
        logger.info("Starting snapshot cleanup job");

        try {
            snapshotService.cleanupOldSnapshots();
            logger.info("Snapshot cleanup job completed");
        } catch (Exception e) {
            logger.error("Snapshot cleanup job failed", e);
        

}
}
}
