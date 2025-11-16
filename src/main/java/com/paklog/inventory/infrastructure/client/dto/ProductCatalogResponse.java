package com.paklog.inventory.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing a Product from the Product Catalog API.
 * Maps to the Product schema defined in product_catalog.yaml
 */
public class ProductCatalogResponse {

    @JsonProperty("sku")
    private String sku;

    @JsonProperty("title")
    private String title;

    @JsonProperty("dimensions")
    private Dimensions dimensions;

    @JsonProperty("attributes")
    private Attributes attributes;

    public ProductCatalogResponse() {
    }

    public ProductCatalogResponse(String sku, String title) {
        this.sku = sku;
        this.title = title;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Dimensions getDimensions() {
        return dimensions;
    }

    public void setDimensions(Dimensions dimensions) {
        this.dimensions = dimensions;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

    public static class Dimensions {
        @JsonProperty("item")
        private DimensionSet item;

        @JsonProperty("package")
        private DimensionSet packageDimensions;

        public DimensionSet getItem() {
            return item;
        }

        public void setItem(DimensionSet item) {
            this.item = item;
        }

        public DimensionSet getPackageDimensions() {
            return packageDimensions;
        }

        public void setPackageDimensions(DimensionSet packageDimensions) {
            this.packageDimensions = packageDimensions;
        }
    }

    public static class DimensionSet {
        @JsonProperty("length")
        private DimensionMeasurement length;

        @JsonProperty("width")
        private DimensionMeasurement width;

        @JsonProperty("height")
        private DimensionMeasurement height;

        @JsonProperty("weight")
        private WeightMeasurement weight;

        public DimensionMeasurement getLength() {
            return length;
        }

        public void setLength(DimensionMeasurement length) {
            this.length = length;
        }

        public DimensionMeasurement getWidth() {
            return width;
        }

        public void setWidth(DimensionMeasurement width) {
            this.width = width;
        }

        public DimensionMeasurement getHeight() {
            return height;
        }

        public void setHeight(DimensionMeasurement height) {
            this.height = height;
        }

        public WeightMeasurement getWeight() {
            return weight;
        }

        public void setWeight(WeightMeasurement weight) {
            this.weight = weight;
        }
    }

    public static class DimensionMeasurement {
        @JsonProperty("value")
        private Double value;

        @JsonProperty("unit")
        private String unit;

        public Double getValue() {
            return value;
        }

        public void setValue(Double value) {
            this.value = value;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }
    }

    public static class WeightMeasurement {
        @JsonProperty("value")
        private Double value;

        @JsonProperty("unit")
        private String unit;

        public Double getValue() {
            return value;
        }

        public void setValue(Double value) {
            this.value = value;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }
    }

    public static class Attributes {
        @JsonProperty("hazmat_info")
        private HazmatInfo hazmatInfo;

        public HazmatInfo getHazmatInfo() {
            return hazmatInfo;
        }

        public void setHazmatInfo(HazmatInfo hazmatInfo) {
            this.hazmatInfo = hazmatInfo;
        }
    }

    public static class HazmatInfo {
        @JsonProperty("is_hazmat")
        private Boolean isHazmat;

        @JsonProperty("un_number")
        private String unNumber;

        public Boolean getIsHazmat() {
            return isHazmat;
        }

        public void setIsHazmat(Boolean isHazmat) {
            this.isHazmat = isHazmat;
        }

        public String getUnNumber() {
            return unNumber;
        }

        public void setUnNumber(String unNumber) {
            this.unNumber = unNumber;
        }
    }
}
