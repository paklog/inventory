package com.paklog.inventory.infrastructure.persistence.mongodb;

import com.paklog.inventory.domain.model.Location;
import com.paklog.inventory.domain.model.StockLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockLocationRepositoryImplTest {

    @Mock
    private StockLocationSpringRepository springRepository;

    @InjectMocks
    private StockLocationRepositoryImpl repository;

    private String sku;
    private Location location;
    private String id;
    private StockLocation stockLocation;
    private StockLocationDocument stockLocationDocument;

    @BeforeEach
    void setUp() {
        sku = "TEST-SKU-001";
        location = new Location("A1", "S1", "B1");
        id = sku + ":" + location.getAisle() + ":" + location.getShelf() + ":" + location.getBin();
        stockLocation = new StockLocation(sku, location, 100);
        stockLocationDocument = StockLocationDocument.fromDomain(stockLocation);
    }

    @Test
    void findBySkuAndLocation_found() {
        when(springRepository.findById(id)).thenReturn(Optional.of(stockLocationDocument));

        Optional<StockLocation> result = repository.findBySkuAndLocation(sku, location);

        assertTrue(result.isPresent());
        assertEquals(stockLocation, result.get());
        verify(springRepository, times(1)).findById(id);
    }

    @Test
    void findBySkuAndLocation_notFound() {
        when(springRepository.findById(id)).thenReturn(Optional.empty());

        Optional<StockLocation> result = repository.findBySkuAndLocation(sku, location);

        assertFalse(result.isPresent());
        verify(springRepository, times(1)).findById(id);
    }

    @Test
    void findBySku() {
        List<StockLocationDocument> documents = Arrays.asList(stockLocationDocument);
        when(springRepository.findBySku(sku)).thenReturn(documents);

        List<StockLocation> result = repository.findBySku(sku);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(stockLocation, result.get(0));
        verify(springRepository, times(1)).findBySku(sku);
    }

    @Test
    void save() {
        when(springRepository.save(any(StockLocationDocument.class))).thenReturn(stockLocationDocument);

        StockLocation result = repository.save(stockLocation);

        assertEquals(stockLocation, result);
        verify(springRepository, times(1)).save(any(StockLocationDocument.class));
    }
}
