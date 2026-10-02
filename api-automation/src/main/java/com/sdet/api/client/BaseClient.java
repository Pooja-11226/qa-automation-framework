package com.sdet.api.client;

import static io.restassured.RestAssured.given;

import com.sdet.api.spec.RequestSpecFactory;
import io.restassured.specification.RequestSpecification;

/**
 * Common request setup for all clients. Clients return the raw {@code Response} and never assert,
 * so the same method serves positive and negative tests; the tests own the expectations.
 */
abstract class BaseClient {

    protected RequestSpecification request() {
        return request(Auth.none());
    }

    protected RequestSpecification request(Auth auth) {
        return auth.applyTo(given().spec(RequestSpecFactory.defaultSpec()));
    }

    /** For requests that carry credentials in the body; see {@link RequestSpecFactory#credentialsSpec()}. */
    protected RequestSpecification credentialsRequest() {
        return given().spec(RequestSpecFactory.credentialsSpec());
    }
}
