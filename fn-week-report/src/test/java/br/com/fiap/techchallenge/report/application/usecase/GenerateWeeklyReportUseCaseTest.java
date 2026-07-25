package br.com.fiap.techchallenge.report.application.usecase;

import br.com.fiap.techchallenge.report.application.gateway.FeedbackGateway;
import br.com.fiap.techchallenge.report.application.gateway.ReportPublisherGateway;
import br.com.fiap.techchallenge.report.domain.model.Feedback;
import br.com.fiap.techchallenge.report.domain.model.WeeklyReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenerateWeeklyReportUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-07-13T11:00:00Z");
    private static final LocalDateTime END = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);

    private FeedbackGateway feedbackGateway;
    private ReportPublisherGateway publisherGateway;
    private GenerateWeeklyReportUseCase useCase;

    @BeforeEach
    void setUp() {
        feedbackGateway = mock(FeedbackGateway.class);
        publisherGateway = mock(ReportPublisherGateway.class);
        useCase = new GenerateWeeklyReportUseCase(feedbackGateway, publisherGateway,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private Feedback feedback(int rating, LocalDateTime submittedAt) {
        return new Feedback("description " + rating, rating, submittedAt);
    }

    @Test
    @DisplayName("Calcula a média geral das notas do período, arredondada a 2 casas")
    void calculaMediaDasNotas() {
        when(feedbackGateway.findByPeriod(any(), any())).thenReturn(List.of(
                feedback(10, END.minusDays(1)),
                feedback(7, END.minusDays(2)),
                feedback(3, END.minusDays(3))));

        WeeklyReport report = useCase.generateReport();

        assertEquals(6.67, report.averageRating());
        assertEquals(3, report.totalEvaluations());
    }

    @Test
    @DisplayName("Agrupa a quantidade de avaliações por dia de envio")
    void agrupaQuantidadePorDia() {
        LocalDateTime yesterday = END.minusDays(1);
        LocalDateTime twoDaysAgo = END.minusDays(2);
        when(feedbackGateway.findByPeriod(any(), any())).thenReturn(List.of(
                feedback(8, yesterday.withHour(9)),
                feedback(6, yesterday.withHour(15)),
                feedback(4, twoDaysAgo.withHour(10))));

        WeeklyReport report = useCase.generateReport();

        assertEquals(
                Map.of(yesterday.toLocalDate(), 2L, twoDaysAgo.toLocalDate(), 1L),
                report.countByDay());
    }

    @Test
    @DisplayName("Sem feedback no período: gera relatório com contagens zeradas e média 0.0, sem exceção")
    void geraRelatorioVazioQuandoNaoHaFeedback() {
        when(feedbackGateway.findByPeriod(any(), any())).thenReturn(List.of());

        WeeklyReport report = useCase.generateReport();

        assertEquals(0.0, report.averageRating());
        assertEquals(0, report.totalEvaluations());
        assertTrue(report.countByDay().isEmpty());
        assertTrue(report.evaluations().isEmpty());
    }

    @Test
    @DisplayName("Mesmo sem feedback, o relatório é entregue à porta de publicação")
    void publicaRelatorioMesmoSemFeedback() {
        when(feedbackGateway.findByPeriod(any(), any())).thenReturn(List.of());
        when(publisherGateway.publish(any())).thenReturn("relatorio-semanal-2026-07-13.json");

        String fileName = useCase.execute();

        assertEquals("relatorio-semanal-2026-07-13.json", fileName);
        verify(publisherGateway).publish(any(WeeklyReport.class));
    }

    @Test
    @DisplayName("Consulta a janela dos últimos 7 dias e publica o relatório consolidado")
    void publicaRelatorioComJanelaDeSeteDias() {
        when(feedbackGateway.findByPeriod(eq(END.minusDays(7)), eq(END))).thenReturn(List.of(
                feedback(10, END.minusDays(1))));

        useCase.execute();

        ArgumentCaptor<WeeklyReport> captor = ArgumentCaptor.forClass(WeeklyReport.class);
        verify(publisherGateway).publish(captor.capture());
        WeeklyReport report = captor.getValue();
        assertEquals(LocalDate.of(2026, 7, 6), report.periodStart());
        assertEquals(LocalDate.of(2026, 7, 13), report.periodEnd());
        assertEquals(10.0, report.averageRating());
        assertEquals(1, report.evaluations().size());
        assertEquals("description 10", report.evaluations().get(0).description());
    }

    @Test
    @DisplayName("Média com lista vazia é 0.0 (método de cálculo isolado)")
    void mediaListaVazia() {
        assertEquals(0.0, GenerateWeeklyReportUseCase.calculateAverageRating(List.of()));
        assertEquals(LocalDate.now(Clock.fixed(NOW, ZoneOffset.UTC)), END.toLocalDate());
    }
}
