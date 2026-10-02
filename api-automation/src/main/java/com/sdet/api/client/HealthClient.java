package com.sdet.api.client;

import io.restassured.response.Response;

public class HealthClient extends BaseClient {

    private static final String PING = "/ping";

    public Response ping() {
        return request().get(PING);
    }
}
