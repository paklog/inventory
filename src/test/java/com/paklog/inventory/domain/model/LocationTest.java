package com.paklog.inventory.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocationTest {

    @Test
    void constructorAndGetters() {
        Location location = new Location("A1", "S2", "B3");
        assertEquals("A1", location.getAisle());
        assertEquals("S2", location.getShelf());
        assertEquals("B3", location.getBin());
    }

    @Test
    void equalsAndHashCode() {
        Location location1 = new Location("A1", "S2", "B3");
        Location location2 = new Location("A1", "S2", "B3");
        Location location3 = new Location("A1", "S2", "B4");

        assertEquals(location1, location2);
        assertNotEquals(location1, location3);
        assertEquals(location1.hashCode(), location2.hashCode());
        assertNotEquals(location1.hashCode(), location3.hashCode());
    }

    @Test
    void toStringMethod() {
        Location location = new Location("A1", "S2", "B3");
        assertEquals("Location{aisle='A1', shelf='S2', bin='B3'}", location.toString());
    }
}
