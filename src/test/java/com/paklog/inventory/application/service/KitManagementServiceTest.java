package com.paklog.inventory.application.service;

import com.paklog.inventory.domain.model.Kit;
import com.paklog.inventory.domain.model.KitComponent;
import com.paklog.inventory.domain.model.KitType;
import com.paklog.inventory.domain.repository.KitRepository;
import com.paklog.inventory.domain.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for KitManagementService - validates kit/BOM operations.
 * Tests kit creation, component management, and availability calculations.
 */
@ExtendWith(MockitoExtension.class)
class KitManagementServiceTest {

    @Mock
    private KitRepository kitRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private KitManagementService kitManagementService;

    private String kitSku;
    private List<KitComponent> components;

    @BeforeEach
    void setUp() {
        kitSku = "KIT-LAPTOP-001";
        components = List.of(
            KitComponent.required("SKU-SCREEN", 1),
            KitComponent.required("SKU-KEYBOARD", 1),
            KitComponent.required("SKU-BATTERY", 1),
            KitComponent.optional("SKU-MOUSE", 1)
        );
    }

    @Test
    @DisplayName("Should create physical kit successfully")
    void shouldCreatePhysicalKitSuccessfully() {
        // Given
        when(kitRepository.existsByKitSku(kitSku)).thenReturn(false);

        // When
        kitManagementService.createPhysicalKit(kitSku, "Laptop Kit", components);

        // Then
        verify(kitRepository).save(argThat(kit ->
            kit.getKitSku().equals(kitSku) &&
            kit.getKitType() == KitType.PHYSICAL
        ));
    }

    @Test
    @DisplayName("Should create virtual kit successfully")
    void shouldCreateVirtualKitSuccessfully() {
        // Given
        when(kitRepository.existsByKitSku(kitSku)).thenReturn(false);

        // When
        kitManagementService.createVirtualKit(kitSku, "Virtual Bundle", components, true);

        // Then
        verify(kitRepository).save(argThat(kit ->
            kit.getKitSku().equals(kitSku) &&
            kit.getKitType() == KitType.VIRTUAL
        ));
    }

    @Test
    @DisplayName("Should throw exception when creating duplicate kit")
    void shouldThrowExceptionWhenCreatingDuplicateKit() {
        // Given
        when(kitRepository.existsByKitSku(kitSku)).thenReturn(true);

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            kitManagementService.createKit(kitSku, "Duplicate Kit", components,
                KitType.PHYSICAL, false)
        );

