package com.paklog.inventory.infrastructure.web;

import com.paklog.inventory.application.dto.CreateAdjustmentRequest;
import com.paklog.inventory.application.dto.InventoryHealthMetricsResponse;
import com.paklog.inventory.application.dto.StockLevelResponse;
import com.paklog.inventory.application.service.InventoryCommandService;
import com.paklog.inventory.application.service.InventoryQueryService;
import com.paklog.inventory.domain.exception.DomainException;
import com.paklog.inventory.domain.exception.InsufficientStockException;
import com.paklog.inventory.domain.exception.InvalidQuantityException;
import com.paklog.inventory.domain.exception.ProductStockNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryCommandService commandService;
    private final InventoryQueryService queryService;

    public InventoryController(InventoryCommandService commandService, InventoryQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping("/stock_levels/{sku}")
    public ResponseEntity<StockLevelResponse> getStockLevel(@PathVariable String sku) {
        StockLevelResponse stockLevel = queryService.getStockLevel(sku);
        return ResponseEntity.ok(stockLevel);
    }

    @PostMapping("/adjustments")
    public ResponseEntity<Void> createStockAdjustment(@Valid @RequestBody CreateAdjustmentRequest request) {
        // In a real application, operatorId would come from authentication context
        String operatorId = "admin"; 
        commandService.adjustStock(request.getSku(), request.getQuantityChange(), request.getReasonCode(), request.getComment(), operatorId);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/reports/health")
    public ResponseEntity<InventoryHealthMetricsResponse> getInventoryHealthMetrics(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        InventoryHealthMetricsResponse metrics = queryService.getInventoryHealthMetrics(category, startDate, endDate);
        return ResponseEntity.ok(metrics);
    }

    // Exception Handler for Domain Exceptions
    @ExceptionHandler(InvalidQuantityException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInvalidQuantityException(InvalidQuantityException e) {
        return e.getMessage();
    }
    
    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInsufficientStockException(InsufficientStockException e) {
        return e.getMessage();
    }
    
    @ExceptionHandler(ProductStockNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleProductStockNotFoundException(ProductStockNotFoundException e) {
        return e.getMessage();
    }
}