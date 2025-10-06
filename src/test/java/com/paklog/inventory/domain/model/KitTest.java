package com.paklog.inventory.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Kit domain model - validates kit/BOM logic and assembly calculations.
 */
class KitTest {

    @Test
    @DisplayName("Should create physical kit")
    void shouldCreatePhysicalKit() {
        // Given
        List<KitComponent> components = List.of(
            KitComponent.required("SKU-SCREEN", 1),
            KitComponent.required("SKU-KEYBOARD", 1),
            KitComponent.required("SKU-BATTERY", 1)
        );

        // When
        Kit kit = Kit.physical("KIT-LAPTOP-001", "Laptop Kit", components);

        // Then
        assertEquals("KIT-LAPTOP-001", kit.getKitSku());
        assertEquals("Laptop Kit", kit.getKitDescription());
        assertEquals(KitType.PHYSICAL, kit.getKitType());
        assertTrue(kit.isPhysical());
        assertFalse(kit.isVirtual());
        assertFalse(kit.isAllowPartialKit());
        assertEquals(3, kit.getComponentCount());
        assertTrue(kit.isActive());
    }

    @Test
    @DisplayName("Should create virtual kit")
    void shouldCreateVirtualKit() {
        // Given
        List<KitComponent> components = List.of(
            KitComponent.required("SKU-ITEM1", 2),
            KitComponent.required("SKU-ITEM2", 3)
        );

        // When
        Kit kit = Kit.virtual("KIT-BUNDLE-001", "Virtual Bundle", components, true);

        // Then
        assertEquals(KitType.VIRTUAL, kit.getKitType());
        assertTrue(kit.isVirtual());
        assertFalse(kit.isPhysical());
        assertTrue(kit.isAllowPartialKit());
    }

