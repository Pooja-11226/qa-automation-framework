package com.sdet.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Dates are ISO-8601 strings (yyyy-MM-dd) as the API expects; strings also allow invalid-date negative tests. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingDates(String checkin, String checkout) {
}
