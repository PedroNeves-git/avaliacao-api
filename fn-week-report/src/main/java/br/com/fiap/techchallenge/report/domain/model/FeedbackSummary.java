package br.com.fiap.techchallenge.report.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

// @JsonProperty mantém as chaves originais do JSON já publicado
public record FeedbackSummary(
        @JsonProperty("descricao") String description,
        @JsonProperty("dataEnvio") LocalDateTime submittedAt) {

    public static FeedbackSummary from(Feedback feedback) {
        return new FeedbackSummary(feedback.description(), feedback.submittedAt());
    }
}
