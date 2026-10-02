package com.sdet.api.data;

import java.util.Arrays;
import java.util.List;

/** JSON paths of booking fields, used to build payloads with a field removed or replaced. */
public enum BookingField {
    FIRSTNAME("firstname", true),
    LASTNAME("lastname", true),
    TOTAL_PRICE("totalprice", true),
    DEPOSIT_PAID("depositpaid", true),
    BOOKING_DATES("bookingdates", true),
    CHECKIN("bookingdates.checkin", true),
    CHECKOUT("bookingdates.checkout", true),
    ADDITIONAL_NEEDS("additionalneeds", false);

    private final String jsonPath;
    private final boolean mandatory;

    BookingField(String jsonPath, boolean mandatory) {
        this.jsonPath = jsonPath;
        this.mandatory = mandatory;
    }

    public String jsonPath() {
        return jsonPath;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public static List<BookingField> mandatoryFields() {
        return Arrays.stream(values()).filter(BookingField::isMandatory).toList();
    }
}
