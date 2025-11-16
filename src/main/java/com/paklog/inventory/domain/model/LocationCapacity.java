package com.paklog.inventory.domain.model;

import java.util.Objects;

/**
 * Value object representing capacity constraints for a warehouse location.
 */
public class LocationCapacity {

    private final Integer maxPallets;
    private final Double maxWeightKg;
    private final Double maxCubicMeters;
    private final Integer maxUnits;

    private LocationCapacity(Integer maxPallets, Double maxWeightKg,
                            Double maxCubicMeters, Integer maxUnits) {
this.maxPallets = maxPallets;
        this.maxWeightKg = maxWeightKg;
        this.maxCubicMeters = maxCubicMeters;
        this.maxUnits = maxUnits;
    }



    public static LocationCapacity unlimited() {
        return new LocationCapacity(null, null, null, null);
    }

    public static LocationCapacity of(Integer maxPallets, Double maxWeightKg,
                                     Double maxCubicMeters, Integer maxUnits) {
        return new LocationCapacity(maxPallets, maxWeightKg, maxCubicMeters, maxUnits);
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean canAccommodate(int pallets, double weightKg, double cubicMeters, int units) {
        if (maxPallets != null && pallets > maxPallets) {
            return false;
        }
        if (maxWeightKg != null && weightKg > maxWeightKg) {
            return false;
        }
        if (maxCubicMeters != null && cubicMeters > maxCubicMeters) {
            return false;
        }
        if (maxUnits != null && units > maxUnits) {
            return false;
        }
        return true;
    }

    public boolean isUnlimited() {
        return maxPallets == null && maxWeightKg == null &&
               maxCubicMeters == null && maxUnits == null;
    }

    public Integer getMaxPallets() {
        return maxPallets;
    }

    public Double getMaxWeightKg() {
        return maxWeightKg;
    }

    public Double getMaxCubicMeters() {
        return maxCubicMeters;
    }

    public Integer getMaxUnits() {
        return maxUnits;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocationCapacity that = (LocationCapacity) o;
        return Objects.equals(maxPallets, that.maxPallets) &&
               Objects.equals(maxWeightKg, that.maxWeightKg) &&
               Objects.equals(maxCubicMeters, that.maxCubicMeters) &&
               Objects.equals(maxUnits, that.maxUnits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxPallets, maxWeightKg, maxCubicMeters, maxUnits);
    }

    @Override
    public String toString() {
        return "LocationCapacity{" +
                "maxPallets=" + maxPallets +
                ", maxWeightKg=" + maxWeightKg +
                ", maxCubicMeters=" + maxCubicMeters +
                ", maxUnits=" + maxUnits +
                '}';
    }

    public static class Builder {
        private Integer maxPallets;
        private Double maxWeightKg;
        private Double maxCubicMeters;
        private Integer maxUnits;

        public Builder maxPallets(Integer maxPallets) {
            this.maxPallets = maxPallets;
            return this;
        }

        public Builder maxWeightKg(Double maxWeightKg) {
            this.maxWeightKg = maxWeightKg;
            return this;
        }

        public Builder maxCubicMeters(Double maxCubicMeters) {
            this.maxCubicMeters = maxCubicMeters;
            return this;
        }

        public Builder maxUnits(Integer maxUnits) {
            this.maxUnits = maxUnits;
            return this;
        }

        public LocationCapacity build() {
            return new LocationCapacity(maxPallets, maxWeightKg, maxCubicMeters, maxUnits);
        

}
}
}
