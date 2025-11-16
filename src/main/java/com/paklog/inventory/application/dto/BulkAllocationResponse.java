package com.paklog.inventory.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for bulk allocation operations.
 *
 * Provides comprehensive results for high-performance bulk stock allocation,
 * including:
 * - Summary statistics (successful/failed counts)
 * - Detailed results for each allocation attempt
 * - Performance metrics (processing time)
 * - Success rate calculation
 *
 * Designed to handle 10,000+ allocation requests in a single API call with
 * partial success handling, allowing some allocations to succeed even if others fail.
 */
public class BulkAllocationResponse {

    @JsonProperty("total_requests")
    private final int totalRequests;

    @JsonProperty("successful_allocations")
    private final int successfulAllocations;

    @JsonProperty("failed_allocations")
    private final int failedAllocations;

    private final List<AllocationResult> results;

    @JsonProperty("processed_at")
    private final LocalDateTime processedAt;

    @JsonProperty("processing_time_ms")
    private final long processingTimeMs;

    public BulkAllocationResponse(int totalRequests, int successfulAllocations,
                                 int failedAllocations, List<AllocationResult> results,
                                 LocalDateTime processedAt, long processingTimeMs) {
        this.totalRequests = totalRequests;
        this.successfulAllocations = successfulAllocations;
        this.failedAllocations = failedAllocations;
        this.results = results;
        this.processedAt = processedAt;
        this.processingTimeMs = processingTimeMs;
    }

    /**
     * Factory method to create a BulkAllocationResponse from allocation results.
     *
     * Automatically calculates success/failure counts from the results list.
     *
     * @param results list of individual allocation results
     * @param processingTimeMs total processing time in milliseconds
     * @return a new BulkAllocationResponse instance
     */
    public static BulkAllocationResponse of(List<AllocationResult> results, long processingTimeMs) {
        int total = results.size();
        int successful = (int) results.stream().filter(AllocationResult::isSuccess).count();
        int failed = total - successful;

        return new BulkAllocationResponse(total, successful, failed, results,
                                         LocalDateTime.now(), processingTimeMs);
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public int getSuccessfulAllocations() {
        return successfulAllocations;
    }

    public int getFailedAllocations() {
        return failedAllocations;
    }

    public List<AllocationResult> getResults() {
        return results;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    /**
     * Calculates the success rate as a percentage.
     *
     * @return success rate (0-100), or 0.0 if no requests were processed
     */
    @JsonProperty("success_rate")
    public double getSuccessRate() {
        return totalRequests > 0 ? (double) successfulAllocations / totalRequests * 100 : 0.0;
    }

    @Override
    public String toString() {
        return "BulkAllocationResponse{" +
                "totalRequests=" + totalRequests +
                ", successfulAllocations=" + successfulAllocations +
                ", failedAllocations=" + failedAllocations +
                ", successRate=" + String.format("%.2f%%", getSuccessRate()) +
                ", processingTimeMs=" + processingTimeMs +
                '}';
    }
}
