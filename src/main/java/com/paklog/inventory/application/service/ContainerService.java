package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.Container;
import com.paklog.inventory.domain.model.ContainerStatus;
import com.paklog.inventory.domain.model.ContainerType;
import com.paklog.inventory.domain.model.Location;
import com.paklog.inventory.domain.repository.ContainerRepository;
import com.paklog.inventory.domain.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application service for container/LPN management.
 * Handles container lifecycle, item management, and tracking.
 */
@Service
public class ContainerService {

    private static final Logger logger = LoggerFactory.getLogger(ContainerService.class);

    private final ContainerRepository containerRepository;
    private final OutboxRepository outboxRepository;

    public ContainerService(ContainerRepository containerRepository,
                           OutboxRepository outboxRepository) {
        this.containerRepository = containerRepository;
        this.outboxRepository = outboxRepository;
    }

    /**
     * Create a new container
     */
    @Transactional
    public String createContainer(ContainerType type, Location location, String createdBy) {
        logger.info("Creating container: type={}, location={}, createdBy={}",
                type, location.toLocationCode(), createdBy);

        Container container = Container.create(type, location, createdBy);
        Container saved = containerRepository.save(container);

        logger.info("Container created: LPN={}, type={}", saved.getLpn(), type);
        return saved.getLpn();
    }

    /**
     * Create container with specific LPN (barcode integration)
     */
    @Transactional
    public String createContainerWithLPN(String lpn, ContainerType type,
                                        Location location, String createdBy) {
        logger.info("Creating container with LPN: lpn={}, type={}, location={}",
                lpn, type, location.toLocationCode());

        if (containerRepository.existsByLpn(lpn)) {
            throw new IllegalArgumentException("Container already exists with LPN: " + lpn);
        }

        Container container = Container.createWithLPN(lpn, type, location, createdBy);
        Container saved = containerRepository.save(container);

        logger.info("Container created: LPN={}, type={}", saved.getLpn(), type);
        return saved.getLpn();
    }

    /**
     * Add item to container
     */
    @Transactional
    public void addItem(String lpn, String sku, int quantity, String lotNumber, Location sourceLocation) {
        logger.info("Adding item to container: LPN={}, SKU={}, qty={}, lot={}",
                lpn, sku, quantity, lotNumber);

        Container container = containerRepository.findByLpn(lpn)
                .orElseThrow(() -> new IllegalArgumentException("Container not found: " + lpn));

        container.addItem(sku, quantity, lotNumber, sourceLocation);
        containerRepository.save(container);

        logger.info("Item added to container: LPN={}, SKU={}, totalItems={}, totalQty={}",
                lpn, sku, container.getItems().size(), container.getTotalQuantity());
    }

    /**
     * Remove item from container
     */
    @Transactional
    public void removeItem(String lpn, String sku, int quantity, String lotNumber) {
        logger.info("Removing item from container: LPN={}, SKU={}, qty={}, lot={}",
                lpn, sku, quantity, lotNumber);

        Container container = containerRepository.findByLpn(lpn)
                .orElseThrow(() -> new IllegalArgumentException("Container not found: " + lpn));

        container.removeItem(sku, quantity, lotNumber);
        containerRepository.save(container);

        logger.info("Item removed from container: LPN={}, SKU={}, remainingQty={}",
                lpn, sku, container.getTotalQuantity());
    }

    /**
     * Move container to a new location
     */
    @Transactional
    public void moveContainer(String lpn, Location newLocation) {
        logger.info("Moving container: LPN={}, newLocation={}", lpn, newLocation.toLocationCode());

        Container container = containerRepository.findByLpn(lpn)
                .orElseThrow(() -> new IllegalArgumentException("Container not found: " + lpn));

        container.moveTo(newLocation);
        containerRepository.save(container);

        logger.info("Container moved: LPN={}, location={}", lpn, newLocation.toLocationCode());
    }

