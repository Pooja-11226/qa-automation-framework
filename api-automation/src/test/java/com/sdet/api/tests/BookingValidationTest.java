package com.sdet.api.tests;

import static com.sdet.api.tests.support.TestGroups.NEGATIVE;
import static com.sdet.api.tests.support.TestGroups.REGRESSION;
import static org.assertj.core.api.Assertions.assertThat;

import com.sdet.api.client.HttpStatus;
import com.sdet.api.client.MediaTypes;
import com.sdet.api.data.BookingDataFactory;
import com.sdet.api.data.BookingField;
import com.sdet.api.model.Booking;
import com.sdet.api.model.CreateBookingResponse;
import com.sdet.api.tests.support.ApiAssertions;
import io.qameta.allure.Feature;
import java.util.Map;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Negative input handling. Where the API rejects bad input but with a debatable status code, these tests
 * assert the reliable contract (the request is rejected and nothing is created); the precise expected
 * status is asserted separately in {@link BookingKnownDefectsTest}.
 */
@Feature("Input validation")
public class BookingValidationTest extends BaseApiTest {

    private static final int NON_EXISTENT_ID = 999_999_999;

    @DataProvider(name = "unknownBookingIds")
    public Object[][] unknownBookingIds() {
        return new Object[][] {{NON_EXISTENT_ID}, {0}, {-1}, {"not-a-number"}};
    }

    @Test(dataProvider = "unknownBookingIds", groups = {NEGATIVE, REGRESSION},
            description = "GET /booking/{id} returns 404 for ids that do not exist or are invalid")
    public void getUnknownBookingReturnsNotFound(Object bookingId) {
        ApiAssertions.assertStatus(bookingClient.getById(bookingId), HttpStatus.NOT_FOUND);
    }

    @Test(groups = {NEGATIVE, REGRESSION}, description = "PUT to a non-existent booking is rejected with a client error")
    public void updateUnknownBookingIsRejected() {
        ApiAssertions.assertClientError(
                bookingClient.update(NON_EXISTENT_ID, BookingDataFactory.validBooking(), adminToken()));
    }

    @Test(groups = {NEGATIVE, REGRESSION}, description = "DELETE of a non-existent booking is rejected with a client error")
    public void deleteUnknownBookingIsRejected() {
        ApiAssertions.assertClientError(bookingClient.delete(NON_EXISTENT_ID, adminToken()));
    }

    @DataProvider(name = "mandatoryFields")
    public Object[][] mandatoryFields() {
        return BookingField.mandatoryFields().stream().map(field -> new Object[] {field}).toArray(Object[][]::new);
    }

    @Test(dataProvider = "mandatoryFields", groups = {NEGATIVE, REGRESSION},
            description = "POST /booking without a mandatory field is rejected")
    public void createWithoutMandatoryFieldIsRejected(BookingField missingField) {
        ApiAssertions.assertRequestRejected(bookingClient.create(BookingDataFactory.validBookingWithout(missingField)));
    }

    @Test(groups = {NEGATIVE, REGRESSION}, description = "POST /booking with an empty JSON object is rejected")
    public void createWithEmptyBodyIsRejected() {
        ApiAssertions.assertRequestRejected(bookingClient.create(Map.of()));
    }

    @Test(groups = {NEGATIVE, REGRESSION}, description = "POST /booking with malformed JSON returns 400")
    public void createWithMalformedJsonReturnsBadRequest() {
        String truncatedJson = "{\"firstname\": \"Broken\", \"lastname\": ";

        ApiAssertions.assertStatus(bookingClient.createWithRawBody(truncatedJson, MediaTypes.JSON), HttpStatus.BAD_REQUEST);
    }

    @Test(groups = {NEGATIVE, REGRESSION}, description = "POST /booking with an unsupported content type is rejected")
    public void createWithUnsupportedContentTypeIsRejected() {
        String validJson = BookingDataFactory.toJson(BookingDataFactory.validBooking());

        ApiAssertions.assertRequestRejected(bookingClient.createWithRawBody(validJson, MediaTypes.TEXT_PLAIN));
    }

    @Test(groups = {NEGATIVE, REGRESSION},
            description = "GET /booking/{id} with an unsupported Accept header returns 418 (documented API behaviour)")
    public void getWithUnsupportedAcceptHeaderIsRejected() {
        CreateBookingResponse created = createBooking(BookingDataFactory.validBooking());

        ApiAssertions.assertStatus(
                bookingClient.getByIdWithAcceptHeader(created.bookingid(), MediaTypes.TEXT_PLAIN), HttpStatus.IM_A_TEAPOT);
    }

    @Test(dataProvider = "mandatoryFields", groups = {NEGATIVE, REGRESSION},
            description = "PUT /booking/{id} without a mandatory field returns 400 and leaves the booking unchanged")
    public void updateWithoutMandatoryFieldReturnsBadRequest(BookingField missingField) {
        Booking original = BookingDataFactory.validBooking();
        CreateBookingResponse created = createBooking(original);

        ApiAssertions.assertStatus(
                bookingClient.update(created.bookingid(), BookingDataFactory.validBookingWithout(missingField), adminToken()),
                HttpStatus.BAD_REQUEST);

        assertThat(fetchBooking(created.bookingid())).as("booking after rejected update")
                .usingRecursiveComparison().isEqualTo(original);
    }
}
