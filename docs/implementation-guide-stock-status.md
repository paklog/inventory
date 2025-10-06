# Implementation Guide: Stock Status Management

**Priority**: CRITICAL
**Estimated Effort**: 2-3 days
**Dependencies**: None

---

## Overview

Implement stock status segregation to enable quality management, quarantine handling, and damaged goods tracking. This is a **foundational capability** required for any enterprise WMS.

---

## Step 1: Enhance ProductStock Aggregate

### Current State
ProductStock tracks only total quantities (QoH, Allocated, ATP).

### Target State
ProductStock tracks quantities segregated by status (AVAILABLE, QUARANTINE, DAMAGED, etc.).

### Code Changes

**File**: `/src/main/java/com/paklog/inventory/domain/model/ProductStock.java`

Add stock status tracking:

```java
public class ProductStock {

    private String sku;
    private StockLevel stockLevel; // Keep for backward compatibility
    private StockStatusQuantity stockStatusQuantity; // NEW: status-segregated quantities
    private LocalDateTime lastUpdated;
    private Long version;
    private List<LotBatch> lotBatches;
    private List<InventoryHold> inventoryHolds; // NEW: holds management

    // ... existing code ...

    /**
     * Move quantity from one status to another (e.g., QUARANTINE → AVAILABLE)
     */
    public void changeStockStatus(StockStatus fromStatus, StockStatus toStatus,
                                  int quantity, String reason) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("changeStatus", quantity,
                "Quantity must be positive");
        }

        StockStatusQuantity previousStatusQuantity = this.stockStatusQuantity;
        this.stockStatusQuantity = stockStatusQuantity.moveQuantity(fromStatus, toStatus, quantity);
        this.lastUpdated = LocalDateTime.now();

        addEvent(new StockStatusChangedEvent(sku, fromStatus, toStatus, quantity, reason, null));
    }

    /**
     * Place a hold on inventory
     */
    public void placeHold(HoldType holdType, int quantity, String reason, String placedBy) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("placeHold", quantity,
                "Hold quantity must be positive");
        }

        // Verify available quantity
        int availableQuantity = stockStatusQuantity.getAvailableQuantity();
        if (availableQuantity < quantity) {
            throw new InsufficientStockException(sku, quantity, availableQuantity);
        }

        InventoryHold hold = InventoryHold.create(holdType, quantity, reason, placedBy);
        this.inventoryHolds.add(hold);
        this.lastUpdated = LocalDateTime.now();

        addEvent(new InventoryHoldPlacedEvent(sku, hold.getHoldId(), holdType,
            quantity, reason, null));
    }

    /**
     * Release a hold
     */
    public void releaseHold(String holdId, String releasedBy) {
        InventoryHold hold = inventoryHolds.stream()
            .filter(h -> h.getHoldId().equals(holdId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Hold not found: " + holdId));

        if (!hold.isActive()) {
            throw new IllegalStateException("Cannot release expired or inactive hold");
        }

        inventoryHolds.removeIf(h -> h.getHoldId().equals(holdId));
        this.lastUpdated = LocalDateTime.now();

        addEvent(new InventoryHoldReleasedEvent(sku, holdId,
            hold.getQuantityOnHold(), releasedBy));
    }

    /**
     * Get Available-to-Promise considering status and holds
     */
    @Override
    public int getAvailableToPromise() {
        int availableQuantity = stockStatusQuantity.getAvailableQuantity();

        // Subtract active holds
        int totalHeld = inventoryHolds.stream()
            .filter(InventoryHold::isActive)
            .mapToInt(InventoryHold::getQuantityOnHold)
            .sum();

        return Math.max(0, availableQuantity - totalHeld);
    }

    /**
     * Get quantity by specific status
     */
    public int getQuantityByStatus(StockStatus status) {
        return stockStatusQuantity.getQuantityByStatus(status);
    }

    /**
     * Get active holds
     */
    public List<InventoryHold> getActiveHolds() {
        return inventoryHolds.stream()
            .filter(InventoryHold::isActive)
            .toList();
    }
}
```

---

## Step 2: Update InventoryCommandService

Add new command operations for stock status management.

**File**: `/src/main/java/com/paklog/inventory/application/service/InventoryCommandService.java`

