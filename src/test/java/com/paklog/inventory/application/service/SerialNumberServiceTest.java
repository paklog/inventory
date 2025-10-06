package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.Location;
import com.paklog.inventory.domain.model.LocationType;
import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.model.SerialNumber;
import com.paklog.inventory.domain.model.SerialStatus;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import com.paklog.inventory.domain.repository.SerialNumberRepository;
import com.paklog.inventory.domain.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for SerialNumberService - critical for traceability and compliance.
 * Validates serial number lifecycle, allocation, movement, and warranty tracking.
 */
@ExtendWith(MockitoExtension.class)
class SerialNumberServiceTest {

    @Mock
    private SerialNumberRepository serialNumberRepository;

    @Mock
    private ProductStockRepository productStockRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private SerialNumberService serialNumberService;

    private String testSku;
    private String testSerialNumber;
    private Location testLocation;
    private LocalDate manufactureDate;
    private LocalDate warrantyExpiryDate;

    @BeforeEach
    void setUp() {
        testSku = "SKU-LAPTOP-001";
        testSerialNumber = "SN123456789";
        testLocation = Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL);
        manufactureDate = LocalDate.of(2024, 1, 15);
        warrantyExpiryDate = LocalDate.of(2027, 1, 15); // 3-year warranty
    }

    @Test
    @DisplayName("Should receive new serial number and create stock")
    void shouldReceiveNewSerialNumberAndCreateStock() {
        // Given: New SKU with no existing stock
        when(serialNumberRepository.existsBySerialNumber(testSerialNumber)).thenReturn(false);
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.empty());

        // When
        serialNumberService.receiveSerialNumber(
            testSku, testSerialNumber, testLocation, manufactureDate, warrantyExpiryDate
        );

        // Then
        verify(serialNumberRepository).save(any(SerialNumber.class));
        verify(productStockRepository).save(any(ProductStock.class));
        verify(outboxRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should receive serial number for existing stock")
    void shouldReceiveSerialNumberForExistingStock() {
        // Given: Existing stock
        ProductStock productStock = ProductStock.createWithSerialTracking(testSku);

        when(serialNumberRepository.existsBySerialNumber(testSerialNumber)).thenReturn(false);
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));

        // When
        serialNumberService.receiveSerialNumber(
            testSku, testSerialNumber, testLocation, manufactureDate, warrantyExpiryDate
        );

        // Then
        verify(serialNumberRepository).save(argThat(serial ->
            serial.getSerialNumber().equals(testSerialNumber) &&
            serial.getSku().equals(testSku) &&
            serial.getStatus() == SerialStatus.IN_INVENTORY
        ));
        verify(productStockRepository).save(productStock);
    }

    @Test
    @DisplayName("Should throw exception when receiving duplicate serial number")
    void shouldThrowExceptionWhenReceivingDuplicateSerialNumber() {
        // Given: Serial number already exists
        when(serialNumberRepository.existsBySerialNumber(testSerialNumber)).thenReturn(true);

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            serialNumberService.receiveSerialNumber(
                testSku, testSerialNumber, testLocation, manufactureDate, warrantyExpiryDate
            )
        );

        // Verify no save operations
        verify(serialNumberRepository, never()).save(any());
        verify(productStockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should allocate serial number to customer")
    void shouldAllocateSerialNumberToCustomer() {
        // Given: Available serial number
        ProductStock productStock = ProductStock.createWithSerialTracking(testSku);
        SerialNumber serial = SerialNumber.create(testSku, testSerialNumber, testLocation,
            manufactureDate, warrantyExpiryDate);

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));
        when(serialNumberRepository.findBySerialNumber(testSerialNumber)).thenReturn(Optional.of(serial));

        String customerId = "CUST-12345";

        // When
        serialNumberService.allocateSerialNumber(testSku, testSerialNumber, customerId);

        // Then
        verify(serialNumberRepository).save(argThat(s ->
            s.getSerialNumber().equals(testSerialNumber) &&
            s.getStatus() == SerialStatus.ALLOCATED &&
            s.getCustomerId().equals(customerId)
        ));
        verify(productStockRepository).save(productStock);
        verify(outboxRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should throw exception when allocating non-existent serial number")
    void shouldThrowExceptionWhenAllocatingNonExistentSerialNumber() {
        // Given
        ProductStock productStock = ProductStock.createWithSerialTracking(testSku);
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));
        when(serialNumberRepository.findBySerialNumber(testSerialNumber)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            serialNumberService.allocateSerialNumber(testSku, testSerialNumber, "CUST-001")
        );
    }

    @Test
    @DisplayName("Should throw exception when allocating from non-existent SKU")
    void shouldThrowExceptionWhenAllocatingFromNonExistentSku() {
        // Given
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            serialNumberService.allocateSerialNumber(testSku, testSerialNumber, "CUST-001")
        );

        verify(serialNumberRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should ship allocated serial number")
    void shouldShipAllocatedSerialNumber() {
        // Given: Allocated serial
        SerialNumber allocatedSerial = SerialNumber.create(testSku, testSerialNumber, testLocation,
            manufactureDate, warrantyExpiryDate);
        SerialNumber allocated = allocatedSerial.allocate();

        when(serialNumberRepository.findBySerialNumber(testSerialNumber)).thenReturn(Optional.of(allocated));

        // When
        serialNumberService.shipSerialNumber(testSerialNumber);

        // Then
        verify(serialNumberRepository).save(argThat(serial ->
            serial.getStatus() == SerialStatus.SHIPPED
        ));
    }

    @Test
    @DisplayName("Should throw exception when shipping non-existent serial")
    void shouldThrowExceptionWhenShippingNonExistentSerial() {
        // Given
        when(serialNumberRepository.findBySerialNumber(testSerialNumber)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            serialNumberService.shipSerialNumber(testSerialNumber)
        );
    }

    @Test
    @DisplayName("Should move serial number to new location")
    void shouldMoveSerialNumberToNewLocation() {
        // Given
        SerialNumber serial = SerialNumber.create(testSku, testSerialNumber, testLocation,
            manufactureDate, warrantyExpiryDate);

        Location newLocation = Location.of("WH01", "B", "05", "03", "C", LocationType.GENERAL);

        when(serialNumberRepository.findBySerialNumber(testSerialNumber)).thenReturn(Optional.of(serial));

        // When
        serialNumberService.moveSerialNumber(testSerialNumber, newLocation);

        // Then
        verify(serialNumberRepository).save(argThat(s ->
            s.getCurrentLocation().equals(newLocation)
        ));
    }

    @Test
    @DisplayName("Should change serial status to DAMAGED")
    void shouldChangeSerialStatusToDamaged() {
        // Given
        SerialNumber serial = SerialNumber.create(testSku, testSerialNumber, testLocation,
            manufactureDate, warrantyExpiryDate);

        when(serialNumberRepository.findBySerialNumber(testSerialNumber)).thenReturn(Optional.of(serial));

        // When
        serialNumberService.changeSerialStatus(testSerialNumber, SerialStatus.DAMAGED);

        // Then
        verify(serialNumberRepository).save(argThat(s ->
            s.getStatus() == SerialStatus.DAMAGED
        ));
    }

    @Test
    @DisplayName("Should change serial status to QUARANTINE")
    void shouldChangeSerialStatusToQuarantine() {
        // Given
        SerialNumber serial = SerialNumber.create(testSku, testSerialNumber, testLocation,
            manufactureDate, warrantyExpiryDate);

        when(serialNumberRepository.findBySerialNumber(testSerialNumber)).thenReturn(Optional.of(serial));

        // When
        serialNumberService.changeSerialStatus(testSerialNumber, SerialStatus.QUARANTINE);

        // Then
        verify(serialNumberRepository).save(argThat(s ->
            s.getStatus() == SerialStatus.QUARANTINE
        ));
    }

    @Test
    @DisplayName("Should get all available serials for a SKU")
    void shouldGetAllAvailableSerialsForSku() {
        // Given
        SerialNumber serial1 = SerialNumber.create(testSku, "SN001", testLocation,
            manufactureDate, warrantyExpiryDate);
        SerialNumber serial2 = SerialNumber.create(testSku, "SN002", testLocation,
            manufactureDate, warrantyExpiryDate);

        when(serialNumberRepository.findAvailableBySku(testSku))
            .thenReturn(List.of(serial1, serial2));

        // When
        List<SerialNumber> availableSerials = serialNumberService.getAvailableSerials(testSku);

        // Then
        assertEquals(2, availableSerials.size());
        assertTrue(availableSerials.stream().allMatch(s -> s.getStatus() == SerialStatus.IN_INVENTORY));
    }

    @Test
    @DisplayName("Should get all serials for a SKU (including allocated and shipped)")
    void shouldGetAllSerialsForSku() {
        // Given
        SerialNumber serial1 = SerialNumber.create(testSku, "SN001", testLocation,
            manufactureDate, warrantyExpiryDate);
        SerialNumber serial2 = SerialNumber.create(testSku, "SN002", testLocation,
            manufactureDate, warrantyExpiryDate).allocate();
        SerialNumber serial3 = SerialNumber.create(testSku, "SN003", testLocation,
            manufactureDate, warrantyExpiryDate).ship();

        when(serialNumberRepository.findBySku(testSku))
            .thenReturn(List.of(serial1, serial2, serial3));

        // When
        List<SerialNumber> allSerials = serialNumberService.getAllSerials(testSku);

        // Then
        assertEquals(3, allSerials.size());
    }

    @Test
    @DisplayName("Should get serials allocated to specific customer")
    void shouldGetSerialsAllocatedToSpecificCustomer() {
        // Given
        String customerId = "CUST-12345";
        SerialNumber serial1 = SerialNumber.create(testSku, "SN001", testLocation,
            manufactureDate, warrantyExpiryDate).allocate();
        SerialNumber serial2 = SerialNumber.create(testSku, "SN002", testLocation,
            manufactureDate, warrantyExpiryDate).allocate();

        when(serialNumberRepository.findByCustomerId(customerId))
            .thenReturn(List.of(serial1, serial2));

        // When
        List<SerialNumber> customerSerials = serialNumberService.getCustomerSerials(customerId);

        // Then
        assertEquals(2, customerSerials.size());
        assertTrue(customerSerials.stream().allMatch(s ->
            s.getCustomerId().equals(customerId)
        ));
    }

    @Test
    @DisplayName("Should get serial number details")
    void shouldGetSerialNumberDetails() {
        // Given
        SerialNumber serial = SerialNumber.create(testSku, testSerialNumber, testLocation,
            manufactureDate, warrantyExpiryDate);

        when(serialNumberRepository.findBySerialNumber(testSerialNumber))
            .thenReturn(Optional.of(serial));

        // When
        SerialNumber result = serialNumberService.getSerialNumber(testSerialNumber);

        // Then
        assertNotNull(result);
        assertEquals(testSerialNumber, result.getSerialNumber());
        assertEquals(testSku, result.getSku());
        assertEquals(manufactureDate, result.getManufactureDate());
        assertEquals(warrantyExpiryDate, result.getWarrantyExpiryDate());
    }

    @Test
    @DisplayName("Should throw exception when getting non-existent serial")
    void shouldThrowExceptionWhenGettingNonExistentSerial() {
        // Given
        when(serialNumberRepository.findBySerialNumber(testSerialNumber))
            .thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            serialNumberService.getSerialNumber(testSerialNumber)
        );
    }

    @Test
    @DisplayName("Should check warranty is valid")
    void shouldCheckWarrantyIsValid() {
        // Given: Warranty expires in the future
        LocalDate futureExpiry = LocalDate.now().plusYears(1);
        SerialNumber serial = SerialNumber.create(testSku, testSerialNumber, testLocation,
            manufactureDate, futureExpiry);

        when(serialNumberRepository.findBySerialNumber(testSerialNumber))
            .thenReturn(Optional.of(serial));

        // When
        boolean isValid = serialNumberService.isWarrantyValid(testSerialNumber);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should check warranty is expired")
    void shouldCheckWarrantyIsExpired() {
        // Given: Warranty expired in the past
        LocalDate pastExpiry = LocalDate.now().minusDays(1);
        SerialNumber serial = SerialNumber.create(testSku, testSerialNumber, testLocation,
            manufactureDate, pastExpiry);

        when(serialNumberRepository.findBySerialNumber(testSerialNumber))
            .thenReturn(Optional.of(serial));

        // When
        boolean isValid = serialNumberService.isWarrantyValid(testSerialNumber);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle complete serial number lifecycle")
    void shouldHandleCompleteSerialNumberLifecycle() {
        // Given
        ProductStock productStock = ProductStock.createWithSerialTracking(testSku);
        SerialNumber serial = SerialNumber.create(testSku, testSerialNumber, testLocation,
            manufactureDate, warrantyExpiryDate);

        when(serialNumberRepository.existsBySerialNumber(testSerialNumber))
            .thenReturn(false)  // For receive
            .thenReturn(true);  // For subsequent operations
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));
        when(serialNumberRepository.findBySerialNumber(testSerialNumber)).thenReturn(Optional.of(serial));

        // When: Full lifecycle
        // 1. Receive
        serialNumberService.receiveSerialNumber(
            testSku, testSerialNumber, testLocation, manufactureDate, warrantyExpiryDate
        );
        verify(serialNumberRepository, times(1)).save(any());

        // Update mock to return allocated serial
        SerialNumber allocatedSerial = serial.allocate();
        when(serialNumberRepository.findBySerialNumber(testSerialNumber))
            .thenReturn(Optional.of(allocatedSerial));

        // 2. Allocate to customer
        serialNumberService.allocateSerialNumber(testSku, testSerialNumber, "CUST-001");
        verify(serialNumberRepository, times(2)).save(any());

        // 3. Ship
        serialNumberService.shipSerialNumber(testSerialNumber);
        verify(serialNumberRepository, times(3)).save(any());

        // Then: All operations completed
        verify(productStockRepository, atLeast(2)).save(productStock);
    }

    @Test
    @DisplayName("Should handle multiple serial numbers for same SKU")
    void shouldHandleMultipleSerialsForSameSku() {
        // Given
        ProductStock productStock = ProductStock.createWithSerialTracking(testSku);

        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));
        when(serialNumberRepository.existsBySerialNumber(anyString())).thenReturn(false);

        // When: Receive multiple serials
        serialNumberService.receiveSerialNumber(testSku, "SN001", testLocation,
            manufactureDate, warrantyExpiryDate);
        serialNumberService.receiveSerialNumber(testSku, "SN002", testLocation,
            manufactureDate, warrantyExpiryDate);
        serialNumberService.receiveSerialNumber(testSku, "SN003", testLocation,
            manufactureDate, warrantyExpiryDate);

        // Then: All serials saved
        verify(serialNumberRepository, times(3)).save(any(SerialNumber.class));
        verify(productStockRepository, times(3)).save(productStock);
    }

    @Test
    @DisplayName("Should emit domain events on serial operations")
    void shouldEmitDomainEventsOnSerialOperations() {
        // Given
        ProductStock productStock = ProductStock.createWithSerialTracking(testSku);
        SerialNumber serial = SerialNumber.create(testSku, testSerialNumber, testLocation,
            manufactureDate, warrantyExpiryDate);

        when(serialNumberRepository.existsBySerialNumber(testSerialNumber)).thenReturn(false);
        when(productStockRepository.findBySku(testSku)).thenReturn(Optional.of(productStock));
        when(serialNumberRepository.findBySerialNumber(testSerialNumber)).thenReturn(Optional.of(serial));

        // When: Receive serial
        serialNumberService.receiveSerialNumber(
            testSku, testSerialNumber, testLocation, manufactureDate, warrantyExpiryDate
        );

        // Then: Events saved to outbox
        verify(outboxRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should track serial location history through moves")
    void shouldTrackSerialLocationHistoryThroughMoves() {
        // Given
        SerialNumber serial = SerialNumber.create(testSku, testSerialNumber, testLocation,
            manufactureDate, warrantyExpiryDate);

        Location location2 = Location.of("WH01", "B", "03", "02", "A", LocationType.GENERAL);
        Location location3 = Location.of("WH01", "C", "10", "05", "B", LocationType.GENERAL);

        when(serialNumberRepository.findBySerialNumber(testSerialNumber))
            .thenReturn(Optional.of(serial))
            .thenReturn(Optional.of(serial.moveTo(location2)))
            .thenReturn(Optional.of(serial.moveTo(location3)));

        // When: Move serial multiple times
        serialNumberService.moveSerialNumber(testSerialNumber, location2);
        serialNumberService.moveSerialNumber(testSerialNumber, location3);

        // Then: Location moves tracked
        verify(serialNumberRepository, times(2)).save(any(SerialNumber.class));
    }

    @Test
    @DisplayName("Should validate serial status transitions")
    void shouldValidateSerialStatusTransitions() {
        // Given: Available serial
        SerialNumber serial = SerialNumber.create(testSku, testSerialNumber, testLocation,
            manufactureDate, warrantyExpiryDate);

        when(serialNumberRepository.findBySerialNumber(testSerialNumber)).thenReturn(Optional.of(serial));

        // When: Change through different statuses
        serialNumberService.changeSerialStatus(testSerialNumber, SerialStatus.QUARANTINE);

        // Update mock for next status change
        SerialNumber quarantinedSerial = serial.changeStatus(SerialStatus.QUARANTINE);
        when(serialNumberRepository.findBySerialNumber(testSerialNumber))
            .thenReturn(Optional.of(quarantinedSerial));

        serialNumberService.changeSerialStatus(testSerialNumber, SerialStatus.IN_INVENTORY);

        // Then
        verify(serialNumberRepository, times(2)).save(any(SerialNumber.class));
    }
}
