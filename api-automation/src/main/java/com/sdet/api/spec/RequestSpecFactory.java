package com.sdet.api.spec;

import com.sdet.api.client.MediaTypes;
import com.sdet.api.config.ConfigManager;
import com.sdet.api.listener.Slf4jLoggingFilter;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.specification.RequestSpecification;

/**
 * Builds the base request specifications shared by every client. A new instance is built per request and
 * no global {@code RestAssured.*} static state is used, which keeps parallel execution thread-safe.
 */
public final class RequestSpecFactory {

    private static final String CONNECTION_TIMEOUT_PARAM = "http.connection.timeout";
    private static final String SOCKET_TIMEOUT_PARAM = "http.socket.timeout";
    // Masked in console logs AND in Allure attachments (the Allure filter honours this blacklist).
    private static final String[] SENSITIVE_HEADERS = {"Authorization", "Cookie"};

    private RequestSpecFactory() {
    }

    /** Standard spec: every exchange is attached to the Allure report and dumped to the console if a check fails. */
    public static RequestSpecification defaultSpec() {
        return baseBuilder(true).addFilter(new AllureRestAssured()).build();
    }

    /**
     * For requests whose BODY carries credentials (POST /auth sends the password and receives the token).
     * Bodies cannot be masked by header blacklists, so these exchanges are not attached to Allure or dumped
     * to the console; they are still logged by {@link Slf4jLoggingFilter}, which redacts secrets.
     */
    public static RequestSpecification credentialsSpec() {
        return baseBuilder(false).build();
    }

    private static RequestSpecBuilder baseBuilder(boolean dumpExchangeOnValidationFailure) {
        ConfigManager config = ConfigManager.getInstance();
        return new RequestSpecBuilder()
                .setBaseUri(config.baseUri())
                .setContentType(MediaTypes.JSON)
                .setAccept(MediaTypes.JSON)
                .setConfig(restAssuredConfig(config, dumpExchangeOnValidationFailure))
                .addFilter(new Slf4jLoggingFilter());
    }

    private static RestAssuredConfig restAssuredConfig(ConfigManager config, boolean dumpExchangeOnValidationFailure) {
        LogConfig logConfig = LogConfig.logConfig().blacklistHeader(SENSITIVE_HEADERS[0], SENSITIVE_HEADERS[1]);
        if (dumpExchangeOnValidationFailure) {
            logConfig = logConfig.enableLoggingOfRequestAndResponseIfValidationFails();
        }
        return RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam(CONNECTION_TIMEOUT_PARAM, config.connectTimeoutMs())
                        .setParam(SOCKET_TIMEOUT_PARAM, config.readTimeoutMs()))
                .logConfig(logConfig);
    }
}
