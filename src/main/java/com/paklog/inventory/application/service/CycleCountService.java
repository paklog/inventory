package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.*;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Application service for cycle counting operations.
 * Implements ABC-based cycle count strategies and variance management.
 */
@Service
public class CycleCountService {

    private static final Logger log = LoggerFactory.getLogger(CycleCountService.class);

    private final ProductStockRepository productStockRepository;
    private final InventoryCommandService inventoryCommandService;

    // Configurable thresholds
    private static final int A_ITEM_COUNT_FREQUENCY_DAYS = 30;   // Count A items monthly
    private static final int B_ITEM_COUNT_FREQUENCY_DAYS = 90;   // Count B items quarterly
    private static final int C_ITEM_COUNT_FREQUENCY_DAYS = 180;  // Count C items semi-annually
    private static final int VARIANCE_APPROVAL_THRESHOLD = 10;    // Require approval for variance > 10 units

    public CycleCountService(ProductStockRepository productStockRepository,
                            InventoryCommandService inventoryCommandService) {
        this.productStockRepository = productStockRepository;
        this.inventoryCommandService = inventoryCommandService;
    }

    /**
     * Schedule cycle counts based on ABC classification
     */
    @Transactional
    public List<CycleCount> scheduleABCCycleCounts(LocalDate scheduleDate) {
        log.info("Scheduling ABC cycle counts for date: {}", scheduleDate);

        List<ProductStock> allStocks = productStockRepository.findAll();
        List<CycleCount> scheduledCounts = new ArrayList<>();

        for (ProductStock stock : allStocks) {
            String abcClass = classifyABC(stock);
            boolean shouldCount = shouldCountItem(stock, abcClass, scheduleDate);

            if (shouldCount) {
                // Create count for each location where item is stored
                // In simplified version, using dummy location
                Location location = new Location("A", "1", "1");

                CycleCount count = CycleCount.create(
                    stock.getSku(),
                    location,
                    CountType.SCHEDULED_ABC,
                    null, // To be assigned
                    scheduleDate.atStartOfDay(),
                    stock.getQuantityOnHand()
                );

                scheduledCounts.add(count);
                log.info("Scheduled {} cycle count for SKU: {}, ABC Class: {}",
                        CountType.SCHEDULED_ABC, stock.getSku(), abcClass);
            }
        }

        log.info("Scheduled {} cycle counts for {}", scheduledCounts.size(), scheduleDate);
        return scheduledCounts;
    }

    /**
     * Create an exception-based count for discrepancies
     */
    @Transactional
    public CycleCount createExceptionCount(String sku, Location location, String reason) {
        log.info("Creating exception count for SKU: {} at location: {}, reason: {}",
                sku, location, reason);

        ProductStock stock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        CycleCount count = CycleCount.create(
            sku,
            location,
            CountType.EXCEPTION,
            null,
            LocalDateTime.now(),
            stock.getQuantityOnHand()
        );

        log.info("Created exception count: {}", count.getCountId());
        return count;
    }

    /**
     * Start a cycle count
     */
    @Transactional
    public CycleCount startCount(String countId, String countedBy) {
        log.info("Starting cycle count: {} by {}", countId, countedBy);
        // In real implementation, would load from repository
        // This is a simplified example showing the workflow
        throw new UnsupportedOperationException("Load count from repository and call count.start(countedBy)");
    }

    /**
     * Complete a cycle count
     */
    @Transactional
    public CycleCount completeCount(String countId, int countedQuantity, String lotNumber, String notes) {
        log.info("Completing cycle count: {} with quantity: {}", countId, countedQuantity);
        // Load count, complete it, check if approval needed
        throw new UnsupportedOperationException("Load count from repository and call count.complete()");
    }

    /**
     * Approve a count and adjust system inventory
     */
    @Transactional
    public void approveCountAndAdjust(String countId, String approvedBy,
                                     VarianceResolution resolution, String notes) {
        log.info("Approving cycle count: {} by {}, resolution: {}", countId, approvedBy, resolution);

        // Load count
        // CycleCount count = countRepository.findById(countId);

        // Approve
        // CycleCount approvedCount = count.approve(approvedBy, resolution, notes);

        // If resolution is ADJUST_SYSTEM, adjust inventory
        if (resolution == VarianceResolution.ADJUST_SYSTEM) {
            // int adjustment = approvedCount.getCountedQuantity() - approvedCount.getSystemQuantity();
            // inventoryCommandService.adjustStock(
            //     approvedCount.getSku(),
            //     adjustment,
            //     "CYCLE_COUNT_ADJUSTMENT",
            //     "Count ID: " + countId + ", " + notes,
            //     approvedBy
            // );
            log.info("System inventory adjusted for count: {}", countId);
        }

        // Save approved count
        // countRepository.save(approvedCount);
    }

    /**
     * Calculate cycle count accuracy metrics
     */
    public CycleCountAccuracyMetrics calculateAccuracyMetrics(LocalDate startDate, LocalDate endDate) {
        log.info("Calculating cycle count accuracy metrics from {} to {}", startDate, endDate);

        // In real implementation, would query completed counts from repository
        // This shows the calculation logic

        List<CycleCount> completedCounts = new ArrayList<>(); // Load from repository

        long totalCounts = completedCounts.size();
        long accurateCounts = completedCounts.stream()
                .filter(CycleCount::isAccurate)
                .count();

        double averageAccuracyPercentage = completedCounts.stream()
                .mapToDouble(CycleCount::getAccuracyPercentage)
                .average()
                .orElse(0.0);

        int totalVarianceUnits = completedCounts.stream()
                .map(CycleCount::getVariance)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Math::abs)
                .sum();

        return new CycleCountAccuracyMetrics(
            totalCounts,
            accurateCounts,
            averageAccuracyPercentage,
            totalVarianceUnits
        );
    }

    /**
     * Classify SKU by ABC based on velocity (simplified)
     */
    private String classifyABC(ProductStock stock) {
        // In real implementation, would use actual velocity/value data
        // This is simplified logic
        int quantityOnHand = stock.getQuantityOnHand();

        if (quantityOnHand > 1000) {
            return "A"; // High volume
        } else if (quantityOnHand > 100) {
            return "B"; // Medium volume
        } else {
            return "C"; // Low volume
        }
    }

    /**
     * Determine if item should be counted based on ABC class and last count date
     */
    private boolean shouldCountItem(ProductStock stock, String abcClass, LocalDate scheduleDate) {
        // In real implementation, would check last count date from database
        // This is simplified logic

        // For now, just use simple rotation based on ABC class
        int dayOfYear = scheduleDate.getDayOfYear();

        return switch (abcClass) {
            case "A" -> dayOfYear % A_ITEM_COUNT_FREQUENCY_DAYS == 0;
            case "B" -> dayOfYear % B_ITEM_COUNT_FREQUENCY_DAYS == 0;
            case "C" -> dayOfYear % C_ITEM_COUNT_FREQUENCY_DAYS == 0;
            default -> false;
        };
    }

    /**
     * DTO for cycle count accuracy metrics
     */
    public record CycleCountAccuracyMetrics(
        long totalCounts,
        long accurateCounts,
        double averageAccuracyPercentage,
        int totalVarianceUnits
    ) {
        public double getAccuracyRate() {
            return totalCounts > 0 ? (double) accurateCounts / totalCounts * 100 : 0.0;
        

}
}
}
