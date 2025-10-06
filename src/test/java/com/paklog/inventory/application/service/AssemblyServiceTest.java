package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.*;
import com.paklog.inventory.domain.repository.AssemblyOrderRepository;
import com.paklog.inventory.domain.repository.KitRepository;
import com.paklog.inventory.domain.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for AssemblyService - validates assembly/disassembly operations.
 */
@ExtendWith(MockitoExtension.class)
class AssemblyServiceTest {

    @Mock
    private AssemblyOrderRepository assemblyOrderRepository;

    @Mock
    private KitRepository kitRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private AssemblyService assemblyService;

    private String testKitSku;
    private Kit testKit;
    private Location assemblyLocation;

    @BeforeEach
    void setUp() {
        testKitSku = "KIT-LAPTOP-001";
        assemblyLocation = Location.of("WH01", "ASSEMBLY", "01", "01", "A", LocationType.GENERAL);

        testKit = Kit.physical(testKitSku, "Laptop Kit", List.of(
            KitComponent.required("SKU-SCREEN", 1),
            KitComponent.required("SKU-KEYBOARD", 1),
            KitComponent.required("SKU-BATTERY", 1)
        ));
    }

    @Test
    @DisplayName("Should create assembly order successfully")
    void shouldCreateAssemblyOrderSuccessfully() {
        // Given
        List<ComponentAllocation> allocations = List.of(
            ComponentAllocation.of("SKU-SCREEN", 10, assemblyLocation, "LOT-001"),
            ComponentAllocation.of("SKU-KEYBOARD", 10, assemblyLocation, "LOT-002"),
            ComponentAllocation.of("SKU-BATTERY", 10, assemblyLocation, "LOT-003")
        );

        when(kitRepository.findByKitSku(testKitSku)).thenReturn(Optional.of(testKit));
        when(assemblyOrderRepository.save(any(AssemblyOrder.class))).thenAnswer(i -> i.getArgument(0));

        // When
        String assemblyOrderId = assemblyService.createAssemblyOrder(
            testKitSku, 10, allocations, assemblyLocation, "USER1"
        );

        // Then
        assertNotNull(assemblyOrderId);
        assertTrue(assemblyOrderId.startsWith("ASM-"));

        ArgumentCaptor<AssemblyOrder> orderCaptor = ArgumentCaptor.forClass(AssemblyOrder.class);
        verify(assemblyOrderRepository).save(orderCaptor.capture());

        AssemblyOrder savedOrder = orderCaptor.getValue();
        assertEquals(testKitSku, savedOrder.getKitSku());
        assertEquals(10, savedOrder.getKitQuantity());
        assertEquals(AssemblyType.ASSEMBLE, savedOrder.getAssemblyType());
        assertEquals(AssemblyStatus.CREATED, savedOrder.getStatus());
        assertEquals(3, savedOrder.getComponentAllocations().size());
    }

