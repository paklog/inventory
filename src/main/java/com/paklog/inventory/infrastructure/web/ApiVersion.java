package com.paklog.inventory.infrastructure.web;

/**
 * API version constants for URL-based versioning.
 * Follows semantic versioning principles.
 */
public final class ApiVersion {

    private ApiVersion() {
        // Utility class
    }

    /**
     * Current stable API version
     */
    public static final String V1 = "v1";

    /**
     * Beta/preview API version (for testing new features)
     */
    public static final String V2_BETA = "v2-beta";

    /**
     * Base path for versioned APIs
     */
    public static final String BASE_PATH = "/api";

    /**
     * V1 base path
     */
    public static final String V1_BASE_PATH = BASE_PATH + "/" + V1;

    /**
     * V2 Beta base path
     */
    public static final String V2_BETA_BASE_PATH = BASE_PATH + "/" + V2_BETA;
}