```java
@Service
public class InventoryCommandService {

    // ... existing dependencies ...

    /**
     * Change stock status (e.g., release from quarantine)
     */
    @Transactional
    public ProductStock changeStockStatus(String sku, StockStatus fromStatus,
                                         StockStatus toStatus, int quantity,
                                         String reason, String operatorId) {
        log.info("Changing stock status for sku: {}, from: {}, to: {}, quantity: {}",
                sku, fromStatus, toStatus, quantity);

        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new ProductStockNotFoundException(sku));

        productStock.changeStockStatus(fromStatus, toStatus, quantity, reason);

        // Create ledger entry
        InventoryLedgerEntry ledgerEntry = InventoryLedgerEntry.forAdjustment(
            sku, 0, // No quantity change, just status change
            String.format("Status change: %s -> %s, Reason: %s", fromStatus, toStatus, reason),
            operatorId
        );
        inventoryLedgerRepository.save(ledgerEntry);

        ProductStock savedStock = productStockRepository.save(productStock);
        publishDomainEvents(savedStock);

        log.info("Stock status changed for sku: {}", sku);
        return savedStock;
    }

    /**
     * Place hold on inventory
     */
    @Transactional
    public ProductStock placeInventoryHold(String sku, HoldType holdType,
                                          int quantity, String reason,
                                          String placedBy) {
        log.info("Placing {} hold on sku: {}, quantity: {}, reason: {}",
                holdType, sku, quantity, reason);

        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new ProductStockNotFoundException(sku));

        productStock.placeHold(holdType, quantity, reason, placedBy);

        ProductStock savedStock = productStockRepository.save(productStock);
        publishDomainEvents(savedStock);

        log.info("Hold placed on sku: {}", sku);
        return savedStock;
    }

    /**
     * Release inventory hold
     */
    @Transactional
    public ProductStock releaseInventoryHold(String holdId, String sku, String releasedBy) {
        log.info("Releasing hold: {} for sku: {}", holdId, sku);

        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new ProductStockNotFoundException(sku));

        productStock.releaseHold(holdId, releasedBy);

        ProductStock savedStock = productStockRepository.save(productStock);
        publishDomainEvents(savedStock);

        log.info("Hold released: {}", holdId);
        return savedStock;
    }

    /**
     * Receive stock in quarantine status (QC required)
     */
    @Transactional
    public ProductStock receiveStockInQuarantine(String sku, int quantity, String receiptId) {
        log.info("Receiving stock in QUARANTINE for sku: {}, quantity: {}", sku, quantity);

        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseGet(() -> {
                    log.info("Creating new product stock for sku: {}", sku);
                    return ProductStock.createWithStatusTracking(sku);
                });

        productStock.receiveStockInStatus(quantity, StockStatus.QUARANTINE, receiptId);

        ProductStock savedStock = productStockRepository.save(productStock);
        publishDomainEvents(savedStock);

        log.info("Stock received in QUARANTINE for sku: {}", sku);
        return savedStock;
    }
}
```

---

## Step 3: Add REST API Endpoints

**File**: `/src/main/java/com/paklog/inventory/infrastructure/web/InventoryController.java`

```java
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryCommandService inventoryCommandService;

    // ... existing endpoints ...

    /**
     * Change stock status
     * POST /api/v1/inventory/{sku}/status-change
     */
    @PostMapping("/{sku}/status-change")
    public ResponseEntity<StockLevelResponse> changeStockStatus(
            @PathVariable String sku,
            @RequestBody ChangeStockStatusRequest request) {

        ProductStock updatedStock = inventoryCommandService.changeStockStatus(
            sku,
            request.getFromStatus(),
            request.getToStatus(),
            request.getQuantity(),
            request.getReason(),
            request.getOperatorId()
        );

        return ResponseEntity.ok(StockLevelResponse.fromDomain(updatedStock));
    }

    /**
     * Place inventory hold
     * POST /api/v1/inventory/{sku}/holds
     */
    @PostMapping("/{sku}/holds")
    public ResponseEntity<InventoryHoldResponse> placeHold(
            @PathVariable String sku,
            @RequestBody PlaceHoldRequest request) {

        ProductStock updatedStock = inventoryCommandService.placeInventoryHold(
            sku,
            request.getHoldType(),
            request.getQuantity(),
            request.getReason(),
            request.getPlacedBy()
        );

        return ResponseEntity.ok(InventoryHoldResponse.fromDomain(updatedStock));
    }

    /**
     * Release inventory hold
     * DELETE /api/v1/inventory/{sku}/holds/{holdId}
     */
    @DeleteMapping("/{sku}/holds/{holdId}")
    public ResponseEntity<Void> releaseHold(
            @PathVariable String sku,
            @PathVariable String holdId,
            @RequestParam String releasedBy) {

        inventoryCommandService.releaseInventoryHold(holdId, sku, releasedBy);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get stock by status
     * GET /api/v1/inventory/{sku}/by-status
     */
    @GetMapping("/{sku}/by-status")
    public ResponseEntity<StockByStatusResponse> getStockByStatus(@PathVariable String sku) {
        ProductStock stock = inventoryQueryService.getStockBySku(sku);
        return ResponseEntity.ok(StockByStatusResponse.fromDomain(stock));
    }
}
```

---

## Step 4: Database Schema Changes

### MongoDB Document Structure

**Collection**: `product_stocks`

