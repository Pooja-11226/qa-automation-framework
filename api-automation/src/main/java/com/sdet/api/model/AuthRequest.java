package com.sdet.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthRequest(String username, String password) {

    /** Masked: this record is used as a test parameter and must not leak passwords into logs or reports. */
    @Override
    public String toString() {
        return "AuthRequest[username=" + username + ", password=***]";
    }
}
