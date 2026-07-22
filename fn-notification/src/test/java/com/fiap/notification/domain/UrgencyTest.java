package com.fiap.notification.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrgencyTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void classifiesLowRatingsAsCritical(int rating) {
        assertEquals(Urgency.CRITICAL, Urgency.fromRating(rating));
        assertTrue(Urgency.fromRating(rating).isCritical());
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 5, 6})
    void classifiesMidRatingsAsModerate(int rating) {
        assertEquals(Urgency.MODERATE, Urgency.fromRating(rating));
        assertFalse(Urgency.fromRating(rating).isCritical());
    }

    @ParameterizedTest
    @ValueSource(ints = {7, 8, 9, 10})
    void classifiesHighRatingsAsNormal(int rating) {
        assertEquals(Urgency.NORMAL, Urgency.fromRating(rating));
        assertFalse(Urgency.fromRating(rating).isCritical());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 11, 100})
    void rejectsRatingsOutOfRange(int rating) {
        assertThrows(IllegalArgumentException.class, () -> Urgency.fromRating(rating));
    }
}
