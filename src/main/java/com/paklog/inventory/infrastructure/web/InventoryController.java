package com.paklog.inventory.infrastructure.web;

import com.paklog.inventory.application.dto.CreateAdjustmentRequest;
import com.paklog.inventory.application.dto.CreateReservationRequest;
import com.paklog.inventory.application.dto.InventoryHealthMetricsResponse;
import com.paklog.inventory.application.dto.StockLevelResponse;
import com.paklog.inventory.application.service.InventoryCommandService;
import com.paklog.inventory.application.service.InventoryQueryService;
import com.paklog.inventory.domain.exception.DomainException;
import com.paklog.inventory.domain.exception.InsufficientStockException;
import com.paklog.inventory.domain.exception.InvalidQuantityException;
import com.paklog.inventory.domain.exception.ProductStockNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryCommandService commandService;
    private final InventoryQueryService queryService;

    public InventoryController(InventoryCommandService commandService, InventoryQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping("/stock_levels/{sku}")
    public ResponseEntity<StockLevelResponse> getStockLevel(@PathVariable String sku) {
        log.info("Getting stock level for SKU: {}", sku);
        StockLevelResponse stockLevel = queryService.getStockLevel(sku);
        log.info("Stock level for SKU: {} is {}", sku, stockLevel);
        return ResponseEntity.ok(stockLevel);
    }

    @PostMapping("/stock_levels/{sku}/reservations")
    public ResponseEntity<Void> createReservation(@PathVariable String sku, @RequestBody CreateReservationRequest request) {
        log.info("Creating reservation for SKU: {}, quantity: {}, orderId: {}", sku, request.getQuantity(), request.getOrderId());
        commandService.allocateStock(sku, request.getQuantity(), request.getOrderId());
        log.info("Reservation created for SKU: {}", sku);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/adjustments")
    public ResponseEntity<Void> createStockAdjustment(@Valid @RequestBody CreateAdjustmentRequest request) {
        log.info("Creating stock adjustment: {}", request);
        // In a real application, operatorId would come from authentication context
        String operatorId = "admin"; 
        commandService.adjustStock(request.getSku(), request.getQuantityChange(), request.getReasonCode(), request.getComment(), operatorId);
        log.info("Stock adjustment created: {}", request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/reports/health")
    public ResponseEntity<InventoryHealthMetricsResponse> getInventoryHealthMetrics(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        log.info("Getting inventory health metrics for category: {}, startDate: {}, endDate: {}", category, startDate, endDate);
        InventoryHealthMetricsResponse metrics = queryService.getInventoryHealthMetrics(category, startDate, endDate);
        log.info("Inventory health metrics: {}", metrics);
        return ResponseEntity.ok(metrics);
    }

    // Exception Handler for Domain Exceptions
    @ExceptionHandler(InvalidQuantityException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInvalidQuantityException(InvalidQuantityException e) {
        log.warn("Invalid quantity exception: {}", e.getMessage());
        return e.getMessage();
    }
    
    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInsufficientStockException(InsufficientStockException e) {
        log.warn("Insufficient stock exception: {}", e.getMessage());
        return e.getMessage();
    }
    
    @ExceptionHandler(ProductStockNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleProductStockNotFoundException(ProductStockNotFoundException e) {
        log.warn("Product stock not found exception: {}", e.getMessage());
        return e.getMessage();
    }
}