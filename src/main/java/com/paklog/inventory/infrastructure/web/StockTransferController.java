package com.paklog.inventory.infrastructure.web;

import com.paklog.inventory.application.dto.InitiateTransferRequest;
import com.paklog.inventory.application.dto.StockTransferResponse;
import com.paklog.inventory.application.service.StockTransferService;
import com.paklog.inventory.domain.model.Location;
import com.paklog.inventory.domain.model.LocationType;
import com.paklog.inventory.domain.model.StockTransfer;
import com.paklog.inventory.domain.model.TransferStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for stock transfer management.
 * Provides endpoints for initiating, tracking, and completing transfers.
 */
@RestController
@RequestMapping("/api/v1/inventory/transfers")
public class StockTransferController {

    private static final Logger logger = LoggerFactory.getLogger(StockTransferController.class);

    private final StockTransferService stockTransferService;

    public StockTransferController(StockTransferService stockTransferService) {
        this.stockTransferService = stockTransferService;
    }

    /**
     * Initiate a stock transfer
     * POST /api/v1/inventory/transfers
     */
    @PostMapping
    public ResponseEntity<String> initiateTransfer(@RequestBody InitiateTransferRequest request) {
        logger.info("Initiating transfer: SKU={}, qty={}, from={}/{}, to={}/{}",
                request.sku(), request.quantity(),
                request.sourceWarehouseId(), request.sourceZoneId(),
                request.destinationWarehouseId(), request.destinationZoneId());

        Location sourceLocation = Location.of(
            request.sourceWarehouseId(),
            request.sourceZoneId(),
            request.sourceAisleId(),
            request.sourceShelfId(),
            request.sourceBinId(),
            LocationType.GENERAL
        );

        Location destinationLocation = Location.of(
            request.destinationWarehouseId(),
            request.destinationZoneId(),
            request.destinationAisleId(),
            request.destinationShelfId(),
            request.destinationBinId(),
            LocationType.GENERAL
        );

        String transferId = stockTransferService.initiateTransfer(
            request.sku(),
            sourceLocation,
            destinationLocation,
            request.quantity(),
            request.initiatedBy(),
            request.reason()
        );

        return ResponseEntity.ok(transferId);
    }

    /**
     * Mark transfer as in-transit
     * POST /api/v1/inventory/transfers/{transferId}/in-transit
     */
    @PostMapping("/{transferId}/in-transit")
    public ResponseEntity<Void> markInTransit(@PathVariable String transferId) {
        logger.info("Marking transfer in-transit: transferId={}", transferId);

        stockTransferService.markInTransit(transferId);
        return ResponseEntity.ok().build();
    }

    /**
     * Complete transfer
     * POST /api/v1/inventory/transfers/{transferId}/complete
     */
    @PostMapping("/{transferId}/complete")
    public ResponseEntity<Void> completeTransfer(
            @PathVariable String transferId,
            @RequestParam int actualQuantityReceived) {
        logger.info("Completing transfer: transferId={}, actualQty={}",
                transferId, actualQuantityReceived);

        stockTransferService.completeTransfer(transferId, actualQuantityReceived);
        return ResponseEntity.ok().build();
    }

    /**
     * Cancel transfer
     * POST /api/v1/inventory/transfers/{transferId}/cancel
     */
    @PostMapping("/{transferId}/cancel")
    public ResponseEntity<Void> cancelTransfer(
            @PathVariable String transferId,
            @RequestParam String cancelledBy,
            @RequestParam String cancellationReason) {
        logger.info("Cancelling transfer: transferId={}, cancelledBy={}",
                transferId, cancelledBy);

        stockTransferService.cancelTransfer(transferId, cancelledBy, cancellationReason);
        return ResponseEntity.ok().build();
    }

    /**
     * Get transfer details
     * GET /api/v1/inventory/transfers/{transferId}
     */
    @GetMapping("/{transferId}")
    public ResponseEntity<StockTransferResponse> getTransfer(@PathVariable String transferId) {
        logger.debug("Getting transfer: transferId={}", transferId);

        StockTransfer transfer = stockTransferService.getTransfer(transferId);
        return ResponseEntity.ok(StockTransferResponse.fromDomain(transfer));
    }

    /**
     * Get transfers by SKU
     * GET /api/v1/inventory/transfers/sku/{sku}
     */
    @GetMapping("/sku/{sku}")
    public ResponseEntity<List<StockTransferResponse>> getTransfersBySKU(@PathVariable String sku) {
        logger.debug("Getting transfers by SKU: {}", sku);

        List<StockTransferResponse> transfers = stockTransferService.getTransfersBySKU(sku)
            .stream()
            .map(StockTransferResponse::fromDomain)
            .toList();

        return ResponseEntity.ok(transfers);
    }

    /**
     * Get in-transit transfers
     * GET /api/v1/inventory/transfers/in-transit
     */
    @GetMapping("/in-transit")
    public ResponseEntity<List<StockTransferResponse>> getInTransitTransfers() {
        logger.debug("Getting in-transit transfers");

        List<StockTransferResponse> transfers = stockTransferService.getInTransitTransfers()
            .stream()
            .map(StockTransferResponse::fromDomain)
            .toList();

        return ResponseEntity.ok(transfers);
    }

    /**
     * Get transfers by status
     * GET /api/v1/inventory/transfers/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<StockTransferResponse>> getTransfersByStatus(
            @PathVariable TransferStatus status) {
        logger.debug("Getting transfers by status: {}", status);

        List<StockTransferResponse> transfers = stockTransferService.getTransfersByStatus(status)
            .stream()
            .map(StockTransferResponse::fromDomain)
            .toList();

        return ResponseEntity.ok(transfers);
    }

    /**
     * Get transfers within date range
     * GET /api/v1/inventory/transfers/date-range
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<StockTransferResponse>> getTransfersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        logger.debug("Getting transfers by date range: {} to {}", start, end);

        List<StockTransferResponse> transfers = stockTransferService.getTransfersByDateRange(start, end)
            .stream()
            .map(StockTransferResponse::fromDomain)
            .toList();

        return ResponseEntity.ok(transfers);
    }

    /**
     * Get transfers with shrinkage
     * GET /api/v1/inventory/transfers/with-shrinkage
     */
    @GetMapping("/with-shrinkage")
    public ResponseEntity<List<StockTransferResponse>> getTransfersWithShrinkage() {
        logger.debug("Getting transfers with shrinkage");

        List<StockTransferResponse> transfers = stockTransferService.getTransfersWithShrinkage()
            .stream()
            .map(StockTransferResponse::fromDomain)
            .toList();

        return ResponseEntity.ok(transfers);
    }

    /**
     * Count transfers by status
     * GET /api/v1/inventory/transfers/count/{status}
     */
    @GetMapping("/count/{status}")
    public ResponseEntity<Long> countByStatus(@PathVariable TransferStatus status) {
        logger.debug("Counting transfers by status: {}", status);

        long count = stockTransferService.countByStatus(status);
        return ResponseEntity.ok(count);
    }
}
