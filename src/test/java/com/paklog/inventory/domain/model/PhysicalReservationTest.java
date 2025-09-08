package com.paklog.inventory.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhysicalReservationTest {

    @Test
    void constructorAndGetters() {
        PhysicalReservation reservation = new PhysicalReservation("order123", 5);
        assertEquals("order123", reservation.getReservationId());
        assertEquals(5, reservation.getQuantity());
    }

    @Test
    void equalsAndHashCode() {
        PhysicalReservation reservation1 = new PhysicalReservation("order123", 5);
        PhysicalReservation reservation2 = new PhysicalReservation("order123", 10); // Same ID, different quantity
        PhysicalReservation reservation3 = new PhysicalReservation("order456", 5);

        assertEquals(reservation1, reservation2); // Should be equal based on reservationId
        assertNotEquals(reservation1, reservation3);
        assertEquals(reservation1.hashCode(), reservation2.hashCode());
        assertNotEquals(reservation1.hashCode(), reservation3.hashCode());
    }

    @Test
    void toStringMethod() {
        PhysicalReservation reservation = new PhysicalReservation("order123", 5);
        assertEquals("PhysicalReservation{reservationId='order123', quantity=5};", reservation.toString());
    }
}
