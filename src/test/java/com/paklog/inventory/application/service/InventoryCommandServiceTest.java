package com.paklog.inventory.application.service;

import com.paklog.inventory.application.port.EventPublisherPort;
import com.paklog.inventory.application.validator.ProductExistenceValidator;
import com.paklog.inventory.domain.event.StockLevelChangedEvent;
import com.paklog.inventory.domain.exception.ProductStockNotFoundException;
import com.paklog.inventory.domain.model.InventoryLedgerEntry;
import com.paklog.inventory.domain.model.ProductStock;
import com.paklog.inventory.domain.model.StockLevel;
import com.paklog.inventory.domain.repository.InventoryLedgerRepository;
import com.paklog.inventory.domain.repository.ProductStockRepository;
import com.paklog.inventory.infrastructure.metrics.InventoryMetricsService;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryCommandServiceTest {

    @Mock
    private ProductStockRepository productStockRepository;
    @Mock
    private InventoryLedgerRepository inventoryLedgerRepository;
    @Mock
    private InventoryMetricsService metricsService;
    @Mock
    private EventPublisherPort eventPublisherPort; // Although not directly injected, ProductStock uses it
    @Mock
    private com.paklog.inventory.domain.repository.OutboxRepository outboxRepository;
    @Mock
    private ProductExistenceValidator productExistenceValidator;

    @InjectMocks
    private InventoryCommandService inventoryCommandService;

    private String sku;
    private ProductStock existingProductStock;

    @BeforeEach
    void setUp() {
        sku = "PROD001";
        existingProductStock = ProductStock.load(sku, 100, 20, LocalDateTime.now());
        // Clear uncommitted events from the loaded product stock for clean test state
        existingProductStock.markEventsAsCommitted();
        
        // Mock metrics service behavior
        Timer.Sample mockSample = mock(Timer.Sample.class);
        lenient().when(metricsService.startStockOperation()).thenReturn(mockSample);
        lenient().when(metricsService.startQueryOperation()).thenReturn(mockSample);
        lenient().when(metricsService.startEventProcessing()).thenReturn(mockSample);
    }

    @Test
    @DisplayName("Should adjust stock positively and save ledger entry and product stock")
    void adjustStock_PositiveChange_SavesEntitiesAndReturnsUpdatedProductStock() {
        // Arrange
        int quantityChange = 30;
        String reasonCode = "ADJUSTMENT_IN";
        String comment = "Found extra stock";
        String operatorId = "admin";

        when(productStockRepository.findBySku(sku)).thenReturn(Optional.of(existingProductStock));
        when(productStockRepository.save(any(ProductStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryLedgerRepository.save(any(InventoryLedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductStock updatedProductStock = inventoryCommandService.adjustStock(sku, quantityChange, reasonCode, comment, operatorId);

        // Assert
        assertNotNull(updatedProductStock);
        assertEquals(sku, updatedProductStock.getSku());
        assertEquals(130, updatedProductStock.getQuantityOnHand()); // 100 + 30
        assertEquals(20, updatedProductStock.getQuantityAllocated());
        assertEquals(110, updatedProductStock.getAvailableToPromise());

        verify(productStockRepository, times(1)).findBySku(sku);
        verify(productStockRepository, times(1)).save(any(ProductStock.class));
        verify(inventoryLedgerRepository, times(1)).save(any(InventoryLedgerEntry.class));

        ArgumentCaptor<InventoryLedgerEntry> ledgerEntryCaptor = ArgumentCaptor.forClass(InventoryLedgerEntry.class);
        verify(inventoryLedgerRepository).save(ledgerEntryCaptor.capture());
        InventoryLedgerEntry capturedLedgerEntry = ledgerEntryCaptor.getValue();
        assertEquals(sku, capturedLedgerEntry.getSku());
        assertEquals(quantityChange, capturedLedgerEntry.getQuantityChange());
        assertEquals(reasonCode + " - " + comment, capturedLedgerEntry.getReason());
        assertEquals(operatorId, capturedLedgerEntry.getOperatorId());
        assertEquals(com.paklog.inventory.domain.model.ChangeType.ADJUSTMENT_POSITIVE, capturedLedgerEntry.getChangeType());
    }

    @Test
    @DisplayName("Should adjust stock negatively and save ledger entry and product stock")
    void adjustStock_NegativeChange_SavesEntitiesAndReturnsUpdatedProductStock() {
        // Arrange
        int quantityChange = -10;
        String reasonCode = "ADJUSTMENT_OUT";
        String comment = "Damaged stock";
        String operatorId = "admin";

        when(productStockRepository.findBySku(sku)).thenReturn(Optional.of(existingProductStock));
        when(productStockRepository.save(any(ProductStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryLedgerRepository.save(any(InventoryLedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductStock updatedProductStock = inventoryCommandService.adjustStock(sku, quantityChange, reasonCode, comment, operatorId);

        // Assert
        assertNotNull(updatedProductStock);
        assertEquals(sku, updatedProductStock.getSku());
        assertEquals(90, updatedProductStock.getQuantityOnHand()); // 100 - 10
        assertEquals(20, updatedProductStock.getQuantityAllocated());
        assertEquals(70, updatedProductStock.getAvailableToPromise());

        verify(productStockRepository, times(1)).findBySku(sku);
        verify(productStockRepository, times(1)).save(any(ProductStock.class));
        verify(inventoryLedgerRepository, times(1)).save(any(InventoryLedgerEntry.class));

        ArgumentCaptor<InventoryLedgerEntry> ledgerEntryCaptor = ArgumentCaptor.forClass(InventoryLedgerEntry.class);
        verify(inventoryLedgerRepository).save(ledgerEntryCaptor.capture());
        InventoryLedgerEntry capturedLedgerEntry = ledgerEntryCaptor.getValue();
        assertEquals(sku, capturedLedgerEntry.getSku());
        assertEquals(quantityChange, capturedLedgerEntry.getQuantityChange());
        assertEquals(reasonCode + " - " + comment, capturedLedgerEntry.getReason());
        assertEquals(operatorId, capturedLedgerEntry.getOperatorId());
        assertEquals(com.paklog.inventory.domain.model.ChangeType.ADJUSTMENT_NEGATIVE, capturedLedgerEntry.getChangeType());
    }

    @Test
    @DisplayName("Should throw ProductStockNotFoundException when adjusting stock for non-existent product")
    void adjustStock_ProductNotFound_ThrowsException() {
        // Arrange
        int quantityChange = 10;
        String reasonCode = "ADJUSTMENT_IN";
        String comment = null;
        String operatorId = "admin";

        when(productStockRepository.findBySku(sku)).thenReturn(Optional.empty());

        // Act & Assert
        ProductStockNotFoundException exception = assertThrows(ProductStockNotFoundException.class, () ->
                inventoryCommandService.adjustStock(sku, quantityChange, reasonCode, comment, operatorId));
        assertEquals("ProductStock not found for SKU: " + sku, exception.getMessage());
        assertEquals(sku, exception.getSku());

        verify(productStockRepository, times(1)).findBySku(sku);
        verify(productStockRepository, never()).save(any(ProductStock.class));
        verify(inventoryLedgerRepository, never()).save(any(InventoryLedgerEntry.class));
    }

    @Test
    @DisplayName("Should allocate stock and save ledger entry and product stock")
    void allocateStock_ValidQuantity_SavesEntitiesAndReturnsUpdatedProductStock() {
        // Arrange
        int quantityToAllocate = 30;
        String orderId = "ORDER001";

        when(productStockRepository.findBySku(sku)).thenReturn(Optional.of(existingProductStock));
        when(productStockRepository.save(any(ProductStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryLedgerRepository.save(any(InventoryLedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductStock updatedProductStock = inventoryCommandService.allocateStock(sku, quantityToAllocate, orderId);

        // Assert
        assertNotNull(updatedProductStock);
        assertEquals(sku, updatedProductStock.getSku());
        assertEquals(100, updatedProductStock.getQuantityOnHand());
        assertEquals(50, updatedProductStock.getQuantityAllocated()); // 20 + 30
        assertEquals(50, updatedProductStock.getAvailableToPromise());

        verify(productStockRepository, times(1)).findBySku(sku);
        verify(productStockRepository, times(1)).save(any(ProductStock.class));
        verify(inventoryLedgerRepository, times(1)).save(any(InventoryLedgerEntry.class));

        ArgumentCaptor<InventoryLedgerEntry> ledgerEntryCaptor = ArgumentCaptor.forClass(InventoryLedgerEntry.class);
        verify(inventoryLedgerRepository).save(ledgerEntryCaptor.capture());
        InventoryLedgerEntry capturedLedgerEntry = ledgerEntryCaptor.getValue();
        assertEquals(sku, capturedLedgerEntry.getSku());
        assertEquals(quantityToAllocate, capturedLedgerEntry.getQuantityChange());
        assertEquals(orderId, capturedLedgerEntry.getSourceReference());
        assertEquals(com.paklog.inventory.domain.model.ChangeType.ALLOCATION, capturedLedgerEntry.getChangeType());
    }

    @Test
    @DisplayName("Should throw ProductStockNotFoundException when allocating stock for non-existent product")
    void allocateStock_ProductNotFound_ThrowsException() {
        // Arrange
        int quantityToAllocate = 10;
        String orderId = "ORDER001";

        when(productStockRepository.findBySku(sku)).thenReturn(Optional.empty());

        // Act & Assert
        ProductStockNotFoundException exception = assertThrows(ProductStockNotFoundException.class, () ->
                inventoryCommandService.allocateStock(sku, quantityToAllocate, orderId));
        assertEquals("ProductStock not found for SKU: " + sku, exception.getMessage());

        verify(productStockRepository, times(1)).findBySku(sku);
        verify(productStockRepository, never()).save(any(ProductStock.class));
        verify(inventoryLedgerRepository, never()).save(any(InventoryLedgerEntry.class));
    }

    @Test
    @DisplayName("Should process item picked, deallocate, adjust quantity on hand, and save ledger entry and product stock")
    void processItemPicked_ValidQuantity_SavesEntitiesAndReturnsUpdatedProductStock() {
        // Arrange
        int quantityPicked = 10;
        String orderId = "ORDER001";
        existingProductStock.allocate(quantityPicked); // Allocate some stock first for deallocation

        when(productStockRepository.findBySku(sku)).thenReturn(Optional.of(existingProductStock));
        when(productStockRepository.save(any(ProductStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryLedgerRepository.save(any(InventoryLedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductStock updatedProductStock = inventoryCommandService.processItemPicked(sku, quantityPicked, orderId);

        // Assert
        assertNotNull(updatedProductStock);
        assertEquals(sku, updatedProductStock.getSku());
        assertEquals(90, updatedProductStock.getQuantityOnHand()); // 100 - 10
        assertEquals(20, updatedProductStock.getQuantityAllocated()); // 20 (initial) + 10 (allocated) - 10 (deallocated)
        assertEquals(70, updatedProductStock.getAvailableToPromise()); // 90 - 20

        verify(productStockRepository, times(1)).findBySku(sku);
        verify(productStockRepository, times(1)).save(any(ProductStock.class));
        verify(inventoryLedgerRepository, times(1)).save(any(InventoryLedgerEntry.class));

        ArgumentCaptor<InventoryLedgerEntry> ledgerEntryCaptor = ArgumentCaptor.forClass(InventoryLedgerEntry.class);
        verify(inventoryLedgerRepository).save(ledgerEntryCaptor.capture());
        InventoryLedgerEntry capturedLedgerEntry = ledgerEntryCaptor.getValue();
        assertEquals(sku, capturedLedgerEntry.getSku());
        assertEquals(-quantityPicked, capturedLedgerEntry.getQuantityChange());
        assertEquals(orderId, capturedLedgerEntry.getSourceReference());
        assertEquals(com.paklog.inventory.domain.model.ChangeType.PICK, capturedLedgerEntry.getChangeType());
    }

    @Test
    @DisplayName("Should throw ProductStockNotFoundException when processing item picked for non-existent product")
    void processItemPicked_ProductNotFound_ThrowsException() {
        // Arrange
        int quantityPicked = 10;
        String orderId = "ORDER001";

        when(productStockRepository.findBySku(sku)).thenReturn(Optional.empty());

        // Act & Assert
        ProductStockNotFoundException exception = assertThrows(ProductStockNotFoundException.class, () ->
                inventoryCommandService.processItemPicked(sku, quantityPicked, orderId));
        assertEquals("ProductStock not found for SKU: " + sku, exception.getMessage());
        assertEquals(sku, exception.getSku());

        verify(productStockRepository, times(1)).findBySku(sku);
        verify(productStockRepository, never()).save(any(ProductStock.class));
        verify(inventoryLedgerRepository, never()).save(any(InventoryLedgerEntry.class));
    }

    @Test
    @DisplayName("Should receive stock for existing product and save ledger entry and product stock")
    void receiveStock_ExistingProduct_SavesEntitiesAndReturnsUpdatedProductStock() {
        // Arrange
        int quantityReceived = 50;
        String receiptId = "RECEIPT001";

        when(productStockRepository.findBySku(sku)).thenReturn(Optional.of(existingProductStock));
        when(productStockRepository.save(any(ProductStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryLedgerRepository.save(any(InventoryLedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductStock updatedProductStock = inventoryCommandService.receiveStock(sku, quantityReceived, receiptId);

        // Assert
        assertNotNull(updatedProductStock);
        assertEquals(sku, updatedProductStock.getSku());
        assertEquals(150, updatedProductStock.getQuantityOnHand()); // 100 + 50
        assertEquals(20, updatedProductStock.getQuantityAllocated());
        assertEquals(130, updatedProductStock.getAvailableToPromise());

        verify(productStockRepository, times(1)).findBySku(sku);
        verify(productStockRepository, times(1)).save(any(ProductStock.class));
        verify(inventoryLedgerRepository, times(1)).save(any(InventoryLedgerEntry.class));

        ArgumentCaptor<InventoryLedgerEntry> ledgerEntryCaptor = ArgumentCaptor.forClass(InventoryLedgerEntry.class);
        verify(inventoryLedgerRepository).save(ledgerEntryCaptor.capture());
        InventoryLedgerEntry capturedLedgerEntry = ledgerEntryCaptor.getValue();
        assertEquals(sku, capturedLedgerEntry.getSku());
        assertEquals(quantityReceived, capturedLedgerEntry.getQuantityChange());
        assertEquals(receiptId, capturedLedgerEntry.getSourceReference());
        assertEquals(com.paklog.inventory.domain.model.ChangeType.RECEIPT, capturedLedgerEntry.getChangeType());
    }

    @Test
    @DisplayName("Should receive stock for new product, create it, and save ledger entry and product stock")
    void receiveStock_NewProduct_CreatesProductAndSavesEntitiesAndReturnsUpdatedProductStock() {
        // Arrange
        String newSku = "NEWPROD001";
        int quantityReceived = 75;
        String receiptId = "RECEIPT002";

        when(productStockRepository.findBySku(newSku)).thenReturn(Optional.empty());
        when(productStockRepository.save(any(ProductStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryLedgerRepository.save(any(InventoryLedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductStock newProductStock = inventoryCommandService.receiveStock(newSku, quantityReceived, receiptId);

        // Assert
        assertNotNull(newProductStock);
        assertEquals(newSku, newProductStock.getSku());
        assertEquals(quantityReceived, newProductStock.getQuantityOnHand());
        assertEquals(0, newProductStock.getQuantityAllocated());
        assertEquals(quantityReceived, newProductStock.getAvailableToPromise());

        verify(productStockRepository, times(1)).findBySku(newSku);
        verify(productStockRepository, times(1)).save(any(ProductStock.class));
        verify(inventoryLedgerRepository, times(1)).save(any(InventoryLedgerEntry.class));

        ArgumentCaptor<InventoryLedgerEntry> ledgerEntryCaptor = ArgumentCaptor.forClass(InventoryLedgerEntry.class);
        verify(inventoryLedgerRepository).save(ledgerEntryCaptor.capture());
        InventoryLedgerEntry capturedLedgerEntry = ledgerEntryCaptor.getValue();
        assertEquals(newSku, capturedLedgerEntry.getSku());
        assertEquals(quantityReceived, capturedLedgerEntry.getQuantityChange());
        assertEquals(receiptId, capturedLedgerEntry.getSourceReference());
        assertEquals(com.paklog.inventory.domain.model.ChangeType.RECEIPT, capturedLedgerEntry.getChangeType());

        // Verify that the initial event for the new product is also recorded
        List<StockLevelChangedEvent> events = newProductStock.getUncommittedEvents().stream()
                .filter(e -> e instanceof StockLevelChangedEvent)
                .map(e -> (StockLevelChangedEvent) e)
                .toList();
        assertEquals(0, events.size()); // Events are committed after service operation
    }

    @Test
    @DisplayName("Should increase quantity on hand for existing product")
    void increaseQuantityOnHand_ExistingProduct_IncreasesQuantity() {
        // Arrange
        int quantityToAdd = 25;
        when(productStockRepository.findBySku(sku)).thenReturn(Optional.of(existingProductStock));
        when(productStockRepository.save(any(ProductStock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        inventoryCommandService.increaseQuantityOnHand(sku, quantityToAdd);

        // Assert
        assertEquals(125, existingProductStock.getQuantityOnHand()); // 100 + 25
        verify(productStockRepository, times(1)).findBySku(sku);
        verify(productStockRepository, times(1)).save(any(ProductStock.class));
    }

    @Test
    @DisplayName("Should increase quantity on hand for new product, creating it")
    void increaseQuantityOnHand_NewProduct_CreatesProductAndIncreasesQuantity() {
        // Arrange
        String newSku = "NEWPROD002";
        int quantityToAdd = 50;
        when(productStockRepository.findBySku(newSku)).thenReturn(Optional.empty());
        when(productStockRepository.save(any(ProductStock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        inventoryCommandService.increaseQuantityOnHand(newSku, quantityToAdd);

        // Assert
        ArgumentCaptor<ProductStock> productStockCaptor = ArgumentCaptor.forClass(ProductStock.class);
        verify(productStockRepository).save(productStockCaptor.capture());
        ProductStock capturedProductStock = productStockCaptor.getValue();

        assertEquals(newSku, capturedProductStock.getSku());
        assertEquals(quantityToAdd, capturedProductStock.getQuantityOnHand());
        assertEquals(0, capturedProductStock.getQuantityAllocated());
        assertEquals(quantityToAdd, capturedProductStock.getAvailableToPromise());

        verify(productStockRepository, times(1)).findBySku(newSku);
        verify(productStockRepository, times(1)).save(any(ProductStock.class));
    }

    @Test
    @DisplayName("Should decrease quantity on hand for existing product")
    void decreaseQuantityOnHand_ExistingProduct_DecreasesQuantity() {
        // Arrange
        int quantityToDecrease = 15;
        when(productStockRepository.findBySku(sku)).thenReturn(Optional.of(existingProductStock));
        when(productStockRepository.save(any(ProductStock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        inventoryCommandService.decreaseQuantityOnHand(sku, quantityToDecrease);

        // Assert
        assertEquals(85, existingProductStock.getQuantityOnHand()); // 100 - 15
        verify(productStockRepository, times(1)).findBySku(sku);
        verify(productStockRepository, times(1)).save(any(ProductStock.class));
    }

    @Test
    @DisplayName("Should throw ProductStockNotFoundException when decreasing quantity on hand for non-existent product")
    void decreaseQuantityOnHand_ProductNotFound_ThrowsException() {
        // Arrange
        int quantityToDecrease = 10;
        when(productStockRepository.findBySku(sku)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ProductStockNotFoundException.class, () ->
                inventoryCommandService.decreaseQuantityOnHand(sku, quantityToDecrease));

        verify(productStockRepository, times(1)).findBySku(sku);
        verify(productStockRepository, never()).save(any(ProductStock.class));
        verify(outboxRepository, never()).saveAll(any());
    }
}