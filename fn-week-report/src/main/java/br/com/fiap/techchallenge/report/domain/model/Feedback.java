package br.com.fiap.techchallenge.relatorio.domain.model;

import java.time.LocalDateTime;

/**
 * Entidade de domínio que representa uma avaliação enviada por um usuário.
 *
 * @param descricao texto livre enviado pelo usuário
 * @param nota      nota atribuída (0 a 10)
 * @param dataEnvio momento em que o feedback foi registrado (UTC)
 */
public record Feedback(
        String descricao,
        int nota,
        LocalDateTime dataEnvio) {
}
