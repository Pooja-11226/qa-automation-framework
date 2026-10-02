package com.sdet.api.client;

import com.sdet.api.config.ConfigManager;
import com.sdet.api.model.AuthRequest;
import com.sdet.api.model.AuthResponse;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Obtains the admin token once per run and shares it across threads (double-checked locking),
 * so parallel tests do not hammer the auth endpoint.
 */
public final class TokenManager {

    private static final Logger LOG = LoggerFactory.getLogger(TokenManager.class);
    private static final Object LOCK = new Object();
    private static volatile String cachedToken;

    private TokenManager() {
    }

    public static String getAdminToken() {
        String token = cachedToken;
        if (token == null) {
            synchronized (LOCK) {
                token = cachedToken;
                if (token == null) {
                    token = requestToken();
                    cachedToken = token;
                }
            }
        }
        return token;
    }

    private static String requestToken() {
        ConfigManager config = ConfigManager.getInstance();
        Response response = new AuthClient().createToken(new AuthRequest(config.username(), config.password()));
        if (response.statusCode() != HttpStatus.OK) {
            throw new IllegalStateException("Token request failed with HTTP " + response.statusCode());
        }
        AuthResponse body = response.as(AuthResponse.class);
        if (body.token() == null || body.token().isBlank()) {
            // The body only contains a reason such as "Bad credentials"; safe to surface.
            throw new IllegalStateException("No token issued. Check API_USERNAME/API_PASSWORD. Response: "
                    + response.asString());
        }
        LOG.info("Obtained admin auth token");
        return body.token();
    }
}
