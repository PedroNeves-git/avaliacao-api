package com.fiap.notification.domain;

import java.time.LocalDateTime;

public record Feedback(Long id, String description, int rating, LocalDateTime createdAt) {

    public Feedback {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
        Urgency.fromRating(rating);
    }

    public Urgency urgency() {
        return Urgency.fromRating(rating);
    }

    public boolean isCritical() {
        return urgency().isCritical();
    }
}
