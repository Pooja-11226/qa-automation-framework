package com.sdet.api.client;

/** HTTP status codes used by the suite, named so assertions read as intent rather than magic numbers. */
public final class HttpStatus {
    public static final int OK = 200;
    public static final int CREATED = 201;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int METHOD_NOT_ALLOWED = 405;
    public static final int UNSUPPORTED_MEDIA_TYPE = 415;
    /** Restful-Booker's response to an Accept header it cannot produce. */
    public static final int IM_A_TEAPOT = 418;

    private HttpStatus() {
    }
}
