package com.sdet.api.tests;

import com.sdet.api.client.Auth;
import com.sdet.api.client.AuthClient;
import com.sdet.api.client.AuthMethod;
import com.sdet.api.client.BookingClient;
import com.sdet.api.client.HealthClient;
import com.sdet.api.client.HttpStatus;
import com.sdet.api.client.TokenManager;
import com.sdet.api.config.ConfigManager;
import com.sdet.api.listener.TestLoggingListener;
import com.sdet.api.model.Booking;
import com.sdet.api.model.CreateBookingResponse;
import com.sdet.api.spec.ResponseSpecFactory;
import io.restassured.response.Response;
import java.util.ArrayDeque;
import java.util.Deque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;

/**
 * Shared test infrastructure: clients, credentials, a fail-fast health check and automatic cleanup of
 * every booking a test creates. Tests never depend on each other or on pre-seeded data.
 */
@Listeners(TestLoggingListener.class)
public abstract class BaseApiTest {

    private static final Logger LOG = LoggerFactory.getLogger(BaseApiTest.class);

    protected final BookingClient bookingClient = new BookingClient();
    protected final AuthClient authClient = new AuthClient();
    protected final ConfigManager config = ConfigManager.getInstance();

    // TestNG runs @AfterMethod on the same thread as the test, so a ThreadLocal keeps parallel tests isolated.
    private final ThreadLocal<Deque<Integer>> bookingsToDelete = ThreadLocal.withInitial(ArrayDeque::new);

    /** Fails the whole run with one clear message if the API is down, instead of dozens of confusing failures. */
    @BeforeSuite(alwaysRun = true)
    public void verifyApiIsReachable() {
        if (!config.healthCheckEnabled()) {
            LOG.info("Health check disabled by configuration");
            return;
        }
        Response response = new HealthClient().ping();
        if (response.statusCode() != HttpStatus.CREATED) {
            throw new IllegalStateException(String.format(
                    "API health check failed: GET %s/ping returned HTTP %d. Is the API up?",
                    config.baseUri(), response.statusCode()));
        }
        LOG.info("API health check passed for {}", config.baseUri());
    }

    protected Auth adminToken() {
        return Auth.token(TokenManager.getAdminToken());
    }

    protected Auth adminBasicAuth() {
        return Auth.basic(config.username(), config.password());
    }

    protected Auth authFor(AuthMethod method) {
        return switch (method) {
            case TOKEN_COOKIE -> adminToken();
            case BASIC_AUTH -> adminBasicAuth();
        };
    }

    /** Creates a booking as a precondition (asserting it succeeded) and schedules it for deletion. */
    protected CreateBookingResponse createBooking(Object payload) {
        CreateBookingResponse created = bookingClient.create(payload)
                .then()
                .spec(ResponseSpecFactory.json(HttpStatus.OK))
                .extract()
                .as(CreateBookingResponse.class);
        registerForCleanup(created.bookingid());
        return created;
    }

    protected Booking fetchBooking(int bookingId) {
        return bookingClient.getById(bookingId)
                .then()
                .spec(ResponseSpecFactory.json(HttpStatus.OK))
                .extract()
                .as(Booking.class);
    }

    protected void registerForCleanup(Integer bookingId) {
        if (bookingId != null) {
            bookingsToDelete.get().push(bookingId);
        }
    }

    @AfterMethod(alwaysRun = true)
    public void deleteCreatedBookings() {
        Deque<Integer> ids = bookingsToDelete.get();
        while (!ids.isEmpty()) {
            Integer id = ids.pop();
            try {
                int status = bookingClient.delete(id, adminToken()).statusCode();
                // 405 means it was already deleted, e.g. by a DELETE test.
                if (status != HttpStatus.CREATED && status != HttpStatus.METHOD_NOT_ALLOWED) {
                    LOG.warn("Cleanup of booking {} returned HTTP {}", id, status);
                }
            } catch (RuntimeException e) {
                // Never let cleanup mask the real test result.
                LOG.warn("Cleanup of booking {} failed: {}", id, e.toString());
            }
        }
        bookingsToDelete.remove();
    }
}
