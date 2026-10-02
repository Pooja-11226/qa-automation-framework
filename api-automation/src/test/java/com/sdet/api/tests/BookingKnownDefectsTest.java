package com.sdet.api.tests;

import static com.sdet.api.tests.support.TestGroups.KNOWN_DEFECT;
import static org.assertj.core.api.Assertions.assertThat;

import com.sdet.api.client.HttpStatus;
import com.sdet.api.client.MediaTypes;
import com.sdet.api.data.BookingDataFactory;
import com.sdet.api.data.BookingField;
import com.sdet.api.model.AuthRequest;
import com.sdet.api.model.Booking;
import com.sdet.api.model.BookingDates;
import com.sdet.api.model.CreateBookingResponse;
import com.sdet.api.tests.support.ApiAssertions;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Each test asserts the CORRECT behaviour for a defect found in Restful-Booker and is therefore expected to
 * FAIL until the defect is fixed. The group is excluded from the default run so CI stays meaningful; run it
 * with {@code mvn test -Pknown-defects} to get a live defect status. Details: docs/known-issues.md.
 */
@Feature("Known defects")
public class BookingKnownDefectsTest extends BaseApiTest {

    private static final int NON_EXISTENT_ID = 999_999_999;

    @DataProvider(name = "mandatoryFields")
    public Object[][] mandatoryFields() {
        return BookingField.mandatoryFields().stream().map(field -> new Object[] {field}).toArray(Object[][]::new);
    }

    @Test(dataProvider = "mandatoryFields", groups = KNOWN_DEFECT,
            description = "KD-API-01: missing mandatory field on create should return 400, not 500")
    public void createWithoutMandatoryFieldShouldReturnBadRequest(BookingField missingField) {
        ApiAssertions.assertStatus(
                bookingClient.create(BookingDataFactory.validBookingWithout(missingField)), HttpStatus.BAD_REQUEST);
    }

    @DataProvider(name = "wronglyTypedFields")
    public Object[][] wronglyTypedFields() {
        return new Object[][] {
            {BookingField.TOTAL_PRICE, "one hundred"},
            {BookingField.DEPOSIT_PAID, "yes"},
            {BookingField.FIRSTNAME, 12345},
            {BookingField.CHECKIN, "not-a-date"},
        };
    }

    @Test(dataProvider = "wronglyTypedFields", groups = KNOWN_DEFECT,
            description = "KD-API-02: wrongly typed field values should be rejected with 400")
    public void wronglyTypedFieldShouldReturnBadRequest(BookingField field, Object invalidValue) {
        Response response = bookingClient.create(BookingDataFactory.validBookingWith(field, invalidValue));
        cleanUpIfCreated(response);

        ApiAssertions.assertStatus(response, HttpStatus.BAD_REQUEST);
    }

    @Test(groups = KNOWN_DEFECT, description = "KD-API-03: unsupported content type should return 415, not 500")
    public void unsupportedContentTypeShouldReturn415() {
        String validJson = BookingDataFactory.toJson(BookingDataFactory.validBooking());

        ApiAssertions.assertStatus(
                bookingClient.createWithRawBody(validJson, MediaTypes.TEXT_PLAIN), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test(groups = KNOWN_DEFECT, description = "KD-API-04: updating a non-existent booking should return 404, not 405")
    public void updateUnknownBookingShouldReturnNotFound() {
        ApiAssertions.assertStatus(
                bookingClient.update(NON_EXISTENT_ID, BookingDataFactory.validBooking(), adminToken()), HttpStatus.NOT_FOUND);
    }

    @Test(groups = KNOWN_DEFECT, description = "KD-API-04: deleting a non-existent booking should return 404, not 405")
    public void deleteUnknownBookingShouldReturnNotFound() {
        ApiAssertions.assertStatus(bookingClient.delete(NON_EXISTENT_ID, adminToken()), HttpStatus.NOT_FOUND);
    }

    @Test(groups = KNOWN_DEFECT, description = "KD-API-05: check-out before check-in should be rejected with 400")
    public void checkoutBeforeCheckinShouldBeRejected() {
        LocalDate checkin = LocalDate.now().plusDays(10);
        Booking invalid = BookingDataFactory.validBooking().toBuilder()
                .bookingdates(new BookingDates(checkin.toString(), checkin.minusDays(3).toString()))
                .build();

        Response response = bookingClient.create(invalid);
        cleanUpIfCreated(response);

        ApiAssertions.assertStatus(response, HttpStatus.BAD_REQUEST);
    }

    @Test(groups = KNOWN_DEFECT, description = "KD-API-06: a create rejected for its Accept header must not store the booking")
    public void rejectedAcceptHeaderShouldNotCreateBooking() {
        Booking booking = BookingDataFactory.validBooking();

        Response response = bookingClient.createWithAcceptHeader(booking, MediaTypes.TEXT_PLAIN);
        List<Integer> stored = bookingClient.getIds(Map.of("firstname", booking.firstname()))
                .jsonPath().getList("bookingid", Integer.class);
        stored.forEach(this::registerForCleanup);

        ApiAssertions.assertStatus(response, HttpStatus.IM_A_TEAPOT);
        assertThat(stored).as("bookings stored despite the rejected request").isEmpty();
    }

    @Test(groups = KNOWN_DEFECT, description = "KD-API-07: invalid credentials should return 401, not 200")
    public void invalidCredentialsShouldReturnUnauthorized() {
        ApiAssertions.assertStatus(
                authClient.createToken(new AuthRequest(config.username(), "wrong-password")), HttpStatus.UNAUTHORIZED);
    }

    @Test(groups = KNOWN_DEFECT, description = "KD-API-08: PUT should remove optional fields omitted from the payload")
    public void putShouldReplaceTheWholeBooking() {
        CreateBookingResponse created = createBooking(BookingDataFactory.validBooking());
        Booking replacementWithoutOptionalField = BookingDataFactory.validBooking().toBuilder()
                .additionalneeds(null)
                .build();

        ApiAssertions.assertStatus(
                bookingClient.update(created.bookingid(), replacementWithoutOptionalField, adminToken()), HttpStatus.OK);

        assertThat(fetchBooking(created.bookingid()).additionalneeds())
                .as("additionalneeds after a PUT that omitted it")
                .isNull();
    }

    @Test(groups = KNOWN_DEFECT, description = "KD-API-09: a decimal total price should be stored without truncation")
    public void decimalTotalPriceShouldBePreserved() {
        double price = 150.75;
        Response response = bookingClient.create(BookingDataFactory.validBookingWith(BookingField.TOTAL_PRICE, price));
        cleanUpIfCreated(response);

        ApiAssertions.assertStatus(response, HttpStatus.OK);
        assertThat(response.jsonPath().getDouble("booking.totalprice")).as("stored total price").isEqualTo(price);
    }

    /** Some defects cause invalid bookings to be stored; make sure they are still cleaned up. */
    private void cleanUpIfCreated(Response response) {
        if (response.statusCode() == HttpStatus.OK && response.asString().contains("bookingid")) {
            registerForCleanup(response.jsonPath().getInt("bookingid"));
        }
    }
}
