package com.sdet.api.client;

import io.restassured.response.Response;

public class AuthClient extends BaseClient {

    private static final String AUTH = "/auth";

    /**
     * Accepts any payload (a typed {@code AuthRequest} or a raw map) so invalid bodies can be sent too.
     * Uses the credentials spec so the password and issued token never appear in Allure attachments.
     */
    public Response createToken(Object credentialsPayload) {
        return credentialsRequest().body(credentialsPayload).post(AUTH);
    }
}
