package com.paklog.inventory.infrastructure.web;

import com.paklog.inventory.application.dto.AddStockRequest;
import com.paklog.inventory.application.dto.MoveStockRequest;
import com.paklog.inventory.application.dto.PhysicalReservationRequest;
import com.paklog.inventory.application.dto.PickStockRequest;
import com.paklog.inventory.application.service.PhysicalStockCommandService;
import com.paklog.inventory.application.service.PhysicalStockQueryService;
import com.paklog.inventory.domain.model.Location;
import com.paklog.inventory.domain.model.StockLocation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory/physical-stock")
public class PhysicalStockController {

    private final PhysicalStockCommandService commandService;
    private final PhysicalStockQueryService queryService;

    public PhysicalStockController(PhysicalStockCommandService commandService, PhysicalStockQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<Void> addStock(@RequestBody AddStockRequest request) {
        commandService.addStock(request.getSku(), request.getLocation(), request.getQuantity());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/movements")
    public ResponseEntity<Void> moveStock(@RequestBody MoveStockRequest request) {
        commandService.moveStock(request.getSku(), request.getFromLocation(), request.getToLocation(), request.getQuantity());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/physical-reservations")
    public ResponseEntity<Void> addPhysicalReservation(@RequestBody PhysicalReservationRequest request) {
        commandService.addPhysicalReservation(request.getSku(), request.getLocation(), request.getQuantity(), request.getReservationId());
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/physical-reservations/{sku}/{aisle}/{shelf}/{bin}/{reservationId}")
    public ResponseEntity<Void> removePhysicalReservation(
            @PathVariable String sku,
            @PathVariable String aisle,
            @PathVariable String shelf,
            @PathVariable String bin,
            @PathVariable String reservationId) {
        Location location = new Location(aisle, shelf, bin);
        commandService.removePhysicalReservation(sku, location, reservationId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/picks")
    public ResponseEntity<Void> pickStock(@RequestBody PickStockRequest request) {
        commandService.pickStock(request.getSku(), request.getLocation(), request.getQuantity(), request.getReservationId());
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{sku}")
    public ResponseEntity<List<StockLocation>> getPhysicalStock(@PathVariable String sku) {
        return ResponseEntity.ok(queryService.findBySku(sku));
    }
}
