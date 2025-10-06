package com.paklog.inventory.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UOMConversion - validates unit conversion calculations.
 */
class UOMConversionTest {

    @Test
    @DisplayName("Should create one-way conversion")
    void shouldCreateOneWayConversion() {
        // Given
        UnitOfMeasure pallet = UnitOfMeasure.discrete("PALLET", "Pallet");
        UnitOfMeasure cases = UnitOfMeasure.discrete("CASE", "Case");

        // When: 1 PALLET = 48 CASE (one-way only)
        UOMConversion conversion = UOMConversion.oneWay(
            pallet, cases, new BigDecimal("48")
        );

        // Then
        assertEquals(pallet, conversion.getFromUOM());
        assertEquals(cases, conversion.getToUOM());
        assertEquals(new BigDecimal("48"), conversion.getConversionFactor());
        assertFalse(conversion.isReversible());
    }

    @Test
    @DisplayName("Should create two-way conversion")
    void shouldCreateTwoWayConversion() {
        // Given
        UnitOfMeasure cases = UnitOfMeasure.discrete("CASE", "Case");
        UnitOfMeasure each = UnitOfMeasure.discrete("EA", "Each");

        // When: 1 CASE = 12 EA (two-way)
        UOMConversion conversion = UOMConversion.twoWay(
            cases, each, new BigDecimal("12")
        );

        // Then
        assertEquals(cases, conversion.getFromUOM());
        assertEquals(each, conversion.getToUOM());
        assertEquals(new BigDecimal("12"), conversion.getConversionFactor());
        assertTrue(conversion.isReversible());
    }

