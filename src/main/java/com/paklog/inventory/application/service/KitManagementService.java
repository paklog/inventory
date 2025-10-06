package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.Kit;
import com.paklog.inventory.domain.model.KitComponent;
import com.paklog.inventory.domain.model.KitType;
import com.paklog.inventory.domain.repository.KitRepository;
import com.paklog.inventory.domain.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Application service for kit/BOM management.
 * Handles kit definitions, components, and availability calculations.
 */
@Service
public class KitManagementService {

    private static final Logger logger = LoggerFactory.getLogger(KitManagementService.class);

    private final KitRepository kitRepository;
    private final OutboxRepository outboxRepository;

    public KitManagementService(KitRepository kitRepository,
                               OutboxRepository outboxRepository) {
        this.kitRepository = kitRepository;
        this.outboxRepository = outboxRepository;
    }

    /**
     * Create a new kit
     */
    @Transactional
    public void createKit(String kitSku, String kitDescription, List<KitComponent> components,
                         KitType kitType, boolean allowPartialKit) {
        logger.info("Creating kit: kitSku={}, type={}, components={}",
                kitSku, kitType, components.size());

        if (kitRepository.existsByKitSku(kitSku)) {
            throw new IllegalArgumentException("Kit already exists: " + kitSku);
        }

        Kit kit = Kit.create(kitSku, kitDescription, components, kitType, allowPartialKit);
        kitRepository.save(kit);

        logger.info("Kit created: kitSku={}, type={}, components={}",
                kitSku, kitType, components.size());
    }

    /**
     * Create physical kit (requires assembly)
     */
    @Transactional
    public void createPhysicalKit(String kitSku, String kitDescription, List<KitComponent> components) {
        logger.info("Creating physical kit: kitSku={}, components={}", kitSku, components.size());

        Kit kit = Kit.physical(kitSku, kitDescription, components);
        kitRepository.save(kit);

        logger.info("Physical kit created: kitSku={}", kitSku);
    }

    /**
     * Create virtual kit (logical grouping)
     */
    @Transactional
    public void createVirtualKit(String kitSku, String kitDescription, List<KitComponent> components,
                                boolean allowPartialKit) {
        logger.info("Creating virtual kit: kitSku={}, components={}, allowPartial={}",
                kitSku, components.size(), allowPartialKit);

        Kit kit = Kit.virtual(kitSku, kitDescription, components, allowPartialKit);
        kitRepository.save(kit);

        logger.info("Virtual kit created: kitSku={}", kitSku);
    }

    /**
     * Add component to kit
     */
    @Transactional
    public void addComponent(String kitSku, KitComponent component) {
        logger.info("Adding component to kit: kitSku={}, component={}",
                kitSku, component.getComponentSku());

        Kit kit = kitRepository.findByKitSku(kitSku)
            .orElseThrow(() -> new IllegalArgumentException("Kit not found: " + kitSku));

        kit.addComponent(component);
        kitRepository.save(kit);

        logger.info("Component added to kit: kitSku={}, component={}",
                kitSku, component.getComponentSku());
    }

    /**
     * Remove component from kit
     */
    @Transactional
    public void removeComponent(String kitSku, String componentSku) {
        logger.info("Removing component from kit: kitSku={}, component={}",
                kitSku, componentSku);

        Kit kit = kitRepository.findByKitSku(kitSku)
            .orElseThrow(() -> new IllegalArgumentException("Kit not found: " + kitSku));

        kit.removeComponent(componentSku);
        kitRepository.save(kit);

        logger.info("Component removed from kit: kitSku={}, component={}", kitSku, componentSku);
    }

    /**
     * Update component quantity
     */
    @Transactional
    public void updateComponentQuantity(String kitSku, String componentSku, int newQuantity) {
        logger.info("Updating component quantity: kitSku={}, component={}, newQty={}",
                kitSku, componentSku, newQuantity);

        Kit kit = kitRepository.findByKitSku(kitSku)
            .orElseThrow(() -> new IllegalArgumentException("Kit not found: " + kitSku));

        kit.updateComponentQuantity(componentSku, newQuantity);
        kitRepository.save(kit);

        logger.info("Component quantity updated: kitSku={}, component={}, newQty={}",
                kitSku, componentSku, newQuantity);
    }

    /**
     * Calculate available kits based on component inventory
     */
    @Transactional(readOnly = true)
    public int calculateAvailableKits(String kitSku, Map<String, Integer> componentInventory) {
        logger.debug("Calculating available kits: kitSku={}", kitSku);

        Kit kit = kitRepository.findByKitSku(kitSku)
            .orElseThrow(() -> new IllegalArgumentException("Kit not found: " + kitSku));

        int availableKits = kit.calculateAvailableKits(componentInventory);

        logger.debug("Available kits calculated: kitSku={}, available={}", kitSku, availableKits);
        return availableKits;
    }

    /**
     * Check if kit can be assembled
     */
    @Transactional(readOnly = true)
    public boolean canAssemble(String kitSku, Map<String, Integer> componentInventory, int kitQuantity) {
        Kit kit = kitRepository.findByKitSku(kitSku)
            .orElseThrow(() -> new IllegalArgumentException("Kit not found: " + kitSku));

        return kit.canAssemble(componentInventory, kitQuantity);
    }

    /**
     * Get component shortages for kit assembly
     */
    @Transactional(readOnly = true)
    public Map<String, Integer> getShortages(String kitSku, Map<String, Integer> componentInventory,
                                            int kitQuantity) {
        Kit kit = kitRepository.findByKitSku(kitSku)
            .orElseThrow(() -> new IllegalArgumentException("Kit not found: " + kitSku));

        return kit.getShortages(componentInventory, kitQuantity);
    }

    /**
     * Deactivate kit
     */
    @Transactional
    public void deactivateKit(String kitSku) {
        logger.info("Deactivating kit: kitSku={}", kitSku);

        Kit kit = kitRepository.findByKitSku(kitSku)
            .orElseThrow(() -> new IllegalArgumentException("Kit not found: " + kitSku));

        kit.deactivate();
        kitRepository.save(kit);

        logger.info("Kit deactivated: kitSku={}", kitSku);
    }

    /**
     * Activate kit
     */
    @Transactional
    public void activateKit(String kitSku) {
        logger.info("Activating kit: kitSku={}", kitSku);

        Kit kit = kitRepository.findByKitSku(kitSku)
            .orElseThrow(() -> new IllegalArgumentException("Kit not found: " + kitSku));

        kit.activate();
        kitRepository.save(kit);

        logger.info("Kit activated: kitSku={}", kitSku);
    }

    /**
     * Get kit details
     */
    @Transactional(readOnly = true)
    public Kit getKit(String kitSku) {
        return kitRepository.findByKitSku(kitSku)
            .orElseThrow(() -> new IllegalArgumentException("Kit not found: " + kitSku));
    }

    /**
     * Get all active kits
     */
    @Transactional(readOnly = true)
    public List<Kit> getActiveKits() {
        return kitRepository.findActiveKits();
    }

    /**
     * Get kits by type
     */
    @Transactional(readOnly = true)
    public List<Kit> getKitsByType(KitType kitType) {
        return kitRepository.findByType(kitType);
    }

    /**
     * Find kits containing a component
     */
    @Transactional(readOnly = true)
    public List<Kit> getKitsContainingComponent(String componentSku) {
        return kitRepository.findKitsContainingComponent(componentSku);
    }
}
