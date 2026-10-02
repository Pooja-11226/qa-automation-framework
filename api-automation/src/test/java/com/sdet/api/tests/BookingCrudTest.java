package com.sdet.api.tests;

import static com.sdet.api.tests.support.TestGroups.REGRESSION;
import static com.sdet.api.tests.support.TestGroups.SMOKE;
import static org.assertj.core.api.Assertions.assertThat;

import com.sdet.api.client.AuthMethod;
import com.sdet.api.client.HttpStatus;
import com.sdet.api.data.BookingDataFactory;
import com.sdet.api.model.Booking;
import com.sdet.api.model.BookingDates;
import com.sdet.api.model.CreateBookingResponse;
import com.sdet.api.spec.ResponseSpecFactory;
import com.sdet.api.tests.support.ApiAssertions;
import com.sdet.api.tests.support.Schemas;
import io.qameta.allure.Feature;
import io.restassured.response.Response;
import java.time.LocalDate;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Feature("Booking CRUD")
public class BookingCrudTest extends BaseApiTest {

    @Test(groups = {SMOKE, REGRESSION}, description = "POST /booking creates a booking and echoes the payload")
    public void createBookingReturnsTheCreatedBooking() {
        Booking payload = BookingDataFactory.validBooking();

        Response response = bookingClient.create(payload);

        response.then().spec(ResponseSpecFactory.jsonMatchingSchema(HttpStatus.OK, Schemas.CREATE_BOOKING_RESPONSE));
        CreateBookingResponse created = response.as(CreateBookingResponse.class);
        registerForCleanup(created.bookingid());
        assertThat(created.bookingid()).as("generated booking id").isPositive();
        assertThat(created.booking()).as("echoed booking").usingRecursiveComparison().isEqualTo(payload);
    }

    @DataProvider(name = "bookingVariants")
    public Object[][] bookingVariants() {
        String sameDay = LocalDate.now().plusDays(7).toString();
        return new Object[][] {
            {"deposit not paid", variant().depositpaid(false).build()},
            {"optional additional needs omitted", variant().additionalneeds(null).build()},
            {"zero total price (lower boundary)", variant().totalprice(0).build()},
            {"same-day check-in and check-out", variant().bookingdates(new BookingDates(sameDay, sameDay)).build()},
            {"accented and apostrophe names",
                variant().firstname(BookingDataFactory.uniqueFirstName() + "-Zoë").lastname("O'Brien-Núñez").build()},
        };
    }

    private static Booking.Builder variant() {
        return BookingDataFactory.validBooking().toBuilder();
    }

    @Test(dataProvider = "bookingVariants", groups = REGRESSION,
            description = "Valid booking variants are persisted exactly as sent")
    public void bookingVariantsArePersistedAsSent(String scenario, Booking payload) {
        CreateBookingResponse created = createBooking(payload);

        Response fetched = bookingClient.getById(created.bookingid());

        fetched.then().spec(ResponseSpecFactory.jsonMatchingSchema(HttpStatus.OK, Schemas.BOOKING));
        assertThat(fetched.as(Booking.class)).as(scenario).usingRecursiveComparison().isEqualTo(payload);
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "GET /booking/{id} returns the stored booking")
    public void getBookingByIdReturnsTheStoredBooking() {
        Booking payload = BookingDataFactory.validBooking();
        CreateBookingResponse created = createBooking(payload);

        Response response = bookingClient.getById(created.bookingid());

        response.then().spec(ResponseSpecFactory.jsonMatchingSchema(HttpStatus.OK, Schemas.BOOKING));
        assertThat(response.as(Booking.class)).usingRecursiveComparison().isEqualTo(payload);
    }

    @Test(groups = REGRESSION, description = "Leading and trailing spaces in names are trimmed on create")
    public void namesAreTrimmedOnCreate() {
        Booking expected = BookingDataFactory.validBooking();
        Booking padded = expected.toBuilder()
                .firstname("  " + expected.firstname() + "  ")
                .lastname("  " + expected.lastname() + "  ")
                .build();

        CreateBookingResponse created = createBooking(padded);

        assertThat(created.booking()).usingRecursiveComparison().isEqualTo(expected);
    }

    @DataProvider(name = "authMethods")
    public Object[][] authMethods() {
        return new Object[][] {{AuthMethod.TOKEN_COOKIE}, {AuthMethod.BASIC_AUTH}};
    }

    @Test(dataProvider = "authMethods", groups = {SMOKE, REGRESSION},
            description = "PUT /booking/{id} replaces every field (token and basic auth)")
    public void updateBookingReplacesAllFields(AuthMethod authMethod) {
        CreateBookingResponse created = createBooking(BookingDataFactory.validBooking());
        Booking replacement = BookingDataFactory.validBooking();

        Response response = bookingClient.update(created.bookingid(), replacement, authFor(authMethod));

        response.then().spec(ResponseSpecFactory.jsonMatchingSchema(HttpStatus.OK, Schemas.BOOKING));
        assertThat(response.as(Booking.class)).as("PUT response").usingRecursiveComparison().isEqualTo(replacement);
        assertThat(fetchBooking(created.bookingid())).as("persisted booking")
                .usingRecursiveComparison().isEqualTo(replacement);
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "PATCH /booking/{id} changes only the fields provided")
    public void partialUpdateChangesOnlyProvidedFields() {
        Booking original = BookingDataFactory.validBooking();
        CreateBookingResponse created = createBooking(original);
        Booking patch = BookingDataFactory.partialUpdate();
        Booking expected = original.mergedWith(patch);

        Response response = bookingClient.partialUpdate(created.bookingid(), patch, adminToken());

        response.then().spec(ResponseSpecFactory.jsonMatchingSchema(HttpStatus.OK, Schemas.BOOKING));
        assertThat(response.as(Booking.class)).as("PATCH response").usingRecursiveComparison().isEqualTo(expected);
        assertThat(fetchBooking(created.bookingid())).as("persisted booking: untouched fields must not change")
                .usingRecursiveComparison().isEqualTo(expected);
    }

    @Test(groups = {SMOKE, REGRESSION}, description = "DELETE /booking/{id} removes the booking")
    public void deleteBookingRemovesIt() {
        CreateBookingResponse created = createBooking(BookingDataFactory.validBooking());

        Response response = bookingClient.delete(created.bookingid(), adminToken());

        // Restful-Booker answers a successful DELETE with 201 Created (documented API behaviour).
        ApiAssertions.assertStatus(response, HttpStatus.CREATED);
        ApiAssertions.assertStatus(bookingClient.getById(created.bookingid()), HttpStatus.NOT_FOUND);
    }
}
