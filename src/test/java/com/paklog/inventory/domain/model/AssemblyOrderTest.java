package com.paklog.inventory.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AssemblyOrder - validates assembly/disassembly workflow logic.
 */
class AssemblyOrderTest {

    @Test
    @DisplayName("Should create assembly order")
    void shouldCreateAssemblyOrder() {
        // Given
        Location location = Location.of("WH01", "ASSEMBLY", "01", "01", "A", LocationType.GENERAL);
        List<ComponentAllocation> allocations = List.of(
            ComponentAllocation.of("SKU-SCREEN", 10, location, "LOT-001"),
            ComponentAllocation.of("SKU-KEYBOARD", 10, location, "LOT-002")
        );

        // When
        AssemblyOrder order = AssemblyOrder.createAssembly(
            "ASM-001", "KIT-LAPTOP-001", 10, allocations, location, "USER1"
        );

        // Then
        assertEquals("ASM-001", order.getAssemblyOrderId());
        assertEquals(AssemblyType.ASSEMBLE, order.getAssemblyType());
        assertEquals("KIT-LAPTOP-001", order.getKitSku());
        assertEquals(10, order.getKitQuantity());
        assertEquals(AssemblyStatus.CREATED, order.getStatus());
        assertEquals("USER1", order.getCreatedBy());
        assertTrue(order.isAssembly());
        assertFalse(order.isDisassembly());
        assertEquals(2, order.getComponentAllocations().size());
    }

    @Test
    @DisplayName("Should create disassembly order")
    void shouldCreateDisassemblyOrder() {
        // Given
        Location location = Location.of("WH01", "ASSEMBLY", "01", "01", "A", LocationType.GENERAL);

        // When
        AssemblyOrder order = AssemblyOrder.createDisassembly(
            "DSM-001", "KIT-LAPTOP-001", 5, location, "USER1"
        );

        // Then
        assertEquals(AssemblyType.DISASSEMBLE, order.getAssemblyType());
        assertTrue(order.isDisassembly());
        assertFalse(order.isAssembly());
        assertEquals(0, order.getComponentAllocations().size());
    }

