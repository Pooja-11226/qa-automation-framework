package com.sdet.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateBookingResponse(Integer bookingid, Booking booking) {
}
