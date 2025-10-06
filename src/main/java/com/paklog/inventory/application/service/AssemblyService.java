package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.*;
import com.paklog.inventory.domain.repository.AssemblyOrderRepository;
import com.paklog.inventory.domain.model.OutboxEvent;
import com.paklog.inventory.domain.repository.KitRepository;
import com.paklog.inventory.domain.model.OutboxEvent;
import com.paklog.inventory.domain.repository.OutboxRepository;
import com.paklog.inventory.domain.model.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service for kit assembly/disassembly operations.
 * Handles assembly orders and component allocation.
 */
@Service
public class AssemblyService {

    private static final Logger logger = LoggerFactory.getLogger(AssemblyService.class);

    private final AssemblyOrderRepository assemblyOrderRepository;
    private final KitRepository kitRepository;
    private final OutboxRepository outboxRepository;

    public AssemblyService(AssemblyOrderRepository assemblyOrderRepository,
                          KitRepository kitRepository,
                          OutboxRepository outboxRepository) {
        this.assemblyOrderRepository = assemblyOrderRepository;
        this.kitRepository = kitRepository;
        this.outboxRepository = outboxRepository;
    }

    /**
     * Create assembly order (components → kit)
     */
    @Transactional
    public String createAssemblyOrder(String kitSku, int kitQuantity,
                                     List<ComponentAllocation> componentAllocations,
                                     Location assemblyLocation, String createdBy) {
        logger.info("Creating assembly order: kitSku={}, qty={}, location={}",
                kitSku, kitQuantity, assemblyLocation.toLocationCode());

        // Validate kit exists
        Kit kit = kitRepository.findByKitSku(kitSku)
            .orElseThrow(() -> new IllegalArgumentException("Kit not found: " + kitSku));

        if (!kit.isPhysical()) {
            throw new IllegalArgumentException("Cannot assemble virtual kit: " + kitSku);
        }

        String assemblyOrderId = "ASM-" + UUID.randomUUID().toString();

        AssemblyOrder order = AssemblyOrder.createAssembly(
            assemblyOrderId, kitSku, kitQuantity, componentAllocations,
            assemblyLocation, createdBy
        );

        // Validate all components are allocated
        if (!order.hasAllComponents(kit)) {
            logger.warn("Assembly order created with missing components: orderId={}",
                    assemblyOrderId);
        }

        assemblyOrderRepository.save(order);

        logger.info("Assembly order created: orderId={}, kitSku={}, qty={}",
                assemblyOrderId, kitSku, kitQuantity);

        return assemblyOrderId;
    }

    /**
     * Create disassembly order (kit → components)
     */
    @Transactional
    public String createDisassemblyOrder(String kitSku, int kitQuantity,
                                        Location assemblyLocation, String createdBy) {
        logger.info("Creating disassembly order: kitSku={}, qty={}, location={}",
                kitSku, kitQuantity, assemblyLocation.toLocationCode());

        // Validate kit exists
        kitRepository.findByKitSku(kitSku)
            .orElseThrow(() -> new IllegalArgumentException("Kit not found: " + kitSku));

        String assemblyOrderId = "DIS-" + UUID.randomUUID().toString();

        AssemblyOrder order = AssemblyOrder.createDisassembly(
            assemblyOrderId, kitSku, kitQuantity, assemblyLocation, createdBy
        );

        assemblyOrderRepository.save(order);

        logger.info("Disassembly order created: orderId={}, kitSku={}, qty={}",
                assemblyOrderId, kitSku, kitQuantity);

        return assemblyOrderId;
    }

    /**
     * Start assembly/disassembly work
     */
    @Transactional
    public void startOrder(String assemblyOrderId) {
        logger.info("Starting assembly order: orderId={}", assemblyOrderId);

        AssemblyOrder order = assemblyOrderRepository.findById(assemblyOrderId)
            .orElseThrow(() -> new IllegalArgumentException("Assembly order not found: " + assemblyOrderId));

        order.start();
        assemblyOrderRepository.save(order);

        logger.info("Assembly order started: orderId={}", assemblyOrderId);
    }

