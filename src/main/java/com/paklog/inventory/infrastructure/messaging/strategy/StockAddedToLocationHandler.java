package com.paklog.inventory.infrastructure.messaging.strategy;

import com.paklog.inventory.application.service.InventoryCommandService;
import com.paklog.inventory.domain.event.StockAddedToLocation;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class StockAddedToLocationHandler implements EventHandler {

    private static final String EVENT_TYPE = "paklog.inventory.stock-added-to-location.v1";

    private final InventoryCommandService inventoryCommandService;
    private final ObjectMapper objectMapper;

    public StockAddedToLocationHandler(InventoryCommandService inventoryCommandService, ObjectMapper objectMapper) {
        this.inventoryCommandService = inventoryCommandService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean canHandle(String eventType) {
        return EVENT_TYPE.equals(eventType);
    }

    @Override
    public void handle(CloudEvent event) throws IOException {
        StockAddedToLocation stockAddedToLocation = objectMapper.readValue(event.getData().toBytes(), StockAddedToLocation.class);
        inventoryCommandService.increaseQuantityOnHand(stockAddedToLocation.getSku(), stockAddedToLocation.getQuantity());
    }
}
