package com.paklog.inventory.domain.model;

import com.paklog.inventory.domain.event.DomainEvent;
import com.paklog.inventory.domain.event.StockAddedToLocation;
import com.paklog.inventory.domain.event.StockRemovedFromLocation;
import com.paklog.inventory.domain.event.PhysicalStockReserved;
import com.paklog.inventory.domain.event.PhysicalStockReservationReleased;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StockLocationTest {

    private String sku;
    private Location location;
    private StockLocation stockLocation;

    @BeforeEach
    void setUp() {
        sku = "TEST-SKU-001";
        location = new Location("A1", "S1", "B1");
        stockLocation = new StockLocation(sku, location, 100);
    }

    @Test
    void constructorAndGetters() {
        assertEquals(sku, stockLocation.getSku());
        assertEquals(location, stockLocation.getLocation());
        assertEquals(100, stockLocation.getQuantity());
        assertTrue(stockLocation.getPhysicalReservations().isEmpty());
        assertTrue(stockLocation.getUncommittedEvents().isEmpty());
    }

    @Test
    void addStock_positiveQuantity_increasesQuantityAndAddsEvent() {
        stockLocation.addStock(50);
        assertEquals(150, stockLocation.getQuantity());
        assertEquals(1, stockLocation.getUncommittedEvents().size());
        assertTrue(stockLocation.getUncommittedEvents().get(0) instanceof StockAddedToLocation);
        StockAddedToLocation event = (StockAddedToLocation) stockLocation.getUncommittedEvents().get(0);
        assertEquals(sku, event.getSku());
        assertEquals(location, event.getLocation());
        assertEquals(50, event.getQuantity());
    }

    @Test
    void addStock_zeroQuantity_throwsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> stockLocation.addStock(0));
        assertEquals("Quantity to add must be positive.", exception.getMessage());
        assertEquals(100, stockLocation.getQuantity()); // Quantity should not change
        assertTrue(stockLocation.getUncommittedEvents().isEmpty());
    }

    @Test
    void addStock_negativeQuantity_throwsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> stockLocation.addStock(-10));
        assertEquals("Quantity to add must be positive.", exception.getMessage());
        assertEquals(100, stockLocation.getQuantity()); // Quantity should not change
        assertTrue(stockLocation.getUncommittedEvents().isEmpty());
    }

    @Test
    void removeStock_positiveQuantity_decreasesQuantityAndAddsEvent() {
        stockLocation.removeStock(30);
        assertEquals(70, stockLocation.getQuantity());
        assertEquals(1, stockLocation.getUncommittedEvents().size());
        assertTrue(stockLocation.getUncommittedEvents().get(0) instanceof StockRemovedFromLocation);
        StockRemovedFromLocation event = (StockRemovedFromLocation) stockLocation.getUncommittedEvents().get(0);
        assertEquals(sku, event.getSku());
        assertEquals(location, event.getLocation());
        assertEquals(30, event.getQuantity());
    }

    @Test
    void removeStock_zeroQuantity_throwsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> stockLocation.removeStock(0));
        assertEquals("Quantity to remove must be positive.", exception.getMessage());
        assertEquals(100, stockLocation.getQuantity());
        assertTrue(stockLocation.getUncommittedEvents().isEmpty());
    }

    @Test
    void removeStock_negativeQuantity_throwsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> stockLocation.removeStock(-10));
        assertEquals("Quantity to remove must be positive.", exception.getMessage());
        assertEquals(100, stockLocation.getQuantity());
        assertTrue(stockLocation.getUncommittedEvents().isEmpty());
    }

    @Test
    void removeStock_insufficientStock_throwsException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> stockLocation.removeStock(150));
        assertEquals("Not enough stock to remove.", exception.getMessage());
        assertEquals(100, stockLocation.getQuantity());
        assertTrue(stockLocation.getUncommittedEvents().isEmpty());
    }

    @Test
    void addPhysicalReservation_validQuantity_addsReservationAndEvent() {
        stockLocation.addPhysicalReservation("order123", 20);
        assertEquals(1, stockLocation.getPhysicalReservations().size());
        assertEquals(20, stockLocation.getPhysicalReservations().get(0).getQuantity());
        assertEquals(80, stockLocation.getAvailableToPick());
        assertEquals(1, stockLocation.getUncommittedEvents().size());
        assertTrue(stockLocation.getUncommittedEvents().get(0) instanceof PhysicalStockReserved);
        PhysicalStockReserved event = (PhysicalStockReserved) stockLocation.getUncommittedEvents().get(0);
        assertEquals(sku, event.getSku());
        assertEquals(location, event.getLocation());
        assertEquals(20, event.getQuantity());
        assertEquals("order123", event.getReservationId());
    }

    @Test
    void addPhysicalReservation_zeroQuantity_throwsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> stockLocation.addPhysicalReservation("order123", 0));
        assertEquals("Quantity to reserve must be positive.", exception.getMessage());
        assertTrue(stockLocation.getPhysicalReservations().isEmpty());
        assertTrue(stockLocation.getUncommittedEvents().isEmpty());
    }

    @Test
    void addPhysicalReservation_negativeQuantity_throwsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> stockLocation.addPhysicalReservation("order123", -10));
        assertEquals("Quantity to reserve must be positive.", exception.getMessage());
        assertTrue(stockLocation.getPhysicalReservations().isEmpty());
        assertTrue(stockLocation.getUncommittedEvents().isEmpty());
    }

    @Test
    void addPhysicalReservation_insufficientAvailableToPick_throwsException() {
        stockLocation.addPhysicalReservation("order123", 80);
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> stockLocation.addPhysicalReservation("order456", 30));
        assertEquals("Not enough stock to reserve.", exception.getMessage());
        assertEquals(1, stockLocation.getPhysicalReservations().size()); // Only the first reservation should be added
        assertEquals(20, stockLocation.getAvailableToPick());
        assertEquals(1, stockLocation.getUncommittedEvents().size()); // Only the first event should be added
    }

    @Test
    void removePhysicalReservation_existingReservation_removesReservationAndAddsEvent() {
        stockLocation.addPhysicalReservation("order123", 20);
        stockLocation.markEventsAsCommitted(); // Clear events from addPhysicalReservation

        stockLocation.removePhysicalReservation("order123");
        assertTrue(stockLocation.getPhysicalReservations().isEmpty());
        assertEquals(100, stockLocation.getAvailableToPick());
        assertEquals(1, stockLocation.getUncommittedEvents().size());
        assertTrue(stockLocation.getUncommittedEvents().get(0) instanceof PhysicalStockReservationReleased);
        PhysicalStockReservationReleased event = (PhysicalStockReservationReleased) stockLocation.getUncommittedEvents().get(0);
        assertEquals(sku, event.getSku());
        assertEquals(location, event.getLocation());
        assertEquals("order123", event.getReservationId());
    }

    @Test
    void removePhysicalReservation_nonExistingReservation_doesNothing() {
        stockLocation.addPhysicalReservation("order123", 20);
        stockLocation.markEventsAsCommitted();

        stockLocation.removePhysicalReservation("nonExistentOrder");
        assertEquals(1, stockLocation.getPhysicalReservations().size());
        assertEquals(80, stockLocation.getAvailableToPick());
        assertTrue(stockLocation.getUncommittedEvents().isEmpty());
    }

    @Test
    void getAvailableToPick_noReservations() {
        assertEquals(100, stockLocation.getAvailableToPick());
    }

    @Test
    void getAvailableToPick_withReservations() {
        stockLocation.addPhysicalReservation("order1", 20);
        stockLocation.addPhysicalReservation("order2", 30);
        assertEquals(50, stockLocation.getAvailableToPick());
    }

    @Test
    void markEventsAsCommitted_clearsEvents() {
        stockLocation.addStock(10);
        assertFalse(stockLocation.getUncommittedEvents().isEmpty());
        stockLocation.markEventsAsCommitted();
        assertTrue(stockLocation.getUncommittedEvents().isEmpty());
    }

    @Test
    void equalsAndHashCode() {
        Location location2 = new Location("A1", "S1", "B1");
        Location location3 = new Location("A1", "S1", "B2");

        StockLocation stockLocation1 = new StockLocation("SKU001", location, 10);
        StockLocation stockLocation2 = new StockLocation("SKU001", location2, 20); // Same SKU and location, different quantity
        StockLocation stockLocation3 = new StockLocation("SKU002", location, 10);
        StockLocation stockLocation4 = new StockLocation("SKU001", location3, 10);

        assertEquals(stockLocation1, stockLocation2);
        assertNotEquals(stockLocation1, stockLocation3);
        assertNotEquals(stockLocation1, stockLocation4);

        assertEquals(stockLocation1.hashCode(), stockLocation2.hashCode());
        assertNotEquals(stockLocation1.hashCode(), stockLocation3.hashCode());
        assertNotEquals(stockLocation1.hashCode(), stockLocation4.hashCode());
    }
}
