package com.sdet.api.client;

import io.restassured.response.Response;
import java.util.Map;

/**
 * Restful-Booker /booking resource. Path parameters are typed as {@code Object} on purpose so negative
 * tests can send non-numeric IDs; payloads are {@code Object} so tests can send POJOs or deliberately broken maps.
 */
public class BookingClient extends BaseClient {

    private static final String BOOKINGS = "/booking";
    private static final String BOOKING_BY_ID = "/booking/{id}";
    private static final String ID = "id";

    public Response create(Object payload) {
        return request().body(payload).post(BOOKINGS);
    }

    public Response createWithAcceptHeader(Object payload, String accept) {
        return request().accept(accept).body(payload).post(BOOKINGS);
    }

    /** Sends a body exactly as given, e.g. malformed JSON or a non-JSON content type. */
    public Response createWithRawBody(String rawBody, String contentType) {
        return request().contentType(contentType).body(rawBody).post(BOOKINGS);
    }

    public Response getById(Object id) {
        return request().pathParam(ID, id).get(BOOKING_BY_ID);
    }

    public Response getByIdWithAcceptHeader(Object id, String accept) {
        return request().accept(accept).pathParam(ID, id).get(BOOKING_BY_ID);
    }

    public Response getIds() {
        return getIds(Map.of());
    }

    public Response getIds(Map<String, ?> queryParameters) {
        return request().queryParams(queryParameters).get(BOOKINGS);
    }

    public Response update(Object id, Object payload, Auth auth) {
        return request(auth).pathParam(ID, id).body(payload).put(BOOKING_BY_ID);
    }

    public Response partialUpdate(Object id, Object payload, Auth auth) {
        return request(auth).pathParam(ID, id).body(payload).patch(BOOKING_BY_ID);
    }

    public Response delete(Object id, Auth auth) {
        return request(auth).pathParam(ID, id).delete(BOOKING_BY_ID);
    }
}
