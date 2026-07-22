package br.com.fiap.techchallenge.relatorio.application.usecase;

import br.com.fiap.techchallenge.relatorio.application.gateway.FeedbackGateway;
import br.com.fiap.techchallenge.relatorio.application.gateway.RelatorioPublicadorGateway;
import br.com.fiap.techchallenge.relatorio.domain.exception.RelatorioException;
import br.com.fiap.techchallenge.relatorio.domain.model.AvaliacaoResumo;
import br.com.fiap.techchallenge.relatorio.domain.model.Feedback;
import br.com.fiap.techchallenge.relatorio.domain.model.RelatorioSemanal;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Caso de uso do relatório semanal: consulta as avaliações dos últimos
 * 7 dias, calcula a média geral das notas e as contagens por dia e por
 * urgência, e entrega o relatório consolidado à porta de publicação.
 *
 * <p>Depende apenas das portas ({@link FeedbackGateway} e
 * {@link RelatorioPublicadorGateway}), sem conhecer banco de dados, JSON ou
 * Azure. O relógio é injetável para permitir testes determinísticos.</p>
 */
@ApplicationScoped
public class GerarRelatorioSemanalUseCase {

    static final int DIAS_JANELA = 7;

    private final FeedbackGateway feedbackGateway;
    private final RelatorioPublicadorGateway relatorioPublicadorGateway;
    private final Clock clock;

    @Inject
    public GerarRelatorioSemanalUseCase(FeedbackGateway feedbackGateway,
                                        RelatorioPublicadorGateway relatorioPublicadorGateway) {
        this(feedbackGateway, relatorioPublicadorGateway, Clock.systemUTC());
    }

    GerarRelatorioSemanalUseCase(FeedbackGateway feedbackGateway,
                                 RelatorioPublicadorGateway relatorioPublicadorGateway,
                                 Clock clock) {
        this.feedbackGateway = feedbackGateway;
        this.relatorioPublicadorGateway = relatorioPublicadorGateway;
        this.clock = clock;
    }

    /**
     * Gera o relatório da última semana e o publica pela porta de saída.
     *
     * @return o nome do arquivo publicado
     * @throws RelatorioException em falha de banco, serialização ou upload
     */
    public String executar() {
        return relatorioPublicadorGateway.publicar(gerarRelatorio());
    }

    /**
     * Monta o relatório consolidado dos últimos {@value DIAS_JANELA} dias.
     * Se não houver nenhuma avaliação no período, o relatório é gerado com
     * contagens zeradas e média {@code 0.0}.
     */
    public RelatorioSemanal gerarRelatorio() {
        LocalDateTime fim = LocalDateTime.now(clock);
        LocalDateTime inicio = fim.minusDays(DIAS_JANELA);

        List<Feedback> feedbacks = feedbackGateway.buscarPorPeriodo(inicio, fim);

        return new RelatorioSemanal(
                "Relatório semanal de avaliações — período de %s a %s"
                        .formatted(inicio.toLocalDate(), fim.toLocalDate()),
                fim,
                inicio.toLocalDate(),
                fim.toLocalDate(),
                calcularMediaNotas(feedbacks),
                feedbacks.size(),
                contarPorDia(feedbacks),
                contarPorUrgencia(feedbacks),
                feedbacks.stream().map(AvaliacaoResumo::de).toList());
    }

    /** Média geral das notas, arredondada a 2 casas; {@code 0.0} para lista vazia. */
    static double calcularMediaNotas(List<Feedback> feedbacks) {
        if (feedbacks.isEmpty()) {
            return 0.0;
        }
        double media = feedbacks.stream()
                .mapToInt(Feedback::nota)
                .average()
                .orElse(0.0);
        return BigDecimal.valueOf(media).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** Quantidade de avaliações agrupadas pela data de envio, em ordem cronológica. */
    static Map<LocalDate, Long> contarPorDia(List<Feedback> feedbacks) {
        return feedbacks.stream().collect(Collectors.groupingBy(
                feedback -> feedback.dataEnvio().toLocalDate(),
                TreeMap::new,
                Collectors.counting()));
    }

    /** Quantidade de avaliações agrupadas pelo nível de urgência. */
    static Map<String, Long> contarPorUrgencia(List<Feedback> feedbacks) {
        return feedbacks.stream().collect(Collectors.groupingBy(
                Feedback::urgencia,
                TreeMap::new,
                Collectors.counting()));
    }
}
