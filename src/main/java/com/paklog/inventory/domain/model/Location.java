package com.paklog.inventory.domain.model;

import java.util.Objects;

public class Location {
    private final String aisle;
    private final String shelf;
    private final String bin;

    public Location() {
        this.aisle = null;
        this.shelf = null;
        this.bin = null;
    }

    public Location(String aisle, String shelf, String bin) {
        this.aisle = aisle;
        this.shelf = shelf;
        this.bin = bin;
    }

    public String getAisle() {
        return aisle;
    }

    public String getShelf() {
        return shelf;
    }

    public String getBin() {
        return bin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return Objects.equals(aisle, location.aisle) &&
                Objects.equals(shelf, location.shelf) &&
                Objects.equals(bin, location.bin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aisle, shelf, bin);
    }

    @Override
    public String toString() {
        return "Location{"
                + "aisle='" + aisle + "'"
                + ", shelf='" + shelf + "'"
                + ", bin='" + bin + "'"
                + '}';
    }
}