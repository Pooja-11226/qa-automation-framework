package com.sdet.api.spec;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.ResponseSpecification;

/** Reusable response expectations: status, content type and JSON-schema contract checks. */
public final class ResponseSpecFactory {

    private ResponseSpecFactory() {
    }

    public static ResponseSpecification status(int expectedStatus) {
        return new ResponseSpecBuilder().expectStatusCode(expectedStatus).build();
    }

    public static ResponseSpecification json(int expectedStatus) {
        return new ResponseSpecBuilder()
                .expectStatusCode(expectedStatus)
                .expectContentType(ContentType.JSON)
                .build();
    }

    public static ResponseSpecification jsonMatchingSchema(int expectedStatus, String schemaClasspathLocation) {
        return new ResponseSpecBuilder()
                .addResponseSpecification(json(expectedStatus))
                .expectBody(matchesJsonSchemaInClasspath(schemaClasspathLocation))
                .build();
    }
}
