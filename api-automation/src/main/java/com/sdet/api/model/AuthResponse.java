package com.sdet.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Either {@code token} (success) or {@code reason} (failure) is populated. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthResponse(String token, String reason) {

    @Override
    public String toString() {
        return "AuthResponse[token=" + (token == null ? null : "***") + ", reason=" + reason + "]";
    }
}
