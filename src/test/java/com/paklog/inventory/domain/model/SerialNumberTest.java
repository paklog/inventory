package com.paklog.inventory.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static com.paklog.inventory.domain.model.SerialStatus.*;

/**
 * Tests for SerialNumber - validates serial tracking and lifecycle.
 */
class SerialNumberTest {

    @Test
    @DisplayName("Should create serial number with warranty")
    void shouldCreateSerialNumberWithWarranty() {
        // Given
        Location location = Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL);
        LocalDate manufactureDate = LocalDate.of(2024, 1, 15);
        LocalDate warrantyExpiry = LocalDate.of(2027, 1, 15);

        // When
        SerialNumber serial = SerialNumber.create("SKU-LAPTOP-001", "SN123456",
            location, manufactureDate, warrantyExpiry);

        // Then
        assertEquals("SN123456", serial.getSerialNumber());
        assertEquals("SKU-LAPTOP-001", serial.getSku());
        assertEquals(SerialStatus.IN_INVENTORY, serial.getStatus());
        assertEquals(location, serial.getCurrentLocation());
        assertEquals(manufactureDate, serial.getManufactureDate());
        assertEquals(warrantyExpiry, serial.getWarrantyExpiryDate());
    }

    @Test
    @DisplayName("Should allocate serial number to customer")
    void shouldAllocateSerialNumberToCustomer() {
        // Given
        SerialNumber serial = createTestSerial();

        // When
        SerialNumber allocated = serial.allocate();

        // Then
        assertEquals(SerialStatus.ALLOCATED, allocated.getStatus());
        assertNotNull(allocated.getAllocatedAt());
    }

    @Test
    @DisplayName("Should ship allocated serial")
    void shouldShipAllocatedSerial() {
        // Given
        SerialNumber serial = createTestSerial().allocate();

        // When
        SerialNumber shipped = serial.ship();

        // Then
        assertEquals(SerialStatus.SHIPPED, shipped.getStatus());
    }

    @Test
    @DisplayName("Should move serial to new location")
    void shouldMoveSerialToNewLocation() {
        // Given
        SerialNumber serial = createTestSerial();
        Location newLocation = Location.of("WH01", "B", "05", "03", "C", LocationType.GENERAL);

        // When
        SerialNumber moved = serial.moveTo(newLocation);

        // Then
        assertEquals(newLocation, moved.getCurrentLocation());
    }

    @Test
    @DisplayName("Should change serial status to damaged")
    void shouldChangeSerialStatusToDamaged() {
        // Given
        SerialNumber serial = createTestSerial();

        // When
        SerialNumber damaged = serial.changeStatus(SerialStatus.DAMAGED);

        // Then
        assertEquals(SerialStatus.DAMAGED, damaged.getStatus());
    }

    @Test
    @DisplayName("Should change serial status to quarantine")
    void shouldChangeSerialStatusToQuarantine() {
        // Given
        SerialNumber serial = createTestSerial();

        // When
        SerialNumber quarantined = serial.changeStatus(SerialStatus.QUARANTINE);

        // Then
        assertEquals(SerialStatus.QUARANTINE, quarantined.getStatus());
    }

    @Test
    @DisplayName("Should check warranty validity for valid warranty")
    void shouldCheckWarrantyValidityForValidWarranty() {
        // Given: Warranty expires in future
        SerialNumber serial = SerialNumber.create("SKU-001", "SN001",
            Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL),
            LocalDate.now().minusYears(1),
            LocalDate.now().plusYears(2));

        // When
        boolean isValid = serial.isWarrantyValid();

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should check warranty validity for expired warranty")
    void shouldCheckWarrantyValidityForExpiredWarranty() {
        // Given: Warranty expired yesterday
        SerialNumber serial = SerialNumber.create("SKU-001", "SN001",
            Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL),
            LocalDate.now().minusYears(3),
            LocalDate.now().minusDays(1));

        // When
        boolean isValid = serial.isWarrantyValid();

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should track serial number lifecycle")
    void shouldTrackSerialNumberLifecycle() {
        // Given
        SerialNumber serial = createTestSerial();

        // When: Complete lifecycle
        serial = serial.allocate();
        serial = serial.ship();

        // Then
        assertEquals(SerialStatus.SHIPPED, serial.getStatus());
        assertNotNull(serial.getAllocatedAt());
    }

    private SerialNumber createTestSerial() {
        Location location = Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL);
        LocalDate manufactureDate = LocalDate.of(2024, 1, 15);
        LocalDate warrantyExpiry = LocalDate.of(2027, 1, 15);
        return SerialNumber.create("SKU-LAPTOP-001", "SN123456",
            location, manufactureDate, warrantyExpiry);
    }
}
