package com.paklog.inventory.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Container (LPN) - validates container lifecycle and item management.
 */
class ContainerTest {

    @Test
    @DisplayName("Should create pallet container")
    void shouldCreatePalletContainer() {
        // Given
        Location location = Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL);

        // When
        Container container = Container.create(ContainerType.PALLET, location, "USER1");

        // Then
        assertNotNull(container.getLpn());
        assertEquals(ContainerType.PALLET, container.getType());
        assertEquals(location, container.getCurrentLocation());
        assertEquals(ContainerStatus.ACTIVE, container.getStatus());
        assertTrue(container.isEmpty());
    }

    @Test
    @DisplayName("Should create container with specific LPN")
    void shouldCreateContainerWithSpecificLpn() {
        // Given
        String customLpn = "LPN-CUSTOM-001";
        Location location = Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL);

        // When
        Container container = Container.createWithLPN(customLpn, ContainerType.CASE,
            location, "USER1");

        // Then
        assertEquals(customLpn, container.getLpn());
    }

    @Test
    @DisplayName("Should add item to container")
    void shouldAddItemToContainer() {
        // Given
        Container container = createTestContainer();
        Location sourceLocation = Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL);

        // When
        container.addItem("SKU-001", 100, "LOT-12345", sourceLocation);

        // Then
        assertEquals(1, container.getItems().size());
        assertEquals(100, container.getTotalQuantity());
        assertFalse(container.isEmpty());
    }

    @Test
    @DisplayName("Should add multiple items to create mixed-SKU container")
    void shouldAddMultipleItemsToCreateMixedSkuContainer() {
        // Given
        Container container = createTestContainer();
        Location location = Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL);

        // When
        container.addItem("SKU-001", 100, "LOT-A", location);
        container.addItem("SKU-002", 50, "LOT-B", location);
        container.addItem("SKU-003", 75, "LOT-C", location);

        // Then
        assertEquals(3, container.getItems().size());
        assertEquals(225, container.getTotalQuantity());
        assertTrue(container.isMixedSKU());
    }

    @Test
    @DisplayName("Should remove item from container")
    void shouldRemoveItemFromContainer() {
        // Given
        Container container = createTestContainer();
        Location location = Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL);
        container.addItem("SKU-001", 100, "LOT-001", location);

        // When
        container.removeItem("SKU-001", 30, "LOT-001");

        // Then
        assertEquals(70, container.getTotalQuantity());
    }

    @Test
    @DisplayName("Should move container to new location")
    void shouldMoveContainerToNewLocation() {
        // Given
        Container container = createTestContainer();
        Location newLocation = Location.of("WH01", "B", "05", "03", "C", LocationType.GENERAL);

        // When
        container.moveTo(newLocation);

        // Then
        assertEquals(newLocation, container.getCurrentLocation());
    }

    @Test
    @DisplayName("Should close container")
    void shouldCloseContainer() {
        // Given
        Container container = createTestContainer();
        container.addItem("SKU-001", 100, "LOT-001",
            Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL));

        // When
        container.close();

        // Then
        assertEquals(ContainerStatus.CLOSED, container.getStatus());
    }

    @Test
    @DisplayName("Should ship closed container")
    void shouldShipClosedContainer() {
        // Given
        Container container = createTestContainer();
        container.addItem("SKU-001", 100, "LOT-001",
            Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL));
        container.close();

        // When
        container.ship();

        // Then
        assertEquals(ContainerStatus.SHIPPED, container.getStatus());
    }

    @Test
    @DisplayName("Should empty container")
    void shouldEmptyContainer() {
        // Given
        Container container = createTestContainer();
        Location location = Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL);
        container.addItem("SKU-001", 100, "LOT-001", location);
        container.addItem("SKU-002", 50, "LOT-002", location);

        // When
        container.empty();

        // Then
        assertTrue(container.isEmpty());
        assertEquals(0, container.getTotalQuantity());
        assertEquals(ContainerStatus.ACTIVE, container.getStatus());
    }

    @Test
    @DisplayName("Should nest container inside parent")
    void shouldNestContainerInsideParent() {
        // Given
        Container container = createTestContainer();
        String parentLpn = "LPN-PARENT-001";

        // When
        container.nestInside(parentLpn);

        // Then
        assertEquals(parentLpn, container.getParentLpn());
    }

    @Test
    @DisplayName("Should detect single-SKU container")
    void shouldDetectSingleSkuContainer() {
        // Given
        Container container = createTestContainer();
        Location location = Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL);
        container.addItem("SKU-001", 100, "LOT-A", location);
        container.addItem("SKU-001", 50, "LOT-B", location);

        // When
        boolean isMixed = container.isMixedSKU();

        // Then
        assertFalse(isMixed);
    }

    @Test
    @DisplayName("Should detect mixed-SKU container")
    void shouldDetectMixedSkuContainer() {
        // Given
        Container container = createTestContainer();
        Location location = Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL);
        container.addItem("SKU-001", 100, "LOT-A", location);
        container.addItem("SKU-002", 50, "LOT-B", location);

        // When
        boolean isMixed = container.isMixedSKU();

        // Then
        assertTrue(isMixed);
    }

    @Test
    @DisplayName("Should handle container lifecycle")
    void shouldHandleContainerLifecycle() {
        // Given
        Container container = createTestContainer();
        Location location = Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL);

        // When: Full lifecycle
        container.addItem("SKU-001", 100, "LOT-001", location);
        container.close();
        container.ship();

        // Then
        assertEquals(ContainerStatus.SHIPPED, container.getStatus());
        assertEquals(100, container.getTotalQuantity());
    }

    private Container createTestContainer() {
        Location location = Location.of("WH01", "A", "01", "01", "A", LocationType.GENERAL);
        return Container.create(ContainerType.PALLET, location, "USER1");
    }
}
