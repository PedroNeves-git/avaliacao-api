package br.com.fiap.techchallenge.report.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// @JsonProperty mantém as chaves originais do JSON já publicado no Blob Storage
public record WeeklyReport(
        @JsonProperty("descricao") String description,
        @JsonProperty("dataGeracao") LocalDateTime generatedAt,
        @JsonProperty("periodoInicio") LocalDate periodStart,
        @JsonProperty("periodoFim") LocalDate periodEnd,
        @JsonProperty("mediaGeralNotas") double averageRating,
        @JsonProperty("totalAvaliacoes") long totalEvaluations,
        @JsonProperty("quantidadePorDia") Map<LocalDate, Long> countByDay,
        @JsonProperty("avaliacoes") List<FeedbackSummary> evaluations) {
}
