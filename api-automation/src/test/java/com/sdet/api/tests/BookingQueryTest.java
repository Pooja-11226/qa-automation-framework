package com.sdet.api.tests;

import static com.sdet.api.tests.support.TestGroups.REGRESSION;
import static com.sdet.api.tests.support.TestGroups.SMOKE;
import static org.assertj.core.api.Assertions.assertThat;

import com.sdet.api.client.HttpStatus;
import com.sdet.api.data.BookingDataFactory;
import com.sdet.api.model.Booking;
import com.sdet.api.model.CreateBookingResponse;
import com.sdet.api.spec.ResponseSpecFactory;
import com.sdet.api.tests.support.Schemas;
import io.qameta.allure.Feature;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;

@Feature("Booking search")
public class BookingQueryTest extends BaseApiTest {

    @Test(groups = {SMOKE, REGRESSION}, description = "GET /booking lists booking ids including a newly created one")
    public void listingBookingsIncludesNewBooking() {
        CreateBookingResponse created = createBooking(BookingDataFactory.validBooking());

        List<Integer> ids = bookingClient.getIds()
                .then()
                .spec(ResponseSpecFactory.jsonMatchingSchema(HttpStatus.OK, Schemas.BOOKING_IDS))
                .extract()
                .jsonPath()
                .getList("bookingid", Integer.class);

        assertThat(ids).as("booking ids").contains(created.bookingid());
    }

    @Test(groups = REGRESSION, description = "Filtering by first and last name returns exactly the matching booking")
    public void filteringByNameReturnsOnlyTheMatchingBooking() {
        Booking booking = BookingDataFactory.validBooking();
        CreateBookingResponse created = createBooking(booking);

        List<Integer> ids = bookingClient.getIds(Map.of("firstname", booking.firstname(), "lastname", booking.lastname()))
                .then()
                .spec(ResponseSpecFactory.jsonMatchingSchema(HttpStatus.OK, Schemas.BOOKING_IDS))
                .extract()
                .jsonPath()
                .getList("bookingid", Integer.class);

        // The first name is unique per run, so exactly one booking must match.
        assertThat(ids).as("ids matching the unique name").containsExactly(created.bookingid());
    }

    @Test(groups = REGRESSION, description = "Filtering by a name nobody has returns an empty list, not an error")
    public void filteringByUnknownNameReturnsEmptyList() {
        List<Integer> ids = bookingClient.getIds(Map.of("firstname", BookingDataFactory.uniqueFirstName()))
                .then()
                .spec(ResponseSpecFactory.jsonMatchingSchema(HttpStatus.OK, Schemas.BOOKING_IDS))
                .extract()
                .jsonPath()
                .getList("bookingid", Integer.class);

        assertThat(ids).as("ids matching an unused name").isEmpty();
    }
}
