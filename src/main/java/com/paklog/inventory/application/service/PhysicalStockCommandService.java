package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.Location;
import com.paklog.inventory.domain.model.OutboxEvent;
import com.paklog.inventory.domain.model.StockLocation;
import com.paklog.inventory.domain.repository.OutboxRepository;
import com.paklog.inventory.domain.repository.StockLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PhysicalStockCommandService {

    private final StockLocationRepository stockLocationRepository;
    private final OutboxRepository outboxRepository;

    public PhysicalStockCommandService(StockLocationRepository stockLocationRepository, OutboxRepository outboxRepository) {
        this.stockLocationRepository = stockLocationRepository;
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public void addStock(String sku, Location location, int quantity) {
        StockLocation stockLocation = stockLocationRepository.findBySkuAndLocation(sku, location)
                .orElse(new StockLocation(sku, location, 0));

        stockLocation.addStock(quantity);

        stockLocationRepository.save(stockLocation);
        publishDomainEvents(stockLocation);
    }

    @Transactional
    public void moveStock(String sku, Location fromLocation, Location toLocation, int quantity) {
        StockLocation fromStockLocation = stockLocationRepository.findBySkuAndLocation(sku, fromLocation)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found at from location"));

        StockLocation toStockLocation = stockLocationRepository.findBySkuAndLocation(sku, toLocation)
                .orElse(new StockLocation(sku, toLocation, 0));

        fromStockLocation.removeStock(quantity);
        toStockLocation.addStock(quantity);

        stockLocationRepository.save(fromStockLocation);
        stockLocationRepository.save(toStockLocation);

        publishDomainEvents(fromStockLocation);
        publishDomainEvents(toStockLocation);
    }

    @Transactional
    public void addPhysicalReservation(String sku, Location location, int quantity, String reservationId) {
        StockLocation stockLocation = stockLocationRepository.findBySkuAndLocation(sku, location)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found at location"));

        stockLocation.addPhysicalReservation(reservationId, quantity);

        stockLocationRepository.save(stockLocation);
        publishDomainEvents(stockLocation);
    }

    @Transactional
    public void removePhysicalReservation(String sku, Location location, String reservationId) {
        StockLocation stockLocation = stockLocationRepository.findBySkuAndLocation(sku, location)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found at location"));

        stockLocation.removePhysicalReservation(reservationId);

        stockLocationRepository.save(stockLocation);
        publishDomainEvents(stockLocation);
    }

    @Transactional
    public void pickStock(String sku, Location location, int quantity, String reservationId) {
        StockLocation stockLocation = stockLocationRepository.findBySkuAndLocation(sku, location)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found at location"));

        stockLocation.removePhysicalReservation(reservationId);
        stockLocation.removeStock(quantity);

        stockLocationRepository.save(stockLocation);
        publishDomainEvents(stockLocation);
    }

    private void publishDomainEvents(StockLocation stockLocation) {
        List<OutboxEvent> outboxEvents = stockLocation.getUncommittedEvents().stream()
                .map(OutboxEvent::from)
                .collect(Collectors.toList());
        if (!outboxEvents.isEmpty()) {
            outboxRepository.saveAll(outboxEvents);
            stockLocation.markEventsAsCommitted();
        

}
}
}
