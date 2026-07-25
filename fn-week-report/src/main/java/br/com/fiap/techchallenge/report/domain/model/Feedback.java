package br.com.fiap.techchallenge.report.domain.model;

import java.time.LocalDateTime;

public record Feedback(
        String description,
        int rating,
        LocalDateTime submittedAt) {
}