        verify(kitRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should add component to kit")
    void shouldAddComponentToKit() {
        // Given
        Kit kit = Kit.physical(kitSku, "Test Kit", components);
        when(kitRepository.findByKitSku(kitSku)).thenReturn(Optional.of(kit));

        KitComponent newComponent = KitComponent.required("SKU-CHARGER", 1);

        // When
        kitManagementService.addComponent(kitSku, newComponent);

        // Then
        verify(kitRepository).save(kit);
    }

    @Test
    @DisplayName("Should remove component from kit")
    void shouldRemoveComponentFromKit() {
        // Given
        Kit kit = Kit.physical(kitSku, "Test Kit", components);
        when(kitRepository.findByKitSku(kitSku)).thenReturn(Optional.of(kit));

        // When
        kitManagementService.removeComponent(kitSku, "SKU-MOUSE");

        // Then
        verify(kitRepository).save(kit);
    }

    @Test
    @DisplayName("Should update component quantity")
    void shouldUpdateComponentQuantity() {
        // Given
        Kit kit = Kit.physical(kitSku, "Test Kit", components);
        when(kitRepository.findByKitSku(kitSku)).thenReturn(Optional.of(kit));

        // When
        kitManagementService.updateComponentQuantity(kitSku, "SKU-BATTERY", 2);

        // Then
        verify(kitRepository).save(kit);
    }

    @Test
    @DisplayName("Should calculate available kits based on component inventory")
    void shouldCalculateAvailableKitsBasedOnComponentInventory() {
        // Given
        Kit kit = Kit.physical(kitSku, "Test Kit", components);
        when(kitRepository.findByKitSku(kitSku)).thenReturn(Optional.of(kit));

        Map<String, Integer> componentInventory = Map.of(
            "SKU-SCREEN", 10,
            "SKU-KEYBOARD", 15,
            "SKU-BATTERY", 8,
            "SKU-MOUSE", 20
        );

        // When
        int availableKits = kitManagementService.calculateAvailableKits(kitSku, componentInventory);

        // Then: Limited by battery (8 units)
        assertEquals(8, availableKits);
    }

    @Test
    @DisplayName("Should calculate zero kits when component is out of stock")
    void shouldCalculateZeroKitsWhenComponentIsOutOfStock() {
        // Given
        Kit kit = Kit.physical(kitSku, "Test Kit", components);
        when(kitRepository.findByKitSku(kitSku)).thenReturn(Optional.of(kit));

        Map<String, Integer> componentInventory = Map.of(
            "SKU-SCREEN", 10,
            "SKU-KEYBOARD", 0, // Out of stock
            "SKU-BATTERY", 8
        );

        // When
        int availableKits = kitManagementService.calculateAvailableKits(kitSku, componentInventory);

        // Then
        assertEquals(0, availableKits);
    }

    @Test
    @DisplayName("Should check if kit can be assembled")
    void shouldCheckIfKitCanBeAssembled() {
        // Given
        Kit kit = Kit.physical(kitSku, "Test Kit", components);
        when(kitRepository.findByKitSku(kitSku)).thenReturn(Optional.of(kit));

        Map<String, Integer> componentInventory = Map.of(
            "SKU-SCREEN", 10,
            "SKU-KEYBOARD", 10,
            "SKU-BATTERY", 10
        );

        // When
        boolean canAssemble = kitManagementService.canAssemble(kitSku, componentInventory, 5);

        // Then
        assertTrue(canAssemble);
    }

    @Test
    @DisplayName("Should detect component shortages")
    void shouldDetectComponentShortages() {
        // Given
        Kit kit = Kit.physical(kitSku, "Test Kit", components);
        when(kitRepository.findByKitSku(kitSku)).thenReturn(Optional.of(kit));

        Map<String, Integer> componentInventory = Map.of(
            "SKU-SCREEN", 5,
            "SKU-KEYBOARD", 3,
            "SKU-BATTERY", 10
        );

        // When
        Map<String, Integer> shortages = kitManagementService.getShortages(kitSku, componentInventory, 10);

        // Then
        assertTrue(shortages.containsKey("SKU-SCREEN")); // Need 10, have 5
        assertTrue(shortages.containsKey("SKU-KEYBOARD")); // Need 10, have 3
        assertEquals(5, shortages.get("SKU-SCREEN"));
        assertEquals(7, shortages.get("SKU-KEYBOARD"));
    }

    @Test
    @DisplayName("Should deactivate kit")
    void shouldDeactivateKit() {
        // Given
        Kit kit = Kit.physical(kitSku, "Test Kit", components);
        when(kitRepository.findByKitSku(kitSku)).thenReturn(Optional.of(kit));

        // When
        kitManagementService.deactivateKit(kitSku);

        // Then
        assertFalse(kit.isActive());
        verify(kitRepository).save(kit);
    }

    @Test
    @DisplayName("Should activate kit")
    void shouldActivateKit() {
        // Given
        Kit kit = Kit.physical(kitSku, "Test Kit", components);
        kit.deactivate();
        when(kitRepository.findByKitSku(kitSku)).thenReturn(Optional.of(kit));

        // When
        kitManagementService.activateKit(kitSku);

        // Then
        assertTrue(kit.isActive());
        verify(kitRepository).save(kit);
    }

    @Test
    @DisplayName("Should get kit details")
    void shouldGetKitDetails() {
        // Given
        Kit kit = Kit.physical(kitSku, "Test Kit", components);
        when(kitRepository.findByKitSku(kitSku)).thenReturn(Optional.of(kit));

        // When
        Kit result = kitManagementService.getKit(kitSku);

        // Then
        assertNotNull(result);
        assertEquals(kitSku, result.getKitSku());
    }

    @Test
    @DisplayName("Should get all active kits")
    void shouldGetAllActiveKits() {
        // Given
        Kit kit1 = Kit.physical("KIT-001", "Kit 1", components);
        Kit kit2 = Kit.physical("KIT-002", "Kit 2", components);

        when(kitRepository.findActiveKits()).thenReturn(List.of(kit1, kit2));

        // When
        List<Kit> activeKits = kitManagementService.getActiveKits();

        // Then
        assertEquals(2, activeKits.size());
    }

    @Test
    @DisplayName("Should get kits by type")
    void shouldGetKitsByType() {
        // Given
        Kit kit1 = Kit.physical("KIT-001", "Physical 1", components);
        Kit kit2 = Kit.physical("KIT-002", "Physical 2", components);

        when(kitRepository.findByType(KitType.PHYSICAL)).thenReturn(List.of(kit1, kit2));

        // When
        List<Kit> physicalKits = kitManagementService.getKitsByType(KitType.PHYSICAL);

        // Then
        assertEquals(2, physicalKits.size());
        assertTrue(physicalKits.stream().allMatch(k -> k.getKitType() == KitType.PHYSICAL));
    }

    @Test
    @DisplayName("Should find kits containing specific component")
    void shouldFindKitsContainingSpecificComponent() {
        // Given
        Kit kit1 = Kit.physical("KIT-001", "Kit 1", components);
        Kit kit2 = Kit.physical("KIT-002", "Kit 2", components);

        when(kitRepository.findKitsContainingComponent("SKU-BATTERY"))
            .thenReturn(List.of(kit1, kit2));

        // When
        List<Kit> kitsWithBattery = kitManagementService.getKitsContainingComponent("SKU-BATTERY");

        // Then
        assertEquals(2, kitsWithBattery.size());
    }
}
