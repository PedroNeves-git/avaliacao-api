package com.fiap.notification.gateway;

import com.fiap.notification.domain.Feedback;
import com.fiap.notification.domain.Urgency;

import java.time.LocalDateTime;

public record AlertMessage(String description, Urgency urgency, LocalDateTime createdAt) {

    public static AlertMessage from(Feedback feedback) {
        return new AlertMessage(feedback.description(), feedback.urgency(), feedback.createdAt());
    }
}