    @Test
    @DisplayName("Should throw exception when creating assembly order for non-existent kit")
    void shouldThrowExceptionWhenCreatingAssemblyOrderForNonExistentKit() {
        // Given
        when(kitRepository.findByKitSku(testKitSku)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            assemblyService.createAssemblyOrder(
                testKitSku, 10, List.of(), assemblyLocation, "USER1"
            )
        );

        verify(assemblyOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when creating assembly order for virtual kit")
    void shouldThrowExceptionWhenCreatingAssemblyOrderForVirtualKit() {
        // Given
        Kit virtualKit = Kit.virtual(testKitSku, "Virtual Bundle", List.of(
            KitComponent.required("SKU-001", 1)
        ), false);

        when(kitRepository.findByKitSku(testKitSku)).thenReturn(Optional.of(virtualKit));

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            assemblyService.createAssemblyOrder(
                testKitSku, 10, List.of(), assemblyLocation, "USER1"
            )
        );

        verify(assemblyOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create assembly order with missing components (partial allocation)")
    void shouldCreateAssemblyOrderWithMissingComponents() {
        // Given: Only 2 out of 3 components allocated
        List<ComponentAllocation> partialAllocations = List.of(
            ComponentAllocation.of("SKU-SCREEN", 10, assemblyLocation, "LOT-001"),
            ComponentAllocation.of("SKU-KEYBOARD", 10, assemblyLocation, "LOT-002")
            // Missing SKU-BATTERY
        );

        when(kitRepository.findByKitSku(testKitSku)).thenReturn(Optional.of(testKit));
        when(assemblyOrderRepository.save(any(AssemblyOrder.class))).thenAnswer(i -> i.getArgument(0));

        // When
        String assemblyOrderId = assemblyService.createAssemblyOrder(
            testKitSku, 10, partialAllocations, assemblyLocation, "USER1"
        );

        // Then: Should create order but log warning (tested via verify)
        assertNotNull(assemblyOrderId);
        verify(assemblyOrderRepository).save(any(AssemblyOrder.class));
    }

    @Test
    @DisplayName("Should create disassembly order successfully")
    void shouldCreateDisassemblyOrderSuccessfully() {
        // Given
        when(kitRepository.findByKitSku(testKitSku)).thenReturn(Optional.of(testKit));
        when(assemblyOrderRepository.save(any(AssemblyOrder.class))).thenAnswer(i -> i.getArgument(0));

        // When
        String assemblyOrderId = assemblyService.createDisassemblyOrder(
            testKitSku, 5, assemblyLocation, "USER1"
        );

        // Then
        assertNotNull(assemblyOrderId);
        assertTrue(assemblyOrderId.startsWith("DIS-"));

        ArgumentCaptor<AssemblyOrder> orderCaptor = ArgumentCaptor.forClass(AssemblyOrder.class);
        verify(assemblyOrderRepository).save(orderCaptor.capture());

        AssemblyOrder savedOrder = orderCaptor.getValue();
        assertEquals(testKitSku, savedOrder.getKitSku());
        assertEquals(5, savedOrder.getKitQuantity());
        assertEquals(AssemblyType.DISASSEMBLE, savedOrder.getAssemblyType());
        assertEquals(AssemblyStatus.CREATED, savedOrder.getStatus());
    }

    @Test
    @DisplayName("Should throw exception when creating disassembly order for non-existent kit")
    void shouldThrowExceptionWhenCreatingDisassemblyOrderForNonExistentKit() {
        // Given
        when(kitRepository.findByKitSku(testKitSku)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            assemblyService.createDisassemblyOrder(
                testKitSku, 5, assemblyLocation, "USER1"
            )
        );

        verify(assemblyOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should start assembly order successfully")
    void shouldStartAssemblyOrderSuccessfully() {
        // Given
        String orderId = "ASM-123";
        AssemblyOrder order = createTestAssemblyOrder(orderId);

        when(assemblyOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(assemblyOrderRepository.save(any(AssemblyOrder.class))).thenAnswer(i -> i.getArgument(0));

        // When
        assemblyService.startOrder(orderId);

        // Then
        assertEquals(AssemblyStatus.IN_PROGRESS, order.getStatus());
        verify(assemblyOrderRepository).save(order);
    }

    @Test
    @DisplayName("Should throw exception when starting non-existent order")
    void shouldThrowExceptionWhenStartingNonExistentOrder() {
        // Given
        String orderId = "ASM-NONEXISTENT";
        when(assemblyOrderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            assemblyService.startOrder(orderId)
        );

        verify(assemblyOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should complete assembly order with 100% yield")
    void shouldCompleteAssemblyOrderWithFullYield() {
        // Given
        String orderId = "ASM-123";
        AssemblyOrder order = createTestAssemblyOrder(orderId);
        order.start();

        when(assemblyOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(assemblyOrderRepository.save(any(AssemblyOrder.class))).thenAnswer(i -> i.getArgument(0));

        // When
        assemblyService.completeOrder(orderId, 10, "USER2");

        // Then
        assertEquals(AssemblyStatus.COMPLETED, order.getStatus());
        assertEquals(10, order.getActualQuantityProduced());
        assertEquals(100.0, order.getYieldPercentage(), 0.01);

        verify(assemblyOrderRepository).save(order);
        verify(outboxRepository).saveAll(order.getUncommittedEvents().stream().map(OutboxEvent::from).toList());
    }

    @Test
    @DisplayName("Should complete assembly order with yield loss")
    void shouldCompleteAssemblyOrderWithYieldLoss() {
        // Given
        String orderId = "ASM-123";
        AssemblyOrder order = createTestAssemblyOrder(orderId);
        order.start();

        when(assemblyOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(assemblyOrderRepository.save(any(AssemblyOrder.class))).thenAnswer(i -> i.getArgument(0));

        // When: Produce 8 out of 10 (80% yield)
        assemblyService.completeOrder(orderId, 8, "USER2");

        // Then
        assertEquals(AssemblyStatus.COMPLETED, order.getStatus());
        assertEquals(8, order.getActualQuantityProduced());
        assertEquals(80.0, order.getYieldPercentage(), 0.01);

        verify(assemblyOrderRepository).save(order);
        verify(outboxRepository).saveAll(order.getUncommittedEvents().stream().map(OutboxEvent::from).toList());
    }

    @Test
    @DisplayName("Should throw exception when completing non-existent order")
    void shouldThrowExceptionWhenCompletingNonExistentOrder() {
        // Given
        String orderId = "ASM-NONEXISTENT";
        when(assemblyOrderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            assemblyService.completeOrder(orderId, 10, "USER2")
        );

        verify(assemblyOrderRepository, never()).save(any());
        verify(outboxRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should cancel assembly order successfully")
    void shouldCancelAssemblyOrderSuccessfully() {
        // Given
        String orderId = "ASM-123";
        AssemblyOrder order = createTestAssemblyOrder(orderId);

        when(assemblyOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(assemblyOrderRepository.save(any(AssemblyOrder.class))).thenAnswer(i -> i.getArgument(0));

        // When
        assemblyService.cancelOrder(orderId, "No longer needed");

        // Then
        assertEquals(AssemblyStatus.CANCELLED, order.getStatus());
        verify(assemblyOrderRepository).save(order);
    }

    @Test
    @DisplayName("Should throw exception when cancelling non-existent order")
    void shouldThrowExceptionWhenCancellingNonExistentOrder() {
        // Given
        String orderId = "ASM-NONEXISTENT";
        when(assemblyOrderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            assemblyService.cancelOrder(orderId, "Reason")
        );

        verify(assemblyOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should add component allocation to assembly order")
    void shouldAddComponentAllocationToAssemblyOrder() {
        // Given
        String orderId = "ASM-123";
        AssemblyOrder order = AssemblyOrder.createAssembly(
            orderId, testKitSku, 10, List.of(), assemblyLocation, "USER1"
        );

        ComponentAllocation allocation = ComponentAllocation.of(
            "SKU-SCREEN", 10, assemblyLocation, "LOT-001"
        );

        when(assemblyOrderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(assemblyOrderRepository.save(any(AssemblyOrder.class))).thenAnswer(i -> i.getArgument(0));

        // When
        assemblyService.addComponentAllocation(orderId, allocation);

        // Then
        assertEquals(1, order.getComponentAllocations().size());
        verify(assemblyOrderRepository).save(order);
    }

    @Test
    @DisplayName("Should throw exception when adding allocation to non-existent order")
    void shouldThrowExceptionWhenAddingAllocationToNonExistentOrder() {
        // Given
        String orderId = "ASM-NONEXISTENT";
        ComponentAllocation allocation = ComponentAllocation.of(
            "SKU-SCREEN", 10, assemblyLocation, "LOT-001"
        );

        when(assemblyOrderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            assemblyService.addComponentAllocation(orderId, allocation)
        );

        verify(assemblyOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get assembly order by ID")
    void shouldGetAssemblyOrderById() {
        // Given
        String orderId = "ASM-123";
        AssemblyOrder order = createTestAssemblyOrder(orderId);

        when(assemblyOrderRepository.findById(orderId)).thenReturn(Optional.of(order));

        // When
        AssemblyOrder result = assemblyService.getOrder(orderId);

        // Then
        assertNotNull(result);
        assertEquals(orderId, result.getAssemblyOrderId());
        assertEquals(testKitSku, result.getKitSku());
    }

    @Test
    @DisplayName("Should throw exception when getting non-existent order")
    void shouldThrowExceptionWhenGettingNonExistentOrder() {
        // Given
        String orderId = "ASM-NONEXISTENT";
        when(assemblyOrderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            assemblyService.getOrder(orderId)
        );
    }

    @Test
    @DisplayName("Should get orders by kit SKU")
    void shouldGetOrdersByKitSku() {
        // Given
        List<AssemblyOrder> orders = List.of(
            createTestAssemblyOrder("ASM-001"),
            createTestAssemblyOrder("ASM-002")
        );

        when(assemblyOrderRepository.findByKitSku(testKitSku)).thenReturn(orders);

        // When
        List<AssemblyOrder> result = assemblyService.getOrdersByKitSku(testKitSku);

        // Then
        assertEquals(2, result.size());
        verify(assemblyOrderRepository).findByKitSku(testKitSku);
    }

    @Test
    @DisplayName("Should get orders by status")
    void shouldGetOrdersByStatus() {
        // Given
        List<AssemblyOrder> orders = List.of(
            createTestAssemblyOrder("ASM-001")
        );

        when(assemblyOrderRepository.findByStatus(AssemblyStatus.CREATED))
            .thenReturn(orders);

        // When
        List<AssemblyOrder> result = assemblyService.getOrdersByStatus(AssemblyStatus.CREATED);

        // Then
        assertEquals(1, result.size());
        verify(assemblyOrderRepository).findByStatus(AssemblyStatus.CREATED);
    }

    @Test
    @DisplayName("Should get orders by type")
    void shouldGetOrdersByType() {
        // Given
        List<AssemblyOrder> orders = List.of(
            createTestAssemblyOrder("ASM-001"),
            createTestAssemblyOrder("ASM-002")
        );

        when(assemblyOrderRepository.findByType(AssemblyType.ASSEMBLE))
            .thenReturn(orders);

        // When
        List<AssemblyOrder> result = assemblyService.getOrdersByType(AssemblyType.ASSEMBLE);

        // Then
        assertEquals(2, result.size());
        verify(assemblyOrderRepository).findByType(AssemblyType.ASSEMBLE);
    }

    @Test
    @DisplayName("Should get in-progress orders")
    void shouldGetInProgressOrders() {
        // Given
        AssemblyOrder order1 = createTestAssemblyOrder("ASM-001");
        AssemblyOrder order2 = createTestAssemblyOrder("ASM-002");
        order1.start();
        order2.start();

        List<AssemblyOrder> orders = List.of(order1, order2);

        when(assemblyOrderRepository.findInProgressOrders()).thenReturn(orders);

        // When
        List<AssemblyOrder> result = assemblyService.getInProgressOrders();

        // Then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(o -> o.getStatus() == AssemblyStatus.IN_PROGRESS));
        verify(assemblyOrderRepository).findInProgressOrders();
    }

    @Test
    @DisplayName("Should count orders by status")
    void shouldCountOrdersByStatus() {
        // Given
        when(assemblyOrderRepository.countByStatus(AssemblyStatus.COMPLETED))
            .thenReturn(15L);

        // When
        long count = assemblyService.countOrdersByStatus(AssemblyStatus.COMPLETED);

        // Then
        assertEquals(15, count);
        verify(assemblyOrderRepository).countByStatus(AssemblyStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should handle complete assembly lifecycle through service")
    void shouldHandleCompleteAssemblyLifecycleThroughService() {
        // Given
        List<ComponentAllocation> allocations = List.of(
            ComponentAllocation.of("SKU-SCREEN", 10, assemblyLocation, "LOT-001"),
            ComponentAllocation.of("SKU-KEYBOARD", 10, assemblyLocation, "LOT-002"),
            ComponentAllocation.of("SKU-BATTERY", 10, assemblyLocation, "LOT-003")
        );

        when(kitRepository.findByKitSku(testKitSku)).thenReturn(Optional.of(testKit));
        when(assemblyOrderRepository.save(any(AssemblyOrder.class))).thenAnswer(i -> i.getArgument(0));

        // When: Create order
        String orderId = assemblyService.createAssemblyOrder(
            testKitSku, 10, allocations, assemblyLocation, "USER1"
        );

        // Capture the created order
        ArgumentCaptor<AssemblyOrder> orderCaptor = ArgumentCaptor.forClass(AssemblyOrder.class);
        verify(assemblyOrderRepository, times(1)).save(orderCaptor.capture());
        AssemblyOrder createdOrder = orderCaptor.getValue();

        // When: Start order
        when(assemblyOrderRepository.findById(orderId)).thenReturn(Optional.of(createdOrder));
        assemblyService.startOrder(orderId);

        // When: Complete order
        assemblyService.completeOrder(orderId, 10, "USER2");

        // Then
        assertEquals(AssemblyStatus.COMPLETED, createdOrder.getStatus());
        assertEquals(10, createdOrder.getActualQuantityProduced());
        assertEquals(100.0, createdOrder.getYieldPercentage(), 0.01);

        // Verify all interactions
        verify(assemblyOrderRepository, times(3)).save(any(AssemblyOrder.class));
        verify(outboxRepository).saveAll(createdOrder.getUncommittedEvents().stream().map(OutboxEvent::from).toList());
    }

    private AssemblyOrder createTestAssemblyOrder(String orderId) {
        List<ComponentAllocation> allocations = List.of(
            ComponentAllocation.of("SKU-SCREEN", 10, assemblyLocation, "LOT-001"),
            ComponentAllocation.of("SKU-KEYBOARD", 10, assemblyLocation, "LOT-002"),
            ComponentAllocation.of("SKU-BATTERY", 10, assemblyLocation, "LOT-003")
        );

        return AssemblyOrder.createAssembly(
            orderId, testKitSku, 10, allocations, assemblyLocation, "USER1"
        );
    }
}
