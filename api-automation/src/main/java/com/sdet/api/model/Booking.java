package com.sdet.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Booking payload and response body. Null fields are omitted from JSON, which lets the same type express
 * full bookings, PATCH payloads and "missing field" payloads. Unknown response fields are tolerated here;
 * strict contract checking is the job of the JSON schemas.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Booking(
        String firstname,
        String lastname,
        Integer totalprice,
        Boolean depositpaid,
        BookingDates bookingdates,
        String additionalneeds) {

    /** Expected state after a PATCH: every non-null field of {@code patch} replaces the current value. */
    public Booking mergedWith(Booking patch) {
        return new Booking(
                patch.firstname() != null ? patch.firstname() : firstname,
                patch.lastname() != null ? patch.lastname() : lastname,
                patch.totalprice() != null ? patch.totalprice() : totalprice,
                patch.depositpaid() != null ? patch.depositpaid() : depositpaid,
                patch.bookingdates() != null ? patch.bookingdates() : bookingdates,
                patch.additionalneeds() != null ? patch.additionalneeds() : additionalneeds);
    }

    public Builder toBuilder() {
        return new Builder()
                .firstname(firstname)
                .lastname(lastname)
                .totalprice(totalprice)
                .depositpaid(depositpaid)
                .bookingdates(bookingdates)
                .additionalneeds(additionalneeds);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String firstname;
        private String lastname;
        private Integer totalprice;
        private Boolean depositpaid;
        private BookingDates bookingdates;
        private String additionalneeds;

        private Builder() {
        }

        public Builder firstname(String value) {
            this.firstname = value;
            return this;
        }

        public Builder lastname(String value) {
            this.lastname = value;
            return this;
        }

        public Builder totalprice(Integer value) {
            this.totalprice = value;
            return this;
        }

        public Builder depositpaid(Boolean value) {
            this.depositpaid = value;
            return this;
        }

        public Builder bookingdates(BookingDates value) {
            this.bookingdates = value;
            return this;
        }

        public Builder additionalneeds(String value) {
            this.additionalneeds = value;
            return this;
        }

        public Booking build() {
            return new Booking(firstname, lastname, totalprice, depositpaid, bookingdates, additionalneeds);
        }
    }
}
