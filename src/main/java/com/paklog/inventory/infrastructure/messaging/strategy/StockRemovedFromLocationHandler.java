package com.paklog.inventory.infrastructure.messaging.strategy;

import com.paklog.inventory.application.service.InventoryCommandService;
import com.paklog.inventory.domain.event.StockRemovedFromLocation;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class StockRemovedFromLocationHandler implements EventHandler {

    private static final String EVENT_TYPE = "paklog.inventory.stock-removed-from-location.v1";

    private final InventoryCommandService inventoryCommandService;
    private final ObjectMapper objectMapper;

    public StockRemovedFromLocationHandler(InventoryCommandService inventoryCommandService, ObjectMapper objectMapper) {
        this.inventoryCommandService = inventoryCommandService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean canHandle(String eventType) {
        return EVENT_TYPE.equals(eventType);
    }

    @Override
    public void handle(CloudEvent event) throws IOException {
        StockRemovedFromLocation stockRemovedFromLocation = objectMapper.readValue(event.getData().toBytes(), StockRemovedFromLocation.class);
        inventoryCommandService.decreaseQuantityOnHand(stockRemovedFromLocation.getSku(), stockRemovedFromLocation.getQuantity());
    }
}