    /**
     * Complete assembly/disassembly
     */
    @Transactional
    public void completeOrder(String assemblyOrderId, int actualQuantityProduced, String completedBy) {
        logger.info("Completing assembly order: orderId={}, actualQty={}",
                assemblyOrderId, actualQuantityProduced);

        AssemblyOrder order = assemblyOrderRepository.findById(assemblyOrderId)
            .orElseThrow(() -> new IllegalArgumentException("Assembly order not found: " + assemblyOrderId));

        order.complete(actualQuantityProduced, completedBy);
        assemblyOrderRepository.save(order);
        outboxRepository.saveAll(order.getUncommittedEvents().stream().map(OutboxEvent::from).toList());
        order.markEventsAsCommitted();

        double yield = order.getYieldPercentage();
        logger.info("Assembly order completed: orderId={}, actualQty={}, yield={}%",
                assemblyOrderId, actualQuantityProduced, String.format("%.2f", yield));

        if (yield < 100.0) {
            logger.warn("Assembly order completed with less than 100% yield: orderId={}, yield={}%",
                    assemblyOrderId, String.format("%.2f", yield));
        }
    }

    /**
     * Cancel assembly order
     */
    @Transactional
    public void cancelOrder(String assemblyOrderId, String reason) {
        logger.info("Cancelling assembly order: orderId={}, reason={}", assemblyOrderId, reason);

        AssemblyOrder order = assemblyOrderRepository.findById(assemblyOrderId)
            .orElseThrow(() -> new IllegalArgumentException("Assembly order not found: " + assemblyOrderId));

        order.cancel(reason);
        assemblyOrderRepository.save(order);

        logger.info("Assembly order cancelled: orderId={}", assemblyOrderId);
    }

    /**
     * Add component allocation to assembly order
     */
    @Transactional
    public void addComponentAllocation(String assemblyOrderId, ComponentAllocation allocation) {
        logger.info("Adding component allocation: orderId={}, component={}",
                assemblyOrderId, allocation.getComponentSku());

        AssemblyOrder order = assemblyOrderRepository.findById(assemblyOrderId)
            .orElseThrow(() -> new IllegalArgumentException("Assembly order not found: " + assemblyOrderId));

        order.addComponentAllocation(allocation);
        assemblyOrderRepository.save(order);

        logger.info("Component allocation added: orderId={}, component={}",
                assemblyOrderId, allocation.getComponentSku());
    }

    /**
     * Get assembly order details
     */
    @Transactional(readOnly = true)
    public AssemblyOrder getOrder(String assemblyOrderId) {
        return assemblyOrderRepository.findById(assemblyOrderId)
            .orElseThrow(() -> new IllegalArgumentException("Assembly order not found: " + assemblyOrderId));
    }

    /**
     * Get orders by kit SKU
     */
    @Transactional(readOnly = true)
    public List<AssemblyOrder> getOrdersByKitSku(String kitSku) {
        return assemblyOrderRepository.findByKitSku(kitSku);
    }

    /**
     * Get orders by status
     */
    @Transactional(readOnly = true)
    public List<AssemblyOrder> getOrdersByStatus(AssemblyStatus status) {
        return assemblyOrderRepository.findByStatus(status);
    }

    /**
     * Get orders by type
     */
    @Transactional(readOnly = true)
    public List<AssemblyOrder> getOrdersByType(AssemblyType type) {
        return assemblyOrderRepository.findByType(type);
    }

    /**
     * Get in-progress orders
     */
    @Transactional(readOnly = true)
    public List<AssemblyOrder> getInProgressOrders() {
        return assemblyOrderRepository.findInProgressOrders();
    }

    /**
     * Count orders by status
     */
    @Transactional(readOnly = true)
    public long countOrdersByStatus(AssemblyStatus status) {
        return assemblyOrderRepository.countByStatus(status);
    }
}
