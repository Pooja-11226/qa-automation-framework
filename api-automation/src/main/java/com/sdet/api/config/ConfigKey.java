package com.sdet.api.config;

/**
 * Every configurable setting, with the system-property name and environment-variable name it can be set by.
 */
public enum ConfigKey {
    BASE_URI("api.base.uri", "API_BASE_URI"),
    USERNAME("api.username", "API_USERNAME"),
    PASSWORD("api.password", "API_PASSWORD"),
    CONNECT_TIMEOUT_MS("api.connect.timeout.ms", "API_CONNECT_TIMEOUT_MS"),
    READ_TIMEOUT_MS("api.read.timeout.ms", "API_READ_TIMEOUT_MS"),
    HEALTHCHECK_ENABLED("api.healthcheck.enabled", "API_HEALTHCHECK_ENABLED");

    private final String propertyName;
    private final String environmentVariable;

    ConfigKey(String propertyName, String environmentVariable) {
        this.propertyName = propertyName;
        this.environmentVariable = environmentVariable;
    }

    public String propertyName() {
        return propertyName;
    }

    public String environmentVariable() {
        return environmentVariable;
    }
}
