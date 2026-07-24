package br.com.fiap.techchallenge.relatorio.domain.model;

import java.time.LocalDateTime;

/**
 * Item de avaliação incluído no corpo do relatório semanal, com os campos
 * exigidos pelo contrato do relatório: descrição e data de envio.
 */
public record AvaliacaoResumo(
        String descricao,
        LocalDateTime dataEnvio) {

    public static AvaliacaoResumo de(Feedback feedback) {
        return new AvaliacaoResumo(feedback.descricao(), feedback.dataEnvio());
    }
}
