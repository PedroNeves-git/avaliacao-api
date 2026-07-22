package com.fiap.notification.domain;

public enum Urgency {
    CRITICAL,
    MODERATE,
    NORMAL;

    public static Urgency fromRating(int rating) {
        if (rating < 0 || rating > 10) {
            throw new IllegalArgumentException("Rating must be between 0 and 10: " + rating);
        }
        if (rating <= 3) {
            return CRITICAL;
        }
        if (rating <= 6) {
            return MODERATE;
        }
        return NORMAL;
    }

    public boolean isCritical() {
        return this == CRITICAL;
    }
}
