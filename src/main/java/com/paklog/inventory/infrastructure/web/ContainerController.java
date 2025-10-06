package com.paklog.inventory.infrastructure.web;

import com.paklog.inventory.application.dto.AddContainerItemRequest;
import com.paklog.inventory.application.dto.ContainerResponse;
import com.paklog.inventory.application.dto.CreateContainerRequest;
import com.paklog.inventory.application.service.ContainerService;
import com.paklog.inventory.domain.model.Container;
import com.paklog.inventory.domain.model.ContainerStatus;
import com.paklog.inventory.domain.model.ContainerType;
import com.paklog.inventory.domain.model.Location;
import com.paklog.inventory.domain.model.LocationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for container/LPN management.
 * Provides endpoints for container lifecycle and item management.
 */
@RestController
@RequestMapping("/api/v1/inventory/containers")
public class ContainerController {

    private static final Logger logger = LoggerFactory.getLogger(ContainerController.class);

    private final ContainerService containerService;

    public ContainerController(ContainerService containerService) {
        this.containerService = containerService;
    }

    /**
     * Create a new container
     * POST /api/v1/inventory/containers
     */
    @PostMapping
    public ResponseEntity<String> createContainer(@RequestBody CreateContainerRequest request) {
        logger.info("Creating container: type={}, warehouse={}, zone={}",
                request.type(), request.warehouseId(), request.zoneId());

        Location location = Location.of(
            request.warehouseId(),
            request.zoneId(),
            request.aisleId(),
            request.shelfId(),
            request.binId(),
            LocationType.GENERAL
        );

        String lpn = containerService.createContainer(request.type(), location, request.createdBy());
        return ResponseEntity.ok(lpn);
    }

    /**
     * Create container with specific LPN
     * POST /api/v1/inventory/containers/with-lpn
     */
    @PostMapping("/with-lpn")
    public ResponseEntity<String> createContainerWithLPN(
            @RequestParam String lpn,
            @RequestBody CreateContainerRequest request) {
        logger.info("Creating container with LPN: {}", lpn);

        Location location = Location.of(
            request.warehouseId(),
            request.zoneId(),
            request.aisleId(),
            request.shelfId(),
            request.binId(),
            LocationType.GENERAL
        );

        String createdLpn = containerService.createContainerWithLPN(
            lpn, request.type(), location, request.createdBy()
        );

        return ResponseEntity.ok(createdLpn);
    }

