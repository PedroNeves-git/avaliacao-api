package com.fiap.notification.infrastructure.function;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

// Mirrors the JSON published by fn-feedback on the queue.
@JsonIgnoreProperties(ignoreUnknown = true)
public record FeedbackEvent(Long id, String description, Integer rating, LocalDateTime createdAt) {
}
