package com.sdet.api.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdet.api.model.Booking;
import com.sdet.api.model.BookingDates;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.datafaker.Faker;

/**
 * Creates booking test data. Every booking gets a unique first name so tests never collide in the shared
 * public environment and can be found again via query parameters. Set {@code -Ddata.seed=<n>} to make the
 * generated values reproducible (the uniqueness suffix is always random).
 */
public final class BookingDataFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final String NAME_PREFIX = "QA";
    private static final String[] ADDITIONAL_NEEDS = {"Breakfast", "Late checkout", "Airport transfer", "Extra pillows"};
    // Faker is not documented as thread-safe; one instance per thread keeps parallel runs safe.
    private static final ThreadLocal<Faker> FAKER = ThreadLocal.withInitial(BookingDataFactory::newFaker);

    private BookingDataFactory() {
    }

    public static Booking validBooking() {
        Faker faker = FAKER.get();
        LocalDate checkin = LocalDate.now().plusDays(faker.number().numberBetween(1, 60));
        LocalDate checkout = checkin.plusDays(faker.number().numberBetween(1, 14));
        return Booking.builder()
                .firstname(uniqueFirstName())
                .lastname(faker.name().lastName())
                .totalprice(faker.number().numberBetween(50, 2_000))
                .depositpaid(faker.bool().bool())
                .bookingdates(new BookingDates(checkin.toString(), checkout.toString()))
                .additionalneeds(faker.options().option(ADDITIONAL_NEEDS))
                .build();
    }

    /** PATCH payload that changes two fields and leaves the rest untouched. */
    public static Booking partialUpdate() {
        return Booking.builder()
                .firstname(uniqueFirstName())
                .additionalneeds("Late checkout")
                .build();
    }

    public static String uniqueFirstName() {
        String shortId = UUID.randomUUID().toString().substring(0, 8);
        return NAME_PREFIX + "-" + FAKER.get().name().firstName() + "-" + shortId;
    }

    public static Map<String, Object> asMap(Booking booking) {
        return MAPPER.convertValue(booking, MAP_TYPE);
    }

    public static String toJson(Object payload) {
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialise payload", e);
        }
    }

    /** A valid booking with one field removed, e.g. for "missing mandatory field" tests. */
    public static Map<String, Object> validBookingWithout(BookingField field) {
        Map<String, Object> payload = asMap(validBooking());
        String[] path = field.jsonPath().split("\\.");
        parentOf(payload, path).remove(path[path.length - 1]);
        return payload;
    }

    /** A valid booking with one field set to an arbitrary (possibly wrongly typed) value. */
    public static Map<String, Object> validBookingWith(BookingField field, Object value) {
        Map<String, Object> payload = asMap(validBooking());
        String[] path = field.jsonPath().split("\\.");
        parentOf(payload, path).put(path[path.length - 1], value);
        return payload;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parentOf(Map<String, Object> root, String[] path) {
        Map<String, Object> current = root;
        for (int i = 0; i < path.length - 1; i++) {
            Object child = current.computeIfAbsent(path[i], key -> new LinkedHashMap<String, Object>());
            current = (Map<String, Object>) child;
        }
        return current;
    }

    private static Faker newFaker() {
        String seed = System.getProperty("data.seed");
        return seed == null ? new Faker() : new Faker(new Random(Long.parseLong(seed)));
    }
}
