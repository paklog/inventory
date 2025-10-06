package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.Location;
import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.model.SerialNumber;
import com.paklog.inventory.domain.model.SerialStatus;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import com.paklog.inventory.domain.model.OutboxEvent;
import com.paklog.inventory.domain.repository.SerialNumberRepository;
import com.paklog.inventory.domain.model.OutboxEvent;
import com.paklog.inventory.domain.repository.OutboxRepository;
import com.paklog.inventory.domain.model.OutboxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Application service for serial number tracking.
 * Handles serial number lifecycle and operations.
 */
@Service
public class SerialNumberService {

    private static final Logger logger = LoggerFactory.getLogger(SerialNumberService.class);

    private final SerialNumberRepository serialNumberRepository;
    private final ProductStockRepository productStockRepository;
    private final OutboxRepository outboxRepository;

    public SerialNumberService(SerialNumberRepository serialNumberRepository,
                              ProductStockRepository productStockRepository,
                              OutboxRepository outboxRepository) {
        this.serialNumberRepository = serialNumberRepository;
        this.productStockRepository = productStockRepository;
        this.outboxRepository = outboxRepository;
    }

    /**
     * Receive a serial-tracked item
     */
    @Transactional
    public void receiveSerialNumber(String sku, String serialNumber, Location location,
                                   LocalDate manufactureDate, LocalDate warrantyExpiryDate) {
        logger.info("Receiving serial number: SKU={}, serial={}, location={}",
                sku, serialNumber, location.toLocationCode());

        // Check if serial already exists
        if (serialNumberRepository.existsBySerialNumber(serialNumber)) {
            throw new IllegalArgumentException("Serial number already exists: " + serialNumber);
        }

        // Get or create product stock
        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseGet(() -> ProductStock.createWithSerialTracking(sku));

        // Create serial number with the provided location
        SerialNumber serial = SerialNumber.create(serialNumber, sku, location,
                manufactureDate, warrantyExpiryDate);

        // Add to product stock and save
        productStock.receiveSerialNumber(serial);
        serialNumberRepository.save(serial);
        productStockRepository.save(productStock);
        outboxRepository.saveAll(productStock.getUncommittedEvents().stream().map(OutboxEvent::from).toList());
        productStock.markEventsAsCommitted();

        logger.info("Serial number received: SKU={}, serial={}, totalQty={}",
                sku, serialNumber, productStock.getQuantityOnHand());
    }

    /**
     * Allocate a specific serial number to a customer
     */
    @Transactional
    public void allocateSerialNumber(String sku, String serialNumber, String customerId) {
        logger.info("Allocating serial number: SKU={}, serial={}, customer={}",
                sku, serialNumber, customerId);

        ProductStock productStock = productStockRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + sku));

        SerialNumber serial = serialNumberRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("Serial number not found: " + serialNumber));

        // Allocate in product stock (updates quantity)
        productStock.allocateSerialNumber(serialNumber, customerId);

        // Update serial status (allocate() takes no arguments, use ship() for customer assignment)
        SerialNumber allocatedSerial = serial.allocate();
        serialNumberRepository.save(allocatedSerial);

        productStockRepository.save(productStock);
        outboxRepository.saveAll(productStock.getUncommittedEvents().stream().map(OutboxEvent::from).toList());
        productStock.markEventsAsCommitted();

        logger.info("Serial number allocated: serial={}, customer={}", serialNumber, customerId);
    }

    /**
     * Ship a serial number
     */
    @Transactional
    public void shipSerialNumber(String serialNumber) {
        logger.info("Shipping serial number: {}", serialNumber);

        SerialNumber serial = serialNumberRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("Serial number not found: " + serialNumber));

        SerialNumber shippedSerial = serial.ship();
        serialNumberRepository.save(shippedSerial);

        logger.info("Serial number shipped: {}", serialNumber);
    }

    /**
     * Move serial number to a new location
     */
    @Transactional
    public void moveSerialNumber(String serialNumber, Location newLocation) {
        logger.info("Moving serial number: serial={}, newLocation={}",
                serialNumber, newLocation.toLocationCode());

        SerialNumber serial = serialNumberRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("Serial number not found: " + serialNumber));

        SerialNumber movedSerial = serial.moveTo(newLocation);
        serialNumberRepository.save(movedSerial);

        logger.info("Serial number moved: serial={}, location={}",
                serialNumber, newLocation.toLocationCode());
    }

    /**
     * Change serial number status (e.g., to DAMAGED, QUARANTINE)
     */
    @Transactional
    public void changeSerialStatus(String serialNumber, SerialStatus newStatus) {
        logger.info("Changing serial status: serial={}, newStatus={}", serialNumber, newStatus);

        SerialNumber serial = serialNumberRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("Serial number not found: " + serialNumber));

        SerialNumber updatedSerial = serial.changeStatus(newStatus);
        serialNumberRepository.save(updatedSerial);

        logger.info("Serial status changed: serial={}, status={}", serialNumber, newStatus);
    }

    /**
     * Get all available serial numbers for a SKU
     */
    @Transactional(readOnly = true)
    public List<SerialNumber> getAvailableSerials(String sku) {
        return serialNumberRepository.findAvailableBySku(sku);
    }

    /**
     * Get all serial numbers for a SKU
     */
    @Transactional(readOnly = true)
    public List<SerialNumber> getAllSerials(String sku) {
        return serialNumberRepository.findBySku(sku);
    }

    /**
     * Get serial numbers allocated to a customer
     */
    @Transactional(readOnly = true)
    public List<SerialNumber> getCustomerSerials(String customerId) {
        return serialNumberRepository.findByCustomerId(customerId);
    }

    /**
     * Get serial number details
     */
    @Transactional(readOnly = true)
    public SerialNumber getSerialNumber(String serialNumber) {
        return serialNumberRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("Serial number not found: " + serialNumber));
    }

    /**
     * Check warranty status
     */
    @Transactional(readOnly = true)
    public boolean isWarrantyValid(String serialNumber) {
        SerialNumber serial = getSerialNumber(serialNumber);
        return serial.isWarrantyValid();
    }
}
