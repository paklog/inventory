package com.paklog.inventory.application.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for bulk allocation operations.
 */
public class BulkAllocationResponse {

    private final int totalRequests;
    private final int successfulAllocations;
    private final int failedAllocations;
    private final List<AllocationResult> results;
    private final LocalDateTime processedAt;
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
