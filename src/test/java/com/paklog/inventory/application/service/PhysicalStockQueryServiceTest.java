package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.Location;
import com.paklog.inventory.domain.model.StockLocation;
import com.paklog.inventory.domain.repository.StockLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhysicalStockQueryServiceTest {

    @Mock
    private StockLocationRepository stockLocationRepository;

    @InjectMocks
    private PhysicalStockQueryService service;

    private String sku;
    private Location location1;
    private Location location2;
    private StockLocation stockLocation1;
    private StockLocation stockLocation2;

    @BeforeEach
    void setUp() {
        sku = "TEST-SKU-001";
        location1 = new Location("A1", "S1", "B1");
        location2 = new Location("A1", "S1", "B2");
        stockLocation1 = new StockLocation(sku, location1, 100);
        stockLocation2 = new StockLocation(sku, location2, 50);
    }

    @Test
    void findBySku_returnsListOfStockLocations() {
        when(stockLocationRepository.findBySku(sku)).thenReturn(Arrays.asList(stockLocation1, stockLocation2));

        List<StockLocation> result = service.findBySku(sku);

        assertEquals(2, result.size());
        assertEquals(stockLocation1, result.get(0));
        assertEquals(stockLocation2, result.get(1));
    }
}