    @Test
    @DisplayName("Should throw exception when creating conversion with null UOMs")
    void shouldThrowExceptionWhenCreatingConversionWithNullUoms() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.discrete("EA", "Each");

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            UOMConversion.oneWay(null, uom, new BigDecimal("10"))
        );

        assertThrows(IllegalArgumentException.class, () ->
            UOMConversion.oneWay(uom, null, new BigDecimal("10"))
        );
    }

    @Test
    @DisplayName("Should throw exception when creating conversion from UOM to itself")
    void shouldThrowExceptionWhenCreatingConversionFromUomToItself() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.discrete("EA", "Each");

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            UOMConversion.oneWay(uom, uom, new BigDecimal("10"))
        );
    }

    @Test
    @DisplayName("Should throw exception when creating conversion with zero or negative factor")
    void shouldThrowExceptionWhenCreatingConversionWithInvalidFactor() {
        // Given
        UnitOfMeasure from = UnitOfMeasure.discrete("CASE", "Case");
        UnitOfMeasure to = UnitOfMeasure.discrete("EA", "Each");

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            UOMConversion.oneWay(from, to, BigDecimal.ZERO)
        );

        assertThrows(IllegalArgumentException.class, () ->
            UOMConversion.oneWay(from, to, new BigDecimal("-10"))
        );
    }

    @Test
    @DisplayName("Should convert discrete quantities correctly")
    void shouldConvertDiscreteQuantitiesCorrectly() {
        // Given: 1 CASE = 12 EA
        UnitOfMeasure cases = UnitOfMeasure.discrete("CASE", "Case");
        UnitOfMeasure each = UnitOfMeasure.discrete("EA", "Each");
        UOMConversion conversion = UOMConversion.twoWay(cases, each, new BigDecimal("12"));

        // When: Convert 5 CASE to EA
        BigDecimal result = conversion.convert(new BigDecimal("5"));

        // Then: 5 CASE = 60 EA
        assertEquals(new BigDecimal("60"), result);
    }

    @Test
    @DisplayName("Should convert weight quantities with decimal precision")
    void shouldConvertWeightQuantitiesWithDecimalPrecision() {
        // Given: 1 KG = 2.20462 LB
        UnitOfMeasure kg = UnitOfMeasure.weight("KG", "Kilogram", 3);
        UnitOfMeasure lb = UnitOfMeasure.weight("LB", "Pound", 3);
        UOMConversion conversion = UOMConversion.twoWay(kg, lb, new BigDecimal("2.20462"));

        // When: Convert 10 KG to LB
        BigDecimal result = conversion.convert(new BigDecimal("10"));

        // Then: 10 KG = 22.046 LB (rounded to 3 decimal places)
        assertEquals(new BigDecimal("22.046"), result);
    }

    @Test
    @DisplayName("Should convert multi-level hierarchy correctly")
    void shouldConvertMultiLevelHierarchyCorrectly() {
        // Given: 1 PALLET = 48 CASE
        UnitOfMeasure pallet = UnitOfMeasure.discrete("PALLET", "Pallet");
        UnitOfMeasure cases = UnitOfMeasure.discrete("CASE", "Case");
        UOMConversion conversion = UOMConversion.oneWay(pallet, cases, new BigDecimal("48"));

        // When: Convert 3 PALLET to CASE
        BigDecimal result = conversion.convert(new BigDecimal("3"));

        // Then: 3 PALLET = 144 CASE
        assertEquals(new BigDecimal("144"), result);
    }

    @Test
    @DisplayName("Should reverse convert when reversible")
    void shouldReverseConvertWhenReversible() {
        // Given: 1 CASE = 12 EA (two-way)
        UnitOfMeasure cases = UnitOfMeasure.discrete("CASE", "Case");
        UnitOfMeasure each = UnitOfMeasure.discrete("EA", "Each");
        UOMConversion conversion = UOMConversion.twoWay(cases, each, new BigDecimal("12"));

        // When: Convert 60 EA back to CASE
        BigDecimal result = conversion.convertReverse(new BigDecimal("60"));

        // Then: 60 EA = 5 CASE
        assertEquals(new BigDecimal("5"), result);
    }

    @Test
    @DisplayName("Should reverse convert with fractional result")
    void shouldReverseConvertWithFractionalResult() {
        // Given: 1 CASE = 12 EA (two-way)
        UnitOfMeasure cases = UnitOfMeasure.discrete("CASE", "Case");
        UnitOfMeasure each = UnitOfMeasure.discrete("EA", "Each");
        UOMConversion conversion = UOMConversion.twoWay(cases, each, new BigDecimal("12"));

        // When: Convert 50 EA back to CASE
        BigDecimal result = conversion.convertReverse(new BigDecimal("50"));

        // Then: 50 EA = 4.17 CASE (rounded)
        assertEquals(new BigDecimal("4"), result);
    }

    @Test
    @DisplayName("Should throw exception when reverse converting non-reversible conversion")
    void shouldThrowExceptionWhenReverseConvertingNonReversible() {
        // Given: One-way conversion
        UnitOfMeasure pallet = UnitOfMeasure.discrete("PALLET", "Pallet");
        UnitOfMeasure cases = UnitOfMeasure.discrete("CASE", "Case");
        UOMConversion conversion = UOMConversion.oneWay(pallet, cases, new BigDecimal("48"));

        // When/Then
        assertThrows(IllegalStateException.class, () ->
            conversion.convertReverse(new BigDecimal("48"))
        );
    }

    @Test
    @DisplayName("Should throw exception when converting negative quantity")
    void shouldThrowExceptionWhenConvertingNegativeQuantity() {
        // Given
        UnitOfMeasure cases = UnitOfMeasure.discrete("CASE", "Case");
        UnitOfMeasure each = UnitOfMeasure.discrete("EA", "Each");
        UOMConversion conversion = UOMConversion.twoWay(cases, each, new BigDecimal("12"));

        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            conversion.convert(new BigDecimal("-5"))
        );
    }

    @Test
    @DisplayName("Should convert zero quantity to zero")
    void shouldConvertZeroQuantityToZero() {
        // Given
        UnitOfMeasure cases = UnitOfMeasure.discrete("CASE", "Case");
        UnitOfMeasure each = UnitOfMeasure.discrete("EA", "Each");
        UOMConversion conversion = UOMConversion.twoWay(cases, each, new BigDecimal("12"));

        // When
        BigDecimal result = conversion.convert(BigDecimal.ZERO);

        // Then
        assertEquals(BigDecimal.ZERO.setScale(0), result);
    }

    @Test
    @DisplayName("Should check if conversion can convert between UOMs (forward)")
    void shouldCheckIfConversionCanConvertForward() {
        // Given
        UnitOfMeasure cases = UnitOfMeasure.discrete("CASE", "Case");
        UnitOfMeasure each = UnitOfMeasure.discrete("EA", "Each");
        UOMConversion conversion = UOMConversion.twoWay(cases, each, new BigDecimal("12"));

        // When
        boolean canConvert = conversion.canConvert(cases, each);

        // Then
        assertTrue(canConvert);
    }

    @Test
    @DisplayName("Should check if conversion can convert between UOMs (reverse)")
    void shouldCheckIfConversionCanConvertReverse() {
        // Given
        UnitOfMeasure cases = UnitOfMeasure.discrete("CASE", "Case");
        UnitOfMeasure each = UnitOfMeasure.discrete("EA", "Each");
        UOMConversion conversion = UOMConversion.twoWay(cases, each, new BigDecimal("12"));

        // When
        boolean canConvert = conversion.canConvert(each, cases);

        // Then (reversible conversion)
        assertTrue(canConvert);
    }

    @Test
    @DisplayName("Should not allow reverse conversion check for one-way conversion")
    void shouldNotAllowReverseConversionCheckForOneWay() {
        // Given
        UnitOfMeasure pallet = UnitOfMeasure.discrete("PALLET", "Pallet");
        UnitOfMeasure cases = UnitOfMeasure.discrete("CASE", "Case");
        UOMConversion conversion = UOMConversion.oneWay(pallet, cases, new BigDecimal("48"));

        // When
        boolean canConvertForward = conversion.canConvert(pallet, cases);
        boolean canConvertReverse = conversion.canConvert(cases, pallet);

        // Then
        assertTrue(canConvertForward);
        assertFalse(canConvertReverse);
    }

    @Test
    @DisplayName("Should get reverse conversion from two-way conversion")
    void shouldGetReverseConversionFromTwoWay() {
        // Given: 1 CASE = 12 EA
        UnitOfMeasure cases = UnitOfMeasure.discrete("CASE", "Case");
        UnitOfMeasure each = UnitOfMeasure.discrete("EA", "Each");
        UOMConversion conversion = UOMConversion.twoWay(cases, each, new BigDecimal("12"));

        // When: Get reverse conversion (EA → CASE)
        UOMConversion reverse = conversion.reverse();

        // Then: 1 EA = 0.0833... CASE
        assertEquals(each, reverse.getFromUOM());
        assertEquals(cases, reverse.getToUOM());
        assertTrue(reverse.isReversible());

        // Verify conversion factor is reciprocal
        BigDecimal expectedFactor = BigDecimal.ONE.divide(
            new BigDecimal("12"), 10, RoundingMode.HALF_UP
        );
        assertEquals(expectedFactor, reverse.getConversionFactor());
    }

    @Test
    @DisplayName("Should throw exception when getting reverse of one-way conversion")
    void shouldThrowExceptionWhenGettingReverseOfOneWay() {
        // Given
        UnitOfMeasure pallet = UnitOfMeasure.discrete("PALLET", "Pallet");
        UnitOfMeasure cases = UnitOfMeasure.discrete("CASE", "Case");
        UOMConversion conversion = UOMConversion.oneWay(pallet, cases, new BigDecimal("48"));

        // When/Then
        assertThrows(IllegalStateException.class, conversion::reverse);
    }

    @Test
    @DisplayName("Should handle complex conversion with high precision")
    void shouldHandleComplexConversionWithHighPrecision() {
        // Given: 1 GAL = 3.78541 L
        UnitOfMeasure gal = UnitOfMeasure.volume("GAL", "Gallon", 5);
        UnitOfMeasure liter = UnitOfMeasure.volume("L", "Liter", 5);
        UOMConversion conversion = UOMConversion.twoWay(
            gal, liter, new BigDecimal("3.78541")
        );

        // When: Convert 100 GAL to L
        BigDecimal result = conversion.convert(new BigDecimal("100"));

        // Then: 100 GAL = 378.541 L
        assertEquals(new BigDecimal("378.54100"), result);
    }

    @Test
    @DisplayName("Should round to target UOM precision")
    void shouldRoundToTargetUomPrecision() {
        // Given: Convert from high precision to low precision
        UnitOfMeasure highPrecision = UnitOfMeasure.weight("KG", "Kilogram", 3);
        UnitOfMeasure lowPrecision = UnitOfMeasure.weight("LB", "Pound", 2);
        UOMConversion conversion = UOMConversion.twoWay(
            highPrecision, lowPrecision, new BigDecimal("2.20462")
        );

        // When: Convert 1.234 KG to LB
        BigDecimal result = conversion.convert(new BigDecimal("1.234"));

        // Then: Result should be rounded to 2 decimal places (LB precision)
        assertEquals(2, result.scale());
        assertEquals(new BigDecimal("2.72"), result);
    }

    @Test
    @DisplayName("Should validate toString representation")
    void shouldValidateToStringRepresentation() {
        // Given
        UnitOfMeasure cases = UnitOfMeasure.discrete("CASE", "Case");
        UnitOfMeasure each = UnitOfMeasure.discrete("EA", "Each");
        UOMConversion conversion = UOMConversion.twoWay(cases, each, new BigDecimal("12"));

        // When
        String representation = conversion.toString();

        // Then
        assertEquals("1 CASE = 12 EA", representation);
    }

    @Test
    @DisplayName("Should handle equality correctly")
    void shouldHandleEqualityCorrectly() {
        // Given
        UnitOfMeasure cases = UnitOfMeasure.discrete("CASE", "Case");
        UnitOfMeasure each = UnitOfMeasure.discrete("EA", "Each");

        UOMConversion conversion1 = UOMConversion.twoWay(cases, each, new BigDecimal("12"));
        UOMConversion conversion2 = UOMConversion.twoWay(cases, each, new BigDecimal("12"));
        UOMConversion conversion3 = UOMConversion.twoWay(each, cases, new BigDecimal("0.083"));

        // When/Then
        assertEquals(conversion1, conversion2);
        assertNotEquals(conversion1, conversion3);
    }

    @Test
    @DisplayName("Should handle chain conversions scenario")
    void shouldHandleChainConversionsScenario() {
        // Given: Multi-level hierarchy
        // 1 PALLET = 4 LAYER
        // 1 LAYER = 12 CASE
        // 1 CASE = 12 EA
        // Therefore: 1 PALLET = 576 EA

        UnitOfMeasure pallet = UnitOfMeasure.discrete("PALLET", "Pallet");
        UnitOfMeasure layer = UnitOfMeasure.discrete("LAYER", "Layer");
        UnitOfMeasure cases = UnitOfMeasure.discrete("CASE", "Case");
        UnitOfMeasure each = UnitOfMeasure.discrete("EA", "Each");

        UOMConversion palletToLayer = UOMConversion.twoWay(
            pallet, layer, new BigDecimal("4")
        );
        UOMConversion layerToCase = UOMConversion.twoWay(
            layer, cases, new BigDecimal("12")
        );
        UOMConversion caseToEach = UOMConversion.twoWay(
            cases, each, new BigDecimal("12")
        );

        // When: Convert 1 PALLET → LAYER → CASE → EA
        BigDecimal layers = palletToLayer.convert(BigDecimal.ONE);      // 4 LAYER
        BigDecimal casesQty = layerToCase.convert(layers);              // 48 CASE
        BigDecimal eaches = caseToEach.convert(casesQty);               // 576 EA

        // Then
        assertEquals(new BigDecimal("4"), layers);
        assertEquals(new BigDecimal("48"), casesQty);
        assertEquals(new BigDecimal("576"), eaches);
    }
}
