package com.sdet.api.tests.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;

/**
 * Assertions that put the response body into the failure message, so a red test explains itself
 * without re-running it.
 */
public final class ApiAssertions {

    private static final int MAX_BODY_CHARS = 500;

    private ApiAssertions() {
    }

    public static void assertStatus(Response response, int expectedStatus) {
        assertThat(response.statusCode())
                .as("HTTP status. Response body: %s", abbreviatedBody(response))
                .isEqualTo(expectedStatus);
    }

    public static void assertClientError(Response response) {
        assertThat(response.statusCode())
                .as("Expected a 4xx client error. Response body: %s", abbreviatedBody(response))
                .isBetween(400, 499);
    }

    /**
     * The request must be refused (any 4xx/5xx) and must not have created a resource. Used where the API
     * rejects input but with the wrong status class; the precise status is asserted by the known-defect tests.
     */
    public static void assertRequestRejected(Response response) {
        assertThat(response.statusCode())
                .as("Expected the request to be rejected. Response body: %s", abbreviatedBody(response))
                .isGreaterThanOrEqualTo(400);
        assertThat(response.asString())
                .as("A rejected request must not return a booking id")
                .doesNotContain("bookingid");
    }

    private static String abbreviatedBody(Response response) {
        String body = response.asString();
        return body.length() <= MAX_BODY_CHARS ? body : body.substring(0, MAX_BODY_CHARS) + "...";
    }
}