    @Test
    @DisplayName("Should throw exception when creating order with null ID")
    void shouldThrowExceptionWhenCreatingOrderWithNullId() {
        // Given
        Location location = Location.of("WH01", "ASSEMBLY", "01", "01", "A", LocationType.GENERAL);

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            AssemblyOrder.createAssembly(null, "KIT-001", 10, List.of(), location, "USER1")
        );
    }

    @Test
    @DisplayName("Should throw exception when creating order with zero quantity")
    void shouldThrowExceptionWhenCreatingOrderWithZeroQuantity() {
        // Given
        Location location = Location.of("WH01", "ASSEMBLY", "01", "01", "A", LocationType.GENERAL);

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            AssemblyOrder.createAssembly("ASM-001", "KIT-001", 0, List.of(), location, "USER1")
        );
    }

    @Test
    @DisplayName("Should start assembly order")
    void shouldStartAssemblyOrder() {
        // Given
        AssemblyOrder order = createTestAssemblyOrder();

        // When
        order.start();

        // Then
        assertEquals(AssemblyStatus.IN_PROGRESS, order.getStatus());
        assertTrue(order.getStartedAt().isPresent());
    }

    @Test
    @DisplayName("Should throw exception when starting assembly without allocations")
    void shouldThrowExceptionWhenStartingAssemblyWithoutAllocations() {
        // Given: Assembly order with no component allocations
        Location location = Location.of("WH01", "ASSEMBLY", "01", "01", "A", LocationType.GENERAL);
        AssemblyOrder order = AssemblyOrder.createAssembly(
            "ASM-001", "KIT-001", 10, List.of(), location, "USER1"
        );

        // When/Then
        assertThrows(IllegalStateException.class, order::start);
    }

    @Test
    @DisplayName("Should throw exception when starting already started order")
    void shouldThrowExceptionWhenStartingAlreadyStartedOrder() {
        // Given
        AssemblyOrder order = createTestAssemblyOrder();
        order.start();

        // When/Then
        assertThrows(IllegalStateException.class, order::start);
    }

    @Test
    @DisplayName("Should complete assembly order with exact quantity")
    void shouldCompleteAssemblyOrderWithExactQuantity() {
        // Given
        AssemblyOrder order = createTestAssemblyOrder();
        order.start();

        // When
        order.complete(10, "USER2");

        // Then
        assertEquals(AssemblyStatus.COMPLETED, order.getStatus());
        assertEquals(10, order.getActualQuantityProduced());
        assertEquals(100.0, order.getYieldPercentage(), 0.01);
        assertTrue(order.getCompletedAt().isPresent());
        assertEquals("USER2", order.getCompletedBy().orElse(null));
    }

    @Test
    @DisplayName("Should complete assembly order with yield loss")
    void shouldCompleteAssemblyOrderWithYieldLoss() {
        // Given
        AssemblyOrder order = createTestAssemblyOrder();
        order.start();

        // When: Produce only 9 out of 10 (90% yield)
        order.complete(9, "USER2");

        // Then
        assertEquals(9, order.getActualQuantityProduced());
        assertEquals(90.0, order.getYieldPercentage(), 0.01);
    }

    @Test
    @DisplayName("Should throw exception when completing without starting")
    void shouldThrowExceptionWhenCompletingWithoutStarting() {
        // Given
        AssemblyOrder order = createTestAssemblyOrder();

        // When/Then
        assertThrows(IllegalStateException.class, () ->
            order.complete(10, "USER2")
        );
    }

    @Test
    @DisplayName("Should throw exception when actual quantity exceeds planned")
    void shouldThrowExceptionWhenActualQuantityExceedsPlanned() {
        // Given
        AssemblyOrder order = createTestAssemblyOrder();
        order.start();

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            order.complete(15, "USER2") // Planned was 10
        );
    }

    @Test
    @DisplayName("Should cancel assembly order in CREATED status")
    void shouldCancelAssemblyOrderInCreatedStatus() {
        // Given
        AssemblyOrder order = createTestAssemblyOrder();

        // When
        order.cancel("No longer needed");

        // Then
        assertEquals(AssemblyStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("Should cancel assembly order in IN_PROGRESS status")
    void shouldCancelAssemblyOrderInInProgressStatus() {
        // Given
        AssemblyOrder order = createTestAssemblyOrder();
        order.start();

        // When
        order.cancel("Emergency stop");

        // Then
        assertEquals(AssemblyStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("Should throw exception when cancelling completed order")
    void shouldThrowExceptionWhenCancellingCompletedOrder() {
        // Given
        AssemblyOrder order = createTestAssemblyOrder();
        order.start();
        order.complete(10, "USER2");

        // When/Then
        assertThrows(IllegalStateException.class, () ->
            order.cancel("Cannot cancel")
        );
    }

    @Test
    @DisplayName("Should add component allocation to assembly order")
    void shouldAddComponentAllocationToAssemblyOrder() {
        // Given
        Location location = Location.of("WH01", "ASSEMBLY", "01", "01", "A", LocationType.GENERAL);
        AssemblyOrder order = AssemblyOrder.createAssembly(
            "ASM-001", "KIT-001", 10, List.of(), location, "USER1"
        );

        ComponentAllocation allocation = ComponentAllocation.of(
            "SKU-001", 10, location, "LOT-001"
        );

        // When
        order.addComponentAllocation(allocation);

        // Then
        assertEquals(1, order.getComponentAllocations().size());
    }

    @Test
    @DisplayName("Should throw exception when adding allocation to disassembly order")
    void shouldThrowExceptionWhenAddingAllocationToDisassemblyOrder() {
        // Given
        Location location = Location.of("WH01", "ASSEMBLY", "01", "01", "A", LocationType.GENERAL);
        AssemblyOrder order = AssemblyOrder.createDisassembly(
            "DSM-001", "KIT-001", 10, location, "USER1"
        );

        ComponentAllocation allocation = ComponentAllocation.of(
            "SKU-001", 10, location, "LOT-001"
        );

        // When/Then
        assertThrows(IllegalStateException.class, () ->
            order.addComponentAllocation(allocation)
        );
    }

    @Test
    @DisplayName("Should check if all components are allocated")
    void shouldCheckIfAllComponentsAreAllocated() {
        // Given
        Location location = Location.of("WH01", "ASSEMBLY", "01", "01", "A", LocationType.GENERAL);
        Kit kit = Kit.physical("KIT-001", "Test Kit", List.of(
            KitComponent.required("SKU-001", 1),
            KitComponent.required("SKU-002", 2)
        ));

        List<ComponentAllocation> allocations = List.of(
            ComponentAllocation.of("SKU-001", 10, location, "LOT-001"), // 10 kits
            ComponentAllocation.of("SKU-002", 20, location, "LOT-002")  // 10 kits (2 each)
        );

        AssemblyOrder order = AssemblyOrder.createAssembly(
            "ASM-001", "KIT-001", 10, allocations, location, "USER1"
        );

        // When
        boolean hasAll = order.hasAllComponents(kit);

        // Then
        assertTrue(hasAll);
    }

    @Test
    @DisplayName("Should detect missing component allocations")
    void shouldDetectMissingComponentAllocations() {
        // Given
        Location location = Location.of("WH01", "ASSEMBLY", "01", "01", "A", LocationType.GENERAL);
        Kit kit = Kit.physical("KIT-001", "Test Kit", List.of(
            KitComponent.required("SKU-001", 1),
            KitComponent.required("SKU-002", 2)
        ));

        List<ComponentAllocation> allocations = List.of(
            ComponentAllocation.of("SKU-001", 10, location, "LOT-001")
            // Missing SKU-002
        );

        AssemblyOrder order = AssemblyOrder.createAssembly(
            "ASM-001", "KIT-001", 10, allocations, location, "USER1"
        );

        // When
        boolean hasAll = order.hasAllComponents(kit);

        // Then
        assertFalse(hasAll);
    }

    @Test
    @DisplayName("Should calculate yield percentage correctly")
    void shouldCalculateYieldPercentageCorrectly() {
        // Given
        AssemblyOrder order = createTestAssemblyOrder();
        order.start();

        // When: Produce 8 out of 10 (80% yield)
        order.complete(8, "USER2");

        // Then
        assertEquals(80.0, order.getYieldPercentage(), 0.01);
    }

    @Test
    @DisplayName("Should return zero yield for uncompleted order")
    void shouldReturnZeroYieldForUncompletedOrder() {
        // Given
        AssemblyOrder order = createTestAssemblyOrder();

        // When
        double yield = order.getYieldPercentage();

        // Then
        assertEquals(0.0, yield, 0.01);
    }

    @Test
    @DisplayName("Should handle complete assembly lifecycle")
    void shouldHandleCompleteAssemblyLifecycle() {
        // Given
        AssemblyOrder order = createTestAssemblyOrder();

        // When: Full lifecycle
        assertEquals(AssemblyStatus.CREATED, order.getStatus());

        order.start();
        assertEquals(AssemblyStatus.IN_PROGRESS, order.getStatus());

        order.complete(10, "USER2");
        assertEquals(AssemblyStatus.COMPLETED, order.getStatus());

        // Then
        assertTrue(order.getStartedAt().isPresent());
        assertTrue(order.getCompletedAt().isPresent());
        assertEquals(10, order.getActualQuantityProduced());
        assertEquals(100.0, order.getYieldPercentage(), 0.01);
    }

    @Test
    @DisplayName("Should track disassembly order lifecycle")
    void shouldTrackDisassemblyOrderLifecycle() {
        // Given
        Location location = Location.of("WH01", "ASSEMBLY", "01", "01", "A", LocationType.GENERAL);
        AssemblyOrder order = AssemblyOrder.createDisassembly(
            "DSM-001", "KIT-001", 5, location, "USER1"
        );

        // When: Full lifecycle
        order.start();
        order.complete(5, "USER2");

        // Then
        assertEquals(AssemblyStatus.COMPLETED, order.getStatus());
        assertEquals(5, order.getActualQuantityProduced());
        assertTrue(order.isDisassembly());
    }

    private AssemblyOrder createTestAssemblyOrder() {
        Location location = Location.of("WH01", "ASSEMBLY", "01", "01", "A", LocationType.GENERAL);
        List<ComponentAllocation> allocations = List.of(
            ComponentAllocation.of("SKU-001", 10, location, "LOT-001")
        );
        return AssemblyOrder.createAssembly(
            "ASM-001", "KIT-001", 10, allocations, location, "USER1"
        );
    }
}