    @Test
    @DisplayName("Should throw exception when creating kit with null SKU")
    void shouldThrowExceptionWhenCreatingKitWithNullSku() {
        // Given
        List<KitComponent> components = List.of(KitComponent.required("SKU-001", 1));

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            Kit.create(null, "Description", components, KitType.PHYSICAL, false)
        );
    }

    @Test
    @DisplayName("Should throw exception when creating kit with no components")
    void shouldThrowExceptionWhenCreatingKitWithNoComponents() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            Kit.create("KIT-001", "Description", List.of(), KitType.PHYSICAL, false)
        );
    }

    @Test
    @DisplayName("Should add component to kit")
    void shouldAddComponentToKit() {
        // Given
        Kit kit = Kit.physical("KIT-001", "Test Kit",
            List.of(KitComponent.required("SKU-001", 1)));

        // When
        kit.addComponent(KitComponent.required("SKU-002", 2));

        // Then
        assertEquals(2, kit.getComponentCount());
        assertTrue(kit.getComponent("SKU-002").isPresent());
    }

    @Test
    @DisplayName("Should throw exception when adding duplicate component")
    void shouldThrowExceptionWhenAddingDuplicateComponent() {
        // Given
        Kit kit = Kit.physical("KIT-001", "Test Kit",
            List.of(KitComponent.required("SKU-001", 1)));

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            kit.addComponent(KitComponent.required("SKU-001", 2))
        );
    }

    @Test
    @DisplayName("Should remove component from kit")
    void shouldRemoveComponentFromKit() {
        // Given
        Kit kit = Kit.physical("KIT-001", "Test Kit", List.of(
            KitComponent.required("SKU-001", 1),
            KitComponent.required("SKU-002", 2)
        ));

        // When
        kit.removeComponent("SKU-001");

        // Then
        assertEquals(1, kit.getComponentCount());
        assertFalse(kit.getComponent("SKU-001").isPresent());
    }

    @Test
    @DisplayName("Should throw exception when removing non-existent component")
    void shouldThrowExceptionWhenRemovingNonExistentComponent() {
        // Given
        Kit kit = Kit.physical("KIT-001", "Test Kit",
            List.of(KitComponent.required("SKU-001", 1)));

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            kit.removeComponent("SKU-NONEXISTENT")
        );
    }

    @Test
    @DisplayName("Should throw exception when removing last component")
    void shouldThrowExceptionWhenRemovingLastComponent() {
        // Given
        Kit kit = Kit.physical("KIT-001", "Test Kit",
            List.of(KitComponent.required("SKU-001", 1)));

        // When/Then
        assertThrows(IllegalStateException.class, () ->
            kit.removeComponent("SKU-001")
        );
    }

    @Test
    @DisplayName("Should update component quantity")
    void shouldUpdateComponentQuantity() {
        // Given
        Kit kit = Kit.physical("KIT-001", "Test Kit",
            List.of(KitComponent.required("SKU-001", 1)));

        // When
        kit.updateComponentQuantity("SKU-001", 3);

        // Then
        KitComponent component = kit.getComponent("SKU-001").orElseThrow();
        assertEquals(3, component.getQuantity());
    }

    @Test
    @DisplayName("Should calculate available kits based on component inventory")
    void shouldCalculateAvailableKitsBasedOnComponentInventory() {
        // Given: Kit with 3 components
        Kit kit = Kit.physical("KIT-LAPTOP-001", "Laptop Kit", List.of(
            KitComponent.required("SKU-SCREEN", 1),
            KitComponent.required("SKU-KEYBOARD", 1),
            KitComponent.required("SKU-BATTERY", 1)
        ));

        Map<String, Integer> inventory = Map.of(
            "SKU-SCREEN", 10,
            "SKU-KEYBOARD", 15,
            "SKU-BATTERY", 8  // Limiting component
        );

        // When
        int availableKits = kit.calculateAvailableKits(inventory);

        // Then: Limited by battery (8 units)
        assertEquals(8, availableKits);
    }

    @Test
    @DisplayName("Should calculate zero kits when component is missing")
    void shouldCalculateZeroKitsWhenComponentIsMissing() {
        // Given
        Kit kit = Kit.physical("KIT-001", "Test Kit", List.of(
            KitComponent.required("SKU-001", 1),
            KitComponent.required("SKU-002", 1)
        ));

        Map<String, Integer> inventory = Map.of(
            "SKU-001", 10
            // SKU-002 is missing
        );

        // When
        int availableKits = kit.calculateAvailableKits(inventory);

        // Then
        assertEquals(0, availableKits);
    }

    @Test
    @DisplayName("Should ignore optional components in availability calculation")
    void shouldIgnoreOptionalComponentsInAvailabilityCalculation() {
        // Given
        Kit kit = Kit.physical("KIT-001", "Test Kit", List.of(
            KitComponent.required("SKU-REQUIRED", 1),
            KitComponent.optional("SKU-OPTIONAL", 1)
        ));

        Map<String, Integer> inventory = Map.of(
            "SKU-REQUIRED", 10
            // Optional component not in inventory
        );

        // When
        int availableKits = kit.calculateAvailableKits(inventory);

        // Then: Should still be able to make 10 kits
        assertEquals(10, availableKits);
    }

    @Test
    @DisplayName("Should check if kit can be assembled")
    void shouldCheckIfKitCanBeAssembled() {
        // Given
        Kit kit = Kit.physical("KIT-001", "Test Kit", List.of(
            KitComponent.required("SKU-001", 2),
            KitComponent.required("SKU-002", 3)
        ));

        Map<String, Integer> inventory = Map.of(
            "SKU-001", 20,  // Enough for 10 kits (2 each)
            "SKU-002", 30   // Enough for 10 kits (3 each)
        );

        // When
        boolean canAssemble = kit.canAssemble(inventory, 10);

        // Then
        assertTrue(canAssemble);
    }

    @Test
    @DisplayName("Should detect insufficient components for assembly")
    void shouldDetectInsufficientComponentsForAssembly() {
        // Given
        Kit kit = Kit.physical("KIT-001", "Test Kit", List.of(
            KitComponent.required("SKU-001", 2),
            KitComponent.required("SKU-002", 3)
        ));

        Map<String, Integer> inventory = Map.of(
            "SKU-001", 20,  // Enough for 10 kits
            "SKU-002", 25   // Only enough for 8 kits (need 30 for 10 kits)
        );

        // When
        boolean canAssemble = kit.canAssemble(inventory, 10);

        // Then
        assertFalse(canAssemble);
    }

    @Test
    @DisplayName("Should calculate component shortages")
    void shouldCalculateComponentShortages() {
        // Given
        Kit kit = Kit.physical("KIT-001", "Test Kit", List.of(
            KitComponent.required("SKU-001", 2),
            KitComponent.required("SKU-002", 3)
        ));

        Map<String, Integer> inventory = Map.of(
            "SKU-001", 15,  // Need 20 for 10 kits (shortage: 5)
            "SKU-002", 20   // Need 30 for 10 kits (shortage: 10)
        );

        // When
        Map<String, Integer> shortages = kit.getShortages(inventory, 10);

        // Then
        assertEquals(2, shortages.size());
        assertEquals(5, shortages.get("SKU-001"));
        assertEquals(10, shortages.get("SKU-002"));
    }

    @Test
    @DisplayName("Should return empty shortages when all components available")
    void shouldReturnEmptyShortagesWhenAllComponentsAvailable() {
        // Given
        Kit kit = Kit.physical("KIT-001", "Test Kit", List.of(
            KitComponent.required("SKU-001", 2)
        ));

        Map<String, Integer> inventory = Map.of(
            "SKU-001", 20  // Enough for 10 kits
        );

        // When
        Map<String, Integer> shortages = kit.getShortages(inventory, 10);

        // Then
        assertTrue(shortages.isEmpty());
    }

    @Test
    @DisplayName("Should deactivate kit")
    void shouldDeactivateKit() {
        // Given
        Kit kit = Kit.physical("KIT-001", "Test Kit",
            List.of(KitComponent.required("SKU-001", 1)));

        // When
        kit.deactivate();

        // Then
        assertFalse(kit.isActive());
    }

    @Test
    @DisplayName("Should activate deactivated kit")
    void shouldActivateDeactivatedKit() {
        // Given
        Kit kit = Kit.physical("KIT-001", "Test Kit",
            List.of(KitComponent.required("SKU-001", 1)));
        kit.deactivate();

        // When
        kit.activate();

        // Then
        assertTrue(kit.isActive());
    }

    @Test
    @DisplayName("Should handle kit with multiple quantities per component")
    void shouldHandleKitWithMultipleQuantitiesPerComponent() {
        // Given: Kit requiring multiple units of each component
        Kit kit = Kit.physical("KIT-CHAIR-001", "Chair Kit", List.of(
            KitComponent.required("SKU-LEG", 4),      // 4 legs per chair
            KitComponent.required("SKU-SCREW", 16),   // 16 screws per chair
            KitComponent.required("SKU-SEAT", 1)
        ));

        Map<String, Integer> inventory = Map.of(
            "SKU-LEG", 40,     // Enough for 10 chairs
            "SKU-SCREW", 160,  // Enough for 10 chairs
            "SKU-SEAT", 12     // Enough for 12 chairs
        );

        // When
        int availableKits = kit.calculateAvailableKits(inventory);

        // Then: Limited by legs and screws (both allow 10 chairs)
        assertEquals(10, availableKits);
    }

    @Test
    @DisplayName("Should support substitutable components")
    void shouldSupportSubstitutableComponents() {
        // Given: Kit with substitutable component
        Kit kit = Kit.physical("KIT-001", "Test Kit", List.of(
            KitComponent.required("SKU-PRIMARY", 1),
            KitComponent.substitutable("SKU-SECONDARY", 1, "GROUP-A")
        ));

        // Then
        assertEquals(2, kit.getComponentCount());
        KitComponent component = kit.getComponent("SKU-SECONDARY").orElseThrow();
        assertTrue(component.isSubstitutable());
        assertEquals("GROUP-A", component.getSubstituteGroup());
    }

    @Test
    @DisplayName("Should track kit modification timestamps")
    void shouldTrackKitModificationTimestamps() {
        // Given
        Kit kit = Kit.physical("KIT-001", "Test Kit",
            List.of(KitComponent.required("SKU-001", 1)));

        // When
        kit.addComponent(KitComponent.required("SKU-002", 2));

        // Then
        assertNotNull(kit.getCreatedAt());
        assertNotNull(kit.getLastModifiedAt());
        assertTrue(kit.getLastModifiedAt().isAfter(kit.getCreatedAt()) ||
                   kit.getLastModifiedAt().isEqual(kit.getCreatedAt()));
    }
}
