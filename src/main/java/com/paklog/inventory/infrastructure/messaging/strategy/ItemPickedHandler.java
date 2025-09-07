package com.paklog.inventory.infrastructure.messaging.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.inventory.application.dto.ItemPickedData;
import com.paklog.inventory.application.service.InventoryCommandService;
import com.paklog.inventory.infrastructure.metrics.InventoryMetricsService;
import io.cloudevents.CloudEvent;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ItemPickedHandler implements EventHandler {

    private static final Logger log = LoggerFactory.getLogger(ItemPickedHandler.class);
    private static final String EVENT_TYPE = "com.example.fulfillment.warehouse.item.picked";
    
    private final InventoryCommandService commandService;
    private final ObjectMapper objectMapper;
    private final InventoryMetricsService metricsService;

    public ItemPickedHandler(InventoryCommandService commandService, 
                           ObjectMapper objectMapper,
                           InventoryMetricsService metricsService) {
        this.commandService = commandService;
        this.objectMapper = objectMapper;
        this.metricsService = metricsService;
    }

    @Override
    public boolean canHandle(String eventType) {
        return EVENT_TYPE.equals(eventType);
    }

    @Override
    public void handle(CloudEvent event) throws Exception {
        Timer.Sample sample = metricsService.startEventProcessing();
        
        try {
            if (event.getData() == null) {
                log.warn("Received ItemPicked event with no data. Event ID: {}", event.getId());
                metricsService.incrementEventError(EVENT_TYPE, "no_data");
                return;
            }
            
            ItemPickedData data = objectMapper.readValue(
                event.getData().toBytes(), 
                ItemPickedData.class
            );
            
            commandService.processItemPicked(data.getSku(), data.getQuantityPicked(), data.getOrderId());
            
            metricsService.incrementEventProcessed(EVENT_TYPE);
            metricsService.stopEventProcessing(sample, EVENT_TYPE);
            
            log.info("Processed ItemPicked for SKU: {}, Quantity Picked: {}, Order ID: {}",
                    data.getSku(), data.getQuantityPicked(), data.getOrderId());
        } catch (Exception e) {
            metricsService.incrementEventError(EVENT_TYPE, e.getClass().getSimpleName());
            metricsService.stopEventProcessing(sample, EVENT_TYPE + "_error");
            throw e;
        }
    }
}