    /**
     * Add item to container
     * POST /api/v1/inventory/containers/{lpn}/items
     */
    @PostMapping("/{lpn}/items")
    public ResponseEntity<Void> addItem(
            @PathVariable String lpn,
            @RequestBody AddContainerItemRequest request) {
        logger.info("Adding item to container: LPN={}, SKU={}, qty={}",
                lpn, request.sku(), request.quantity());

        Location sourceLocation = Location.of(
            request.sourceWarehouseId(),
            request.sourceZoneId(),
            request.sourceAisleId(),
            request.sourceShelfId(),
            request.sourceBinId(),
            LocationType.GENERAL
        );

        containerService.addItem(
            lpn, request.sku(), request.quantity(), request.lotNumber(), sourceLocation
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Remove item from container
     * POST /api/v1/inventory/containers/{lpn}/items/remove
     */
    @PostMapping("/{lpn}/items/remove")
    public ResponseEntity<Void> removeItem(
            @PathVariable String lpn,
            @RequestParam String sku,
            @RequestParam int quantity,
            @RequestParam String lotNumber) {
        logger.info("Removing item from container: LPN={}, SKU={}, qty={}",
                lpn, sku, quantity);

        containerService.removeItem(lpn, sku, quantity, lotNumber);
        return ResponseEntity.ok().build();
    }

    /**
     * Move container to new location
     * POST /api/v1/inventory/containers/{lpn}/move
     */
    @PostMapping("/{lpn}/move")
    public ResponseEntity<Void> moveContainer(
            @PathVariable String lpn,
            @RequestParam String warehouseId,
            @RequestParam String zoneId,
            @RequestParam(required = false) String aisleId,
            @RequestParam(required = false) String rackId,
            @RequestParam(required = false) String shelfId,
            @RequestParam(required = false) String binId) {
        logger.info("Moving container: LPN={}, to warehouse={}, zone={}",
                lpn, warehouseId, zoneId);

        Location newLocation = Location.of(warehouseId, zoneId, aisleId, shelfId, binId, LocationType.GENERAL);
        containerService.moveContainer(lpn, newLocation);
        return ResponseEntity.ok().build();
    }

    /**
     * Close container
     * POST /api/v1/inventory/containers/{lpn}/close
     */
    @PostMapping("/{lpn}/close")
    public ResponseEntity<Void> closeContainer(@PathVariable String lpn) {
        logger.info("Closing container: LPN={}", lpn);

        containerService.closeContainer(lpn);
        return ResponseEntity.ok().build();
    }

    /**
     * Ship container
     * POST /api/v1/inventory/containers/{lpn}/ship
     */
    @PostMapping("/{lpn}/ship")
    public ResponseEntity<Void> shipContainer(@PathVariable String lpn) {
        logger.info("Shipping container: LPN={}", lpn);

        containerService.shipContainer(lpn);
        return ResponseEntity.ok().build();
    }

    /**
     * Empty container
     * POST /api/v1/inventory/containers/{lpn}/empty
     */
    @PostMapping("/{lpn}/empty")
    public ResponseEntity<Void> emptyContainer(@PathVariable String lpn) {
        logger.info("Emptying container: LPN={}", lpn);

        containerService.emptyContainer(lpn);
        return ResponseEntity.ok().build();
    }

    /**
     * Nest container inside parent
     * POST /api/v1/inventory/containers/{childLpn}/nest
     */
    @PostMapping("/{childLpn}/nest")
    public ResponseEntity<Void> nestContainer(
            @PathVariable String childLpn,
            @RequestParam String parentLpn) {
        logger.info("Nesting container: child={}, parent={}", childLpn, parentLpn);

        containerService.nestContainer(childLpn, parentLpn);
        return ResponseEntity.ok().build();
    }

    /**
     * Get container details
     * GET /api/v1/inventory/containers/{lpn}
     */
    @GetMapping("/{lpn}")
    public ResponseEntity<ContainerResponse> getContainer(@PathVariable String lpn) {
        logger.debug("Getting container: LPN={}", lpn);

        Container container = containerService.getContainer(lpn);
        return ResponseEntity.ok(ContainerResponse.fromDomain(container));
    }

    /**
     * Get active containers
     * GET /api/v1/inventory/containers/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<ContainerResponse>> getActiveContainers() {
        logger.debug("Getting active containers");

        List<ContainerResponse> containers = containerService.getActiveContainers()
            .stream()
            .map(ContainerResponse::fromDomain)
            .toList();

        return ResponseEntity.ok(containers);
    }

    /**
     * Get containers by type
     * GET /api/v1/inventory/containers/type/{type}
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<ContainerResponse>> getContainersByType(@PathVariable ContainerType type) {
        logger.debug("Getting containers by type: {}", type);

        List<ContainerResponse> containers = containerService.getContainersByType(type)
            .stream()
            .map(ContainerResponse::fromDomain)
            .toList();

        return ResponseEntity.ok(containers);
    }

    /**
     * Get containers at location
     * GET /api/v1/inventory/containers/location
     */
    @GetMapping("/location")
    public ResponseEntity<List<ContainerResponse>> getContainersAtLocation(
            @RequestParam String warehouseId,
            @RequestParam String zoneId) {
        logger.debug("Getting containers at location: warehouse={}, zone={}",
                warehouseId, zoneId);

        List<ContainerResponse> containers = containerService.getContainersAtLocation(warehouseId, zoneId)
            .stream()
            .map(ContainerResponse::fromDomain)
            .toList();

        return ResponseEntity.ok(containers);
    }

    /**
     * Get child containers (nested)
     * GET /api/v1/inventory/containers/{parentLpn}/children
     */
    @GetMapping("/{parentLpn}/children")
    public ResponseEntity<List<ContainerResponse>> getChildContainers(@PathVariable String parentLpn) {
        logger.debug("Getting child containers: parent={}", parentLpn);

        List<ContainerResponse> containers = containerService.getChildContainers(parentLpn)
            .stream()
            .map(ContainerResponse::fromDomain)
            .toList();

        return ResponseEntity.ok(containers);
    }

    /**
     * Get empty containers
     * GET /api/v1/inventory/containers/empty
     */
    @GetMapping("/empty")
    public ResponseEntity<List<ContainerResponse>> getEmptyContainers() {
        logger.debug("Getting empty containers");

        List<ContainerResponse> containers = containerService.getEmptyContainers()
            .stream()
            .map(ContainerResponse::fromDomain)
            .toList();

        return ResponseEntity.ok(containers);
    }

    /**
     * Check if container is at capacity
     * GET /api/v1/inventory/containers/{lpn}/at-capacity
     */
    @GetMapping("/{lpn}/at-capacity")
    public ResponseEntity<Boolean> isAtCapacity(@PathVariable String lpn) {
        logger.debug("Checking if container at capacity: LPN={}", lpn);

        boolean atCapacity = containerService.isAtCapacity(lpn);
        return ResponseEntity.ok(atCapacity);
    }

    /**
     * Check if container is mixed-SKU
     * GET /api/v1/inventory/containers/{lpn}/mixed-sku
     */
    @GetMapping("/{lpn}/mixed-sku")
    public ResponseEntity<Boolean> isMixedSKU(@PathVariable String lpn) {
        logger.debug("Checking if container mixed-SKU: LPN={}", lpn);

        boolean mixedSku = containerService.isMixedSKU(lpn);
        return ResponseEntity.ok(mixedSku);
    }

    /**
     * Count containers by status
     * GET /api/v1/inventory/containers/count/status/{status}
     */
    @GetMapping("/count/status/{status}")
    public ResponseEntity<Long> countByStatus(@PathVariable ContainerStatus status) {
        logger.debug("Counting containers by status: {}", status);

        long count = containerService.countByStatus(status);
        return ResponseEntity.ok(count);
    }

    /**
     * Count containers by type
     * GET /api/v1/inventory/containers/count/type/{type}
     */
    @GetMapping("/count/type/{type}")
    public ResponseEntity<Long> countByType(@PathVariable ContainerType type) {
        logger.debug("Counting containers by type: {}", type);

        long count = containerService.countByType(type);
        return ResponseEntity.ok(count);
    }
}
