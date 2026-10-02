package com.sdet.api.client;

/**
 * Exact media-type strings. Restful-Booker compares the Accept header literally, so REST Assured's
 * {@code ContentType.JSON} (which expands to several comma-separated types) must NOT be used for Accept.
 */
public final class MediaTypes {
    public static final String JSON = "application/json";
    public static final String TEXT_PLAIN = "text/plain";

    private MediaTypes() {
    }
}
