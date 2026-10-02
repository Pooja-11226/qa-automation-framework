package com.sdet.api.tests;

import static com.sdet.api.tests.support.TestGroups.NEGATIVE;
import static com.sdet.api.tests.support.TestGroups.REGRESSION;
import static org.assertj.core.api.Assertions.assertThat;

import com.sdet.api.client.Auth;
import com.sdet.api.client.HttpStatus;
import com.sdet.api.data.BookingDataFactory;
import com.sdet.api.model.Booking;
import com.sdet.api.model.CreateBookingResponse;
import com.sdet.api.tests.support.ApiAssertions;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Feature("Authorization")
public class BookingAuthorizationTest extends BaseApiTest {

    enum WriteOperation {
        UPDATE,
        PARTIAL_UPDATE,
        DELETE
    }

    @DataProvider(name = "unauthorizedWrites")
    public Object[][] unauthorizedWrites() {
        Object[][] invalidAuth = {
            {"no credentials", Auth.none()},
            {"forged token", Auth.token("forged-" + UUID.randomUUID())},
            {"wrong basic-auth password", Auth.basic(config.username(), "wrong-password")},
        };
        List<Object[]> rows = new ArrayList<>();
        for (WriteOperation operation : WriteOperation.values()) {
            for (Object[] auth : invalidAuth) {
                rows.add(new Object[] {operation, auth[0], auth[1]});
            }
        }
        return rows.toArray(new Object[0][]);
    }

    @Test(dataProvider = "unauthorizedWrites", groups = {NEGATIVE, REGRESSION},
            description = "Write operations without valid credentials are forbidden and change nothing")
    public void writeWithoutValidCredentialsIsForbidden(WriteOperation operation, String authDescription, Auth auth) {
        Booking original = BookingDataFactory.validBooking();
        CreateBookingResponse created = createBooking(original);

        Response response = perform(operation, created.bookingid(), auth);

        ApiAssertions.assertStatus(response, HttpStatus.FORBIDDEN);
        assertThat(fetchBooking(created.bookingid()))
                .as("booking must be unchanged after a forbidden %s with %s", operation, authDescription)
                .usingRecursiveComparison()
                .isEqualTo(original);
    }

    private Response perform(WriteOperation operation, int bookingId, Auth auth) {
        return switch (operation) {
            case UPDATE -> bookingClient.update(bookingId, BookingDataFactory.validBooking(), auth);
            case PARTIAL_UPDATE -> bookingClient.partialUpdate(bookingId, BookingDataFactory.partialUpdate(), auth);
            case DELETE -> bookingClient.delete(bookingId, auth);
        };
    }
}
