package com.sdet.api.listener;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Routes HTTP traffic into SLF4J: a one-line summary at INFO (method, URI, status, duration) and the full
 * exchange at DEBUG (written to target/logs/api-tests.log). Credential headers and JSON "password"/"token"
 * values are masked.
 */
public class Slf4jLoggingFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(Slf4jLoggingFilter.class);
    private static final Set<String> SENSITIVE_HEADERS = Set.of("authorization", "cookie");
    private static final String MASK = "***";
    // Masks the value of any JSON "password" or "token" field, e.g. in POST /auth requests and responses.
    private static final Pattern SECRET_JSON_FIELD = Pattern.compile("(\"(?:password|token)\"\\s*:\\s*\")[^\"]*(\")");

    @Override
    public Response filter(FilterableRequestSpecification request,
                           FilterableResponseSpecification responseSpec,
                           FilterContext context) {
        long start = System.nanoTime();
        Response response = context.next(request, responseSpec);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        LOG.info("{} {} -> {} ({} ms)", request.getMethod(), request.getURI(), response.getStatusCode(), elapsedMs);
        if (LOG.isDebugEnabled()) {
            Object body = request.getBody();
            LOG.debug("Request headers: {}", masked(request.getHeaders()));
            LOG.debug("Request body: {}", body == null ? "<none>" : redacted(body.toString()));
            LOG.debug("Response headers: {}", masked(response.getHeaders()));
            LOG.debug("Response body: {}", redacted(response.asPrettyString()));
        }
        return response;
    }

    static String redacted(String body) {
        return SECRET_JSON_FIELD.matcher(body).replaceAll("$1" + MASK + "$2");
    }

    private static String masked(Headers headers) {
        return headers.asList().stream()
                .map(header -> header.getName() + "="
                        + (SENSITIVE_HEADERS.contains(header.getName().toLowerCase(Locale.ROOT)) ? MASK : header.getValue()))
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