```json
{
  "_id": "SKU-001",
  "sku": "SKU-001",
  "stockLevel": {
    "quantityOnHand": 500,
    "quantityAllocated": 100,
    "availableToPromise": 400
  },
  "stockStatusQuantity": {
    "quantitiesByStatus": {
      "AVAILABLE": 400,
      "QUARANTINE": 50,
      "DAMAGED": 25,
      "RETURNED": 25
    }
  },
  "inventoryHolds": [
    {
      "holdId": "HOLD-001",
      "holdType": "QUALITY_HOLD",
      "quantityOnHold": 50,
      "reason": "Supplier quality issue",
      "placedBy": "inspector1",
      "placedAt": "2025-10-04T10:00:00",
      "expiresAt": null,
      "lotNumber": null,
      "location": null
    }
  ],
  "lotBatches": [...],
  "lastUpdated": "2025-10-04T10:00:00",
  "version": 5
}
```

---

## Step 5: Testing Strategy

### Unit Tests

```java
@Test
void shouldChangeStockStatusFromQuarantineToAvailable() {
    ProductStock stock = ProductStock.createWithStatusTracking("SKU-001");
    stock.receiveStockInStatus(100, StockStatus.QUARANTINE, "RECEIPT-001");

    stock.changeStockStatus(StockStatus.QUARANTINE, StockStatus.AVAILABLE, 100,
        "QC passed");

    assertEquals(100, stock.getQuantityByStatus(StockStatus.AVAILABLE));
    assertEquals(0, stock.getQuantityByStatus(StockStatus.QUARANTINE));
}

@Test
void shouldCalculateATPWithHolds() {
    ProductStock stock = ProductStock.create("SKU-001", 100);
    stock.placeHold(HoldType.QUALITY_HOLD, 20, "QC inspection", "inspector1");

    assertEquals(80, stock.getAvailableToPromise());
}

@Test
void shouldPreventAllocationOfQuarantineStock() {
    ProductStock stock = ProductStock.createWithStatusTracking("SKU-001");
    stock.receiveStockInStatus(100, StockStatus.QUARANTINE, "RECEIPT-001");

    assertThrows(InsufficientStockException.class,
        () -> stock.allocate(50));
}
```

### Integration Tests

```java
@Test
@Transactional
void shouldPublishStockStatusChangedEvent() {
    ProductStock stock = ProductStock.createWithStatusTracking("SKU-001");
    stock.receiveStockInStatus(100, StockStatus.QUARANTINE, "RECEIPT-001");
    productStockRepository.save(stock);

    inventoryCommandService.changeStockStatus("SKU-001",
        StockStatus.QUARANTINE, StockStatus.AVAILABLE, 100,
        "QC passed", "inspector1");

    verify(outboxRepository).saveAll(argThat(events ->
        events.stream().anyMatch(e ->
            e.getEventType().equals("com.paklog.inventory.stock_status.changed"))));
}
```

---

## Step 6: Event Handlers (Other Contexts)

### Example: Order Management Context

```java
@Component
public class InventoryEventHandler {

    @EventListener
    public void handleInventoryHoldPlaced(InventoryHoldPlacedEvent event) {
        // If hold affects allocated orders, send notification
        if (event.getHoldType() == HoldType.RECALL_HOLD) {
            orderService.notifyOrdersAffectedByRecall(event.getSku());
        }
    }

    @EventListener
    public void handleStockStatusChanged(StockStatusChangedEvent event) {
        // If stock released from quarantine, retry failed allocations
        if (event.getNewStatus() == StockStatus.AVAILABLE) {
            orderService.retryFailedAllocations(event.getSku());
        }
    }
}
```

---

## Step 7: Monitoring & Metrics

```java
// Add to InventoryMetricsService
public void recordStockStatusChange(String sku, StockStatus fromStatus,
                                   StockStatus toStatus, int quantity) {
    registry.counter("inventory.status.change",
        Tags.of("sku", sku, "from", fromStatus.name(), "to", toStatus.name()))
        .increment(quantity);
}

public void recordHoldPlaced(String sku, HoldType holdType, int quantity) {
    registry.counter("inventory.holds.placed",
        Tags.of("sku", sku, "type", holdType.name()))
        .increment(quantity);
}

// Gauges for current state
registry.gauge("inventory.stock.quarantine",
    () -> stockStatusRepository.getTotalQuantityByStatus(StockStatus.QUARANTINE));

registry.gauge("inventory.holds.active.count",
    () -> inventoryHoldRepository.countActiveHolds());
```

---

## Rollout Plan

### Phase 1: Foundation (Day 1)
- ✅ Create domain models (StockStatus, StockStatusQuantity, InventoryHold)
- ✅ Create domain events
- ✅ Unit tests for domain models

### Phase 2: Integration (Day 2)
- Update ProductStock aggregate
- Update InventoryCommandService
- Database schema migration
- Integration tests

### Phase 3: API & Deployment (Day 3)
- REST API endpoints
- API documentation
- Performance testing
- Production deployment

---

## Success Criteria

- ✅ All stock received in quarantine requires QC approval
- ✅ ATP calculation excludes quarantine/damaged stock
- ✅ Holds prevent allocation
- ✅ Domain events published for all status changes
- ✅ Zero data inconsistencies (status quantities sum to total)
- ✅ Performance: <100ms for status change operations

---

**End of Implementation Guide**
