package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.Location;
import com.paklog.inventory.domain.model.StockLocation;
import com.paklog.inventory.domain.repository.OutboxRepository;
import com.paklog.inventory.domain.repository.StockLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhysicalStockCommandServiceTest {

    @Mock
    private StockLocationRepository stockLocationRepository;
    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private PhysicalStockCommandService service;

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
    void addStock_newLocation() {
        when(stockLocationRepository.findBySkuAndLocation(sku, location)).thenReturn(Optional.empty());
        when(stockLocationRepository.save(any(StockLocation.class))).thenReturn(stockLocation);

        service.addStock(sku, location, 50);

        verify(stockLocationRepository, times(1)).findBySkuAndLocation(sku, location);
        verify(stockLocationRepository, times(1)).save(any(StockLocation.class));
        verify(outboxRepository, times(1)).saveAll(anyList());
    }

    @Test
    void addStock_existingLocation() {
        when(stockLocationRepository.findBySkuAndLocation(sku, location)).thenReturn(Optional.of(stockLocation));
        when(stockLocationRepository.save(any(StockLocation.class))).thenReturn(stockLocation);

        service.addStock(sku, location, 50);

        verify(stockLocationRepository, times(1)).findBySkuAndLocation(sku, location);
        verify(stockLocationRepository, times(1)).save(any(StockLocation.class));
        verify(outboxRepository, times(1)).saveAll(anyList());
        assertEquals(150, stockLocation.getQuantity());
    }

    @Test
    void moveStock_validMove() {
        Location fromLocation = new Location("A1", "S1", "B1");
        Location toLocation = new Location("A1", "S1", "B2");
        StockLocation fromStock = new StockLocation(sku, fromLocation, 100);
        StockLocation toStock = new StockLocation(sku, toLocation, 50);

        when(stockLocationRepository.findBySkuAndLocation(sku, fromLocation)).thenReturn(Optional.of(fromStock));
        when(stockLocationRepository.findBySkuAndLocation(sku, toLocation)).thenReturn(Optional.of(toStock));
        when(stockLocationRepository.save(any(StockLocation.class))).thenReturn(fromStock, toStock);

        service.moveStock(sku, fromLocation, toLocation, 30);

        verify(stockLocationRepository, times(1)).findBySkuAndLocation(sku, fromLocation);
        verify(stockLocationRepository, times(1)).findBySkuAndLocation(sku, toLocation);
        verify(stockLocationRepository, times(2)).save(any(StockLocation.class));
        verify(outboxRepository, times(2)).saveAll(anyList());
        assertEquals(70, fromStock.getQuantity());
        assertEquals(80, toStock.getQuantity());
    }

    @Test
    void moveStock_insufficientStock_throwsException() {
        Location fromLocation = new Location("A1", "S1", "B1");
        Location toLocation = new Location("A1", "S1", "B2");
        StockLocation fromStock = new StockLocation(sku, fromLocation, 10);
        StockLocation toStock = new StockLocation(sku, toLocation, 50);

        when(stockLocationRepository.findBySkuAndLocation(sku, fromLocation)).thenReturn(Optional.of(fromStock));
        when(stockLocationRepository.findBySkuAndLocation(sku, toLocation)).thenReturn(Optional.of(toStock));

        assertThrows(IllegalStateException.class, () -> service.moveStock(sku, fromLocation, toLocation, 30));

        verify(stockLocationRepository, times(1)).findBySkuAndLocation(sku, fromLocation);
        verify(stockLocationRepository, times(1)).findBySkuAndLocation(sku, toLocation);
        verify(stockLocationRepository, never()).save(any(StockLocation.class));
        verify(outboxRepository, never()).saveAll(anyList());
    }

    @Test
    void addPhysicalReservation_valid() {
        when(stockLocationRepository.findBySkuAndLocation(sku, location)).thenReturn(Optional.of(stockLocation));
        when(stockLocationRepository.save(any(StockLocation.class))).thenReturn(stockLocation);

        service.addPhysicalReservation(sku, location, 10, "res1");

        verify(stockLocationRepository, times(1)).findBySkuAndLocation(sku, location);
        verify(stockLocationRepository, times(1)).save(any(StockLocation.class));
        verify(outboxRepository, times(1)).saveAll(anyList());
        assertEquals(1, stockLocation.getPhysicalReservations().size());
        assertEquals(90, stockLocation.getAvailableToPick());
    }

    @Test
    void addPhysicalReservation_insufficientStock_throwsException() {
        when(stockLocationRepository.findBySkuAndLocation(sku, location)).thenReturn(Optional.of(stockLocation));

        assertThrows(IllegalStateException.class, () -> service.addPhysicalReservation(sku, location, 110, "res1"));

        verify(stockLocationRepository, times(1)).findBySkuAndLocation(sku, location);
        verify(stockLocationRepository, never()).save(any(StockLocation.class));
        verify(outboxRepository, never()).saveAll(anyList());
    }

    @Test
    void removePhysicalReservation_valid() {
        stockLocation.addPhysicalReservation("res1", 10);
        stockLocation.markEventsAsCommitted(); // Clear events from addPhysicalReservation

        when(stockLocationRepository.findBySkuAndLocation(sku, location)).thenReturn(Optional.of(stockLocation));
        when(stockLocationRepository.save(any(StockLocation.class))).thenReturn(stockLocation);

        service.removePhysicalReservation(sku, location, "res1");

        verify(stockLocationRepository, times(1)).findBySkuAndLocation(sku, location);
        verify(stockLocationRepository, times(1)).save(any(StockLocation.class));
        verify(outboxRepository, times(1)).saveAll(anyList());
        assertTrue(stockLocation.getPhysicalReservations().isEmpty());
        assertEquals(100, stockLocation.getAvailableToPick());
    }

    @Test
    void pickStock_valid() {
        stockLocation.addPhysicalReservation("res1", 10);
        stockLocation.markEventsAsCommitted();

        when(stockLocationRepository.findBySkuAndLocation(sku, location)).thenReturn(Optional.of(stockLocation));
        when(stockLocationRepository.save(any(StockLocation.class))).thenReturn(stockLocation);

        service.pickStock(sku, location, 10, "res1");

        verify(stockLocationRepository, times(1)).findBySkuAndLocation(sku, location);
        verify(stockLocationRepository, times(1)).save(any(StockLocation.class));
        verify(outboxRepository, times(1)).saveAll(anyList());
        assertTrue(stockLocation.getPhysicalReservations().isEmpty());
        assertEquals(90, stockLocation.getQuantity());
        assertEquals(90, stockLocation.getAvailableToPick());
    }

    @Test
    void pickStock_insufficientStock_throwsException() {
        stockLocation.addPhysicalReservation("res1", 10);
        stockLocation.markEventsAsCommitted();

        when(stockLocationRepository.findBySkuAndLocation(sku, location)).thenReturn(Optional.of(stockLocation));

        assertThrows(IllegalStateException.class, () -> service.pickStock(sku, location, 110, "res1"));

        verify(stockLocationRepository, times(1)).findBySkuAndLocation(sku, location);
        verify(stockLocationRepository, never()).save(any(StockLocation.class));
        verify(outboxRepository, never()).saveAll(anyList());
    }
}
