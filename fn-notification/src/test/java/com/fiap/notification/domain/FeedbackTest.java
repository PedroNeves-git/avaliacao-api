package com.fiap.notification.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedbackTest {

    private static Feedback feedbackWithRating(int rating) {
        return new Feedback(1L, "confusing class", rating, LocalDateTime.now());
    }

    @Test
    void derivesUrgencyFromRating() {
        assertEquals(Urgency.CRITICAL, feedbackWithRating(2).urgency());
        assertTrue(feedbackWithRating(2).isCritical());
        assertFalse(feedbackWithRating(8).isCritical());
    }

    @Test
    void rejectsBlankDescription() {
        assertThrows(IllegalArgumentException.class,
                () -> new Feedback(1L, "  ", 2, LocalDateTime.now()));
    }

    @Test
    void rejectsNullCreatedAt() {
        assertThrows(IllegalArgumentException.class,
                () -> new Feedback(1L, "confusing class", 2, null));
    }

    @Test
    void rejectsRatingOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new Feedback(1L, "confusing class", 42, LocalDateTime.now()));
    }
}
