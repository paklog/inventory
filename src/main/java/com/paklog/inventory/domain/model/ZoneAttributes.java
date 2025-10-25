package com.paklog.inventory.domain.model;

import java.util.Objects;

/**
 * Attributes defining operational characteristics of a zone.
 */
public class ZoneAttributes {

    private final Boolean temperatureControlled;
    private final Double minTemperatureCelsius;
    private final Double maxTemperatureCelsius;
    private final Boolean securityRequired;
    private final Boolean hazmatCertified;
    private final Integer maxWeightKg;
    private final Integer priorityLevel; // 1 (highest) to 10 (lowest)

    private ZoneAttributes(Boolean temperatureControlled, Double minTemperatureCelsius,
                          Double maxTemperatureCelsius, Boolean securityRequired,
                          Boolean hazmatCertified, Integer maxWeightKg, Integer priorityLevel) {
this.temperatureControlled = temperatureControlled;
        this.minTemperatureCelsius = minTemperatureCelsius;
        this.maxTemperatureCelsius = maxTemperatureCelsius;
        this.securityRequired = securityRequired;
        this.hazmatCertified = hazmatCertified;
        this.maxWeightKg = maxWeightKg;
        this.priorityLevel = priorityLevel;
    }



    public static ZoneAttributes defaults() {
        return new Builder()
                .temperatureControlled(false)
                .securityRequired(false)
                .hazmatCertified(false)
                .priorityLevel(5)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Boolean getTemperatureControlled() {
        return temperatureControlled != null ? temperatureControlled : false;
    }

    public Double getMinTemperatureCelsius() {
        return minTemperatureCelsius;
    }

    public Double getMaxTemperatureCelsius() {
        return maxTemperatureCelsius;
    }

    public Boolean getSecurityRequired() {
        return securityRequired != null ? securityRequired : false;
    }

    public Boolean getHazmatCertified() {
        return hazmatCertified != null ? hazmatCertified : false;
    }

    public Integer getMaxWeightKg() {
        return maxWeightKg;
    }

    public Integer getPriorityLevel() {
        return priorityLevel != null ? priorityLevel : 5;
    }

    public boolean isWithinTemperatureRange(Double temperature) {
        if (!getTemperatureControlled() || temperature == null) {
            return true;
        }
        if (minTemperatureCelsius != null && temperature < minTemperatureCelsius) {
            return false;
        }
        if (maxTemperatureCelsius != null && temperature > maxTemperatureCelsius) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ZoneAttributes that = (ZoneAttributes) o;
        return Objects.equals(temperatureControlled, that.temperatureControlled) &&
               Objects.equals(minTemperatureCelsius, that.minTemperatureCelsius) &&
               Objects.equals(maxTemperatureCelsius, that.maxTemperatureCelsius) &&
               Objects.equals(securityRequired, that.securityRequired) &&
               Objects.equals(hazmatCertified, that.hazmatCertified) &&
               Objects.equals(maxWeightKg, that.maxWeightKg) &&
               Objects.equals(priorityLevel, that.priorityLevel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(temperatureControlled, minTemperatureCelsius, maxTemperatureCelsius,
                          securityRequired, hazmatCertified, maxWeightKg, priorityLevel);
    }

    @Override
    public String toString() {
        return "ZoneAttributes{" +
                "temperatureControlled=" + temperatureControlled +
                ", minTemperatureCelsius=" + minTemperatureCelsius +
                ", maxTemperatureCelsius=" + maxTemperatureCelsius +
                ", securityRequired=" + securityRequired +
                ", hazmatCertified=" + hazmatCertified +
                ", maxWeightKg=" + maxWeightKg +
                ", priorityLevel=" + priorityLevel +
                '}';
    }

    public static class Builder {
        private Boolean temperatureControlled;
        private Double minTemperatureCelsius;
        private Double maxTemperatureCelsius;
        private Boolean securityRequired;
        private Boolean hazmatCertified;
        private Integer maxWeightKg;
        private Integer priorityLevel;

        public Builder temperatureControlled(Boolean temperatureControlled) {
            this.temperatureControlled = temperatureControlled;
            return this;
        }

        public Builder minTemperatureCelsius(Double minTemperatureCelsius) {
            this.minTemperatureCelsius = minTemperatureCelsius;
            return this;
        }

        public Builder maxTemperatureCelsius(Double maxTemperatureCelsius) {
            this.maxTemperatureCelsius = maxTemperatureCelsius;
            return this;
        }

        public Builder securityRequired(Boolean securityRequired) {
            this.securityRequired = securityRequired;
            return this;
        }

        public Builder hazmatCertified(Boolean hazmatCertified) {
            this.hazmatCertified = hazmatCertified;
            return this;
        }

        public Builder maxWeightKg(Integer maxWeightKg) {
            this.maxWeightKg = maxWeightKg;
            return this;
        }

        public Builder priorityLevel(Integer priorityLevel) {
            this.priorityLevel = priorityLevel;
            return this;
        }

        public ZoneAttributes build() {
            return new ZoneAttributes(temperatureControlled, minTemperatureCelsius,
                                     maxTemperatureCelsius, securityRequired, hazmatCertified,
                                     maxWeightKg, priorityLevel);
        

}
}
}
