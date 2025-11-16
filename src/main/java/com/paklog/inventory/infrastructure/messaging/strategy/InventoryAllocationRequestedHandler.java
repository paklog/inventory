package com.paklog.inventory.infrastructure.messaging.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.inventory.application.dto.InventoryAllocationRequestedData;
import com.paklog.inventory.application.service.InventoryCommandService;
import com.paklog.inventory.infrastructure.metrics.InventoryMetricsService;
import io.cloudevents.CloudEvent;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InventoryAllocationRequestedHandler implements EventHandler {

    private static final Logger log = LoggerFactory.getLogger(InventoryAllocationRequestedHandler.class);
    private static final String EVENT_TYPE = "com.example.fulfillment.warehouse.inventory.allocation.requested";
    
    private final InventoryCommandService commandService;
    private final ObjectMapper objectMapper;
    private final InventoryMetricsService metricsService;

    public InventoryAllocationRequestedHandler(InventoryCommandService commandService, 
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
                log.warn("Received InventoryAllocationRequested event with no data. Event ID: {}", event.getId());
                metricsService.incrementEventError(EVENT_TYPE, "no_data");
                return;
            }
            
            InventoryAllocationRequestedData data = objectMapper.readValue(
                event.getData().toBytes(), 
                InventoryAllocationRequestedData.class
            );
            
            commandService.allocateStock(data.getSku(), data.getQuantity(), data.getOrderId());
            
            metricsService.incrementEventProcessed(EVENT_TYPE);
            metricsService.stopEventProcessing(sample, EVENT_TYPE);
            
            log.info("Processed InventoryAllocationRequested for SKU: {}, Quantity: {}, Order ID: {}",
                    data.getSku(), data.getQuantity(), data.getOrderId());
        } catch (Exception e) {
            metricsService.incrementEventError(EVENT_TYPE, e.getClass().getSimpleName());
            metricsService.stopEventProcessing(sample, EVENT_TYPE + "_error");
            throw e;
        

}
}
}
