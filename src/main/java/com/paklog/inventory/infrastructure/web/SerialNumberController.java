package com.paklog.inventory.infrastructure.web;

import com.paklog.inventory.application.dto.ReceiveSerialNumberRequest;
import com.paklog.inventory.application.dto.SerialNumberResponse;
import com.paklog.inventory.application.service.SerialNumberService;
import com.paklog.inventory.domain.model.Location;
import com.paklog.inventory.domain.model.LocationType;
import com.paklog.inventory.domain.model.SerialNumber;
import com.paklog.inventory.domain.model.SerialStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for serial number tracking.
 * Provides endpoints for serial number lifecycle management.
 */
@RestController
@RequestMapping("/api/v1/inventory/serial-numbers")
public class SerialNumberController {

    private static final Logger logger = LoggerFactory.getLogger(SerialNumberController.class);

    private final SerialNumberService serialNumberService;

    public SerialNumberController(SerialNumberService serialNumberService) {
        this.serialNumberService = serialNumberService;
    }

    /**
     * Receive a serial-tracked item
     * POST /api/v1/inventory/serial-numbers/receive
     */
    @PostMapping("/receive")
    public ResponseEntity<Void> receiveSerialNumber(@RequestBody ReceiveSerialNumberRequest request) {
        logger.info("Receiving serial number: SKU={}, serial={}",
                request.sku(), request.serialNumber());

        Location location = Location.of(
            request.warehouseId(),
            request.zoneId(),
            request.aisleId(),
            request.shelfId(),
            request.binId(),
            LocationType.GENERAL
        );

        serialNumberService.receiveSerialNumber(
            request.sku(),
            request.serialNumber(),
            location,
            request.manufactureDate(),
            request.warrantyExpiryDate()
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Allocate a serial number to a customer
     * POST /api/v1/inventory/serial-numbers/{serialNumber}/allocate
     */
    @PostMapping("/{serialNumber}/allocate")
    public ResponseEntity<Void> allocateSerialNumber(
            @PathVariable String serialNumber,
            @RequestParam String sku,
            @RequestParam String customerId) {
        logger.info("Allocating serial number: serial={}, customer={}",
                serialNumber, customerId);

        serialNumberService.allocateSerialNumber(sku, serialNumber, customerId);
        return ResponseEntity.ok().build();
    }

    /**
     * Ship a serial number
     * POST /api/v1/inventory/serial-numbers/{serialNumber}/ship
     */
    @PostMapping("/{serialNumber}/ship")
    public ResponseEntity<Void> shipSerialNumber(@PathVariable String serialNumber) {
        logger.info("Shipping serial number: {}", serialNumber);

        serialNumberService.shipSerialNumber(serialNumber);
        return ResponseEntity.ok().build();
    }

    /**
     * Move serial number to new location
     * POST /api/v1/inventory/serial-numbers/{serialNumber}/move
     */
    @PostMapping("/{serialNumber}/move")
    public ResponseEntity<Void> moveSerialNumber(
            @PathVariable String serialNumber,
            @RequestParam String warehouseId,
            @RequestParam String zoneId,
            @RequestParam(required = false) String aisleId,
            @RequestParam(required = false) String rackId,
            @RequestParam(required = false) String shelfId,
            @RequestParam(required = false) String binId) {
        logger.info("Moving serial number: serial={}, to warehouse={}, zone={}",
                serialNumber, warehouseId, zoneId);

        Location newLocation = Location.of(warehouseId, zoneId, aisleId, shelfId, binId, LocationType.GENERAL);
        serialNumberService.moveSerialNumber(serialNumber, newLocation);
        return ResponseEntity.ok().build();
    }

    /**
     * Change serial number status
     * POST /api/v1/inventory/serial-numbers/{serialNumber}/status
     */
    @PostMapping("/{serialNumber}/status")
    public ResponseEntity<Void> changeSerialStatus(
            @PathVariable String serialNumber,
            @RequestParam SerialStatus newStatus) {
        logger.info("Changing serial status: serial={}, newStatus={}",
                serialNumber, newStatus);

        serialNumberService.changeSerialStatus(serialNumber, newStatus);
        return ResponseEntity.ok().build();
    }

    /**
     * Get serial number details
     * GET /api/v1/inventory/serial-numbers/{serialNumber}
     */
    @GetMapping("/{serialNumber}")
    public ResponseEntity<SerialNumberResponse> getSerialNumber(@PathVariable String serialNumber) {
        logger.debug("Getting serial number: {}", serialNumber);

        SerialNumber serial = serialNumberService.getSerialNumber(serialNumber);
        return ResponseEntity.ok(SerialNumberResponse.fromDomain(serial));
    }

    /**
     * Get available serials for a SKU
     * GET /api/v1/inventory/serial-numbers/sku/{sku}/available
     */
    @GetMapping("/sku/{sku}/available")
    public ResponseEntity<List<SerialNumberResponse>> getAvailableSerials(@PathVariable String sku) {
        logger.debug("Getting available serials: SKU={}", sku);

        List<SerialNumberResponse> serials = serialNumberService.getAvailableSerials(sku)
            .stream()
            .map(SerialNumberResponse::fromDomain)
            .toList();

        return ResponseEntity.ok(serials);
    }

    /**
     * Get all serials for a SKU
     * GET /api/v1/inventory/serial-numbers/sku/{sku}
     */
    @GetMapping("/sku/{sku}")
    public ResponseEntity<List<SerialNumberResponse>> getAllSerials(@PathVariable String sku) {
        logger.debug("Getting all serials: SKU={}", sku);

        List<SerialNumberResponse> serials = serialNumberService.getAllSerials(sku)
            .stream()
            .map(SerialNumberResponse::fromDomain)
            .toList();

        return ResponseEntity.ok(serials);
    }

    /**
     * Get serials allocated to a customer
     * GET /api/v1/inventory/serial-numbers/customer/{customerId}
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<SerialNumberResponse>> getCustomerSerials(@PathVariable String customerId) {
        logger.debug("Getting customer serials: customerId={}", customerId);

        List<SerialNumberResponse> serials = serialNumberService.getCustomerSerials(customerId)
            .stream()
            .map(SerialNumberResponse::fromDomain)
            .toList();

        return ResponseEntity.ok(serials);
    }

    /**
     * Check warranty status
     * GET /api/v1/inventory/serial-numbers/{serialNumber}/warranty
     */
    @GetMapping("/{serialNumber}/warranty")
    public ResponseEntity<Boolean> isWarrantyValid(@PathVariable String serialNumber) {
        logger.debug("Checking warranty: {}", serialNumber);

        boolean valid = serialNumberService.isWarrantyValid(serialNumber);
        return ResponseEntity.ok(valid);
    }
}