    /**
     * Close container (no more items can be added)
     */
    @Transactional
    public void closeContainer(String lpn) {
        logger.info("Closing container: LPN={}", lpn);

        Container container = containerRepository.findByLpn(lpn)
                .orElseThrow(() -> new IllegalArgumentException("Container not found: " + lpn));

        container.close();
        containerRepository.save(container);

        logger.info("Container closed: LPN={}, totalItems={}, totalQty={}",
                lpn, container.getItems().size(), container.getTotalQuantity());
    }

    /**
     * Ship container
     */
    @Transactional
    public void shipContainer(String lpn) {
        logger.info("Shipping container: LPN={}", lpn);

        Container container = containerRepository.findByLpn(lpn)
                .orElseThrow(() -> new IllegalArgumentException("Container not found: " + lpn));

        container.ship();
        containerRepository.save(container);

        logger.info("Container shipped: LPN={}", lpn);
    }

    /**
     * Empty container (remove all items)
     */
    @Transactional
    public void emptyContainer(String lpn) {
        logger.info("Emptying container: LPN={}", lpn);

        Container container = containerRepository.findByLpn(lpn)
                .orElseThrow(() -> new IllegalArgumentException("Container not found: " + lpn));

        container.empty();
        containerRepository.save(container);

        logger.info("Container emptied: LPN={}", lpn);
    }

    /**
     * Nest container inside parent
     */
    @Transactional
    public void nestContainer(String childLpn, String parentLpn) {
        logger.info("Nesting container: child={}, parent={}", childLpn, parentLpn);

        Container child = containerRepository.findByLpn(childLpn)
                .orElseThrow(() -> new IllegalArgumentException("Child container not found: " + childLpn));

        if (!containerRepository.existsByLpn(parentLpn)) {
            throw new IllegalArgumentException("Parent container not found: " + parentLpn);
        }

        child.nestInside(parentLpn);
        containerRepository.save(child);

        logger.info("Container nested: child={}, parent={}", childLpn, parentLpn);
    }

    /**
     * Get container details
     */
    @Transactional(readOnly = true)
    public Container getContainer(String lpn) {
        return containerRepository.findByLpn(lpn)
                .orElseThrow(() -> new IllegalArgumentException("Container not found: " + lpn));
    }

    /**
     * Get active containers
     */
    @Transactional(readOnly = true)
    public List<Container> getActiveContainers() {
        return containerRepository.findActiveContainers();
    }

    /**
     * Get containers by type
     */
    @Transactional(readOnly = true)
    public List<Container> getContainersByType(ContainerType type) {
        return containerRepository.findByType(type);
    }

    /**
     * Get containers at a location
     */
    @Transactional(readOnly = true)
    public List<Container> getContainersAtLocation(String warehouseId, String zoneId) {
        return containerRepository.findByLocation(warehouseId, zoneId);
    }

    /**
     * Get child containers (nested)
     */
    @Transactional(readOnly = true)
    public List<Container> getChildContainers(String parentLpn) {
        return containerRepository.findByParentLpn(parentLpn);
    }

    /**
     * Get empty containers
     */
    @Transactional(readOnly = true)
    public List<Container> getEmptyContainers() {
        return containerRepository.findEmptyContainers();
    }

    /**
     * Check if container is at capacity
     */
    @Transactional(readOnly = true)
    public boolean isAtCapacity(String lpn) {
        Container container = getContainer(lpn);
        return container.isAtCapacity();
    }

    /**
     * Check if container is mixed-SKU
     */
    @Transactional(readOnly = true)
    public boolean isMixedSKU(String lpn) {
        Container container = getContainer(lpn);
        return container.isMixedSKU();
    }

    /**
     * Count containers by status
     */
    @Transactional(readOnly = true)
    public long countByStatus(ContainerStatus status) {
        return containerRepository.countByStatus(status);
    }

    /**
     * Count containers by type
     */
    @Transactional(readOnly = true)
    public long countByType(ContainerType type) {
        return containerRepository.countByType(type);
    }
}
