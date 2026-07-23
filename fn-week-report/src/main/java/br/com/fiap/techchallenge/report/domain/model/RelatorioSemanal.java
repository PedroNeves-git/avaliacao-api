package br.com.fiap.techchallenge.relatorio.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Entidade de domínio do relatório semanal consolidado de avaliações.
 *
 * @param descricao             descrição do relatório
 * @param dataGeracao           momento (UTC) em que o relatório foi gerado
 * @param periodoInicio         início da janela de 7 dias
 * @param periodoFim            fim da janela (data da execução)
 * @param mediaGeralNotas       média das notas no período ({@code 0.0} se não houver avaliações)
 * @param totalAvaliacoes       total de avaliações no período
 * @param quantidadePorDia      quantidade de avaliações agrupadas por dia de envio
 * @param avaliacoes            avaliações do período (descrição e data de envio)
 */
public record RelatorioSemanal(
        String descricao,
        LocalDateTime dataGeracao,
        LocalDate periodoInicio,
        LocalDate periodoFim,
        double mediaGeralNotas,
        long totalAvaliacoes,
        Map<LocalDate, Long> quantidadePorDia,
        List<AvaliacaoResumo> avaliacoes) {
}
