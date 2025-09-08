package com.paklog.inventory.domain.model;

import java.util.Objects;

public class PhysicalReservation {
    private final String reservationId;
    private final int quantity;

    public PhysicalReservation(String reservationId, int quantity) {
        this.reservationId = reservationId;
        this.quantity = quantity;
    }

    public String getReservationId() {
        return reservationId;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhysicalReservation that = (PhysicalReservation) o;
        return Objects.equals(reservationId, that.reservationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationId);
    }

    @Override
    public String toString() {
        return "PhysicalReservation{"
                + "reservationId='" + reservationId + "'"
                + ", quantity=" + quantity
                + '}'
                + ";";
    }
}
