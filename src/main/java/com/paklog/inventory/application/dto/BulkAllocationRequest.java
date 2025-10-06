package com.paklog.inventory.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * DTO for bulk allocation requests.
 * Allows allocating inventory for multiple orders in a single API call.
 */
public class BulkAllocationRequest {

    @NotEmpty(message = "Allocation requests cannot be empty")
    @Valid
    private List<AllocationRequestItem> requests;

    public BulkAllocationRequest() {
    }

    public BulkAllocationRequest(List<AllocationRequestItem> requests) {
        this.requests = requests;
    }

    public List<AllocationRequestItem> getRequests() {
        return requests;
    }

    public void setRequests(List<AllocationRequestItem> requests) {
        this.requests = requests;
    }

    @Override
    public String toString() {
        return "BulkAllocationRequest{" +
                "requestCount=" + (requests != null ? requests.size() : 0) +
                '}';
    }
}
