package com.paklog.inventory.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.inventory.application.dto.AddStockRequest;
import com.paklog.inventory.application.dto.MoveStockRequest;
import com.paklog.inventory.application.dto.PhysicalReservationRequest;
import com.paklog.inventory.application.dto.PickStockRequest;
import com.paklog.inventory.application.service.PhysicalStockCommandService;
import com.paklog.inventory.application.service.PhysicalStockQueryService;
import com.paklog.inventory.domain.model.Location;
import com.paklog.inventory.domain.model.StockLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PhysicalStockController.class)
class PhysicalStockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PhysicalStockCommandService commandService;

    @MockBean
    private PhysicalStockQueryService queryService;

    private String sku;
    private Location location;

    @BeforeEach
    void setUp() {
        sku = "TEST-SKU-001";
        location = new Location("A1", "S1", "B1");
    }

    @Test
    void addStock_success() throws Exception {
        AddStockRequest request = new AddStockRequest();
        request.setSku(sku);
        request.setLocation(location);
        request.setQuantity(100);

        mockMvc.perform(post("/inventory/physical-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(commandService, times(1)).addStock(sku, location, 100);
    }

    @Test
    void moveStock_success() throws Exception {
        Location fromLocation = new Location("A1", "S1", "B1");
        Location toLocation = new Location("A1", "S1", "B2");

        MoveStockRequest request = new MoveStockRequest();
        request.setSku(sku);
        request.setFromLocation(fromLocation);
        request.setToLocation(toLocation);
        request.setQuantity(10);

        mockMvc.perform(post("/inventory/physical-stock/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(commandService, times(1)).moveStock(sku, fromLocation, toLocation, 10);
    }

    @Test
    void addPhysicalReservation_success() throws Exception {
        PhysicalReservationRequest request = new PhysicalReservationRequest();
        request.setSku(sku);
        request.setLocation(location);
        request.setQuantity(5);
        request.setReservationId("res123");

        mockMvc.perform(post("/inventory/physical-stock/physical-reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(commandService, times(1)).addPhysicalReservation(sku, location, 5, "res123");
    }

    @Test
    void removePhysicalReservation_success() throws Exception {
        String reservationId = "res123";
        mockMvc.perform(delete("/inventory/physical-stock/physical-reservations/{sku}/{aisle}/{shelf}/{bin}/{reservationId}",
                        sku, location.getAisle(), location.getShelf(), location.getBin(), reservationId))
                .andExpect(status().isAccepted());

        verify(commandService, times(1)).removePhysicalReservation(eq(sku), any(Location.class), eq(reservationId));
    }

    @Test
    void pickStock_success() throws Exception {
        PickStockRequest request = new PickStockRequest();
        request.setSku(sku);
        request.setLocation(location);
        request.setQuantity(5);
        request.setReservationId("pick123");

        mockMvc.perform(post("/inventory/physical-stock/picks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(commandService, times(1)).pickStock(sku, location, 5, "pick123");
    }

    @Test
    void getPhysicalStock_success() throws Exception {
        StockLocation stockLocation = new StockLocation(sku, location, 100);
        when(queryService.findBySku(sku)).thenReturn(Arrays.asList(stockLocation));

        mockMvc.perform(get("/inventory/physical-stock/{sku}", sku))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value(sku))
                .andExpect(jsonPath("$[0].location.aisle").value(location.getAisle()))
                .andExpect(jsonPath("$[0].quantity").value(100));

        verify(queryService, times(1)).findBySku(sku);
    }
}
