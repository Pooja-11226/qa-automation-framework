package com.sdet.api.client;

import io.restassured.specification.RequestSpecification;
import java.util.Objects;

/**
 * How a request authenticates. Modelled as a small sealed hierarchy so positive and negative tests
 * can pass "valid token", "forged token", "wrong basic credentials" or "nothing" through the same client call.
 * {@code toString()} is masked because these values appear in logs and reports as test parameters.
 */
public sealed interface Auth permits Auth.TokenCookie, Auth.BasicAuth, Auth.NoAuth {

    String TOKEN_COOKIE_NAME = "token";

    RequestSpecification applyTo(RequestSpecification request);

    static Auth token(String token) {
        return new TokenCookie(token);
    }

    static Auth basic(String username, String password) {
        return new BasicAuth(username, password);
    }

    static Auth none() {
        return new NoAuth();
    }

    record TokenCookie(String token) implements Auth {
        public TokenCookie {
            Objects.requireNonNull(token, "token");
        }

        /**
         * Sent as a raw Cookie header rather than via {@code request.cookie(...)}: the server sees the same
         * cookie, but the "Cookie" header is blacklisted, so Allure and the logs mask the token. Allure does
         * not mask cookies added with {@code cookie(...)}.
         */
        @Override
        public RequestSpecification applyTo(RequestSpecification request) {
            return request.header("Cookie", TOKEN_COOKIE_NAME + "=" + token);
        }

        @Override
        public String toString() {
            return "TokenCookie[***]";
        }
    }

    record BasicAuth(String username, String password) implements Auth {
        public BasicAuth {
            Objects.requireNonNull(username, "username");
            Objects.requireNonNull(password, "password");
        }

        @Override
        public RequestSpecification applyTo(RequestSpecification request) {
            return request.auth().preemptive().basic(username, password);
        }

        @Override
        public String toString() {
            return "BasicAuth[" + username + ":***]";
        }
    }

    record NoAuth() implements Auth {
        @Override
        public RequestSpecification applyTo(RequestSpecification request) {
            return request;
        }

        @Override
        public String toString() {
            return "NoAuth";
        }
    }
}
