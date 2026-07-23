package br.com.fiap.techchallenge.relatorio.application.usecase;

import br.com.fiap.techchallenge.relatorio.application.gateway.FeedbackGateway;
import br.com.fiap.techchallenge.relatorio.application.gateway.RelatorioPublicadorGateway;
import br.com.fiap.techchallenge.relatorio.domain.model.Feedback;
import br.com.fiap.techchallenge.relatorio.domain.model.RelatorioSemanal;
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

/**
 * Testes unitários do {@link GerarRelatorioSemanalUseCase}, sem runtime do
 * Azure Functions, sem banco e sem credenciais: as portas de saída
 * (persistência e publicação) são mockadas.
 */
class GerarRelatorioSemanalUseCaseTest {

    private static final Instant AGORA = Instant.parse("2026-07-13T11:00:00Z");
    private static final LocalDateTime FIM = LocalDateTime.ofInstant(AGORA, ZoneOffset.UTC);

    private FeedbackGateway feedbackGateway;
    private RelatorioPublicadorGateway publicadorGateway;
    private GerarRelatorioSemanalUseCase useCase;

    @BeforeEach
    void setUp() {
        feedbackGateway = mock(FeedbackGateway.class);
        publicadorGateway = mock(RelatorioPublicadorGateway.class);
        useCase = new GerarRelatorioSemanalUseCase(feedbackGateway, publicadorGateway,
                Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private Feedback feedback(int nota, LocalDateTime dataEnvio) {
        return new Feedback("descricao " + nota, nota, dataEnvio);
    }

    @Test
    @DisplayName("Calcula a média geral das notas do período, arredondada a 2 casas")
    void calculaMediaDasNotas() {
        when(feedbackGateway.buscarPorPeriodo(any(), any())).thenReturn(List.of(
                feedback(10, FIM.minusDays(1)),
                feedback(7, FIM.minusDays(2)),
                feedback(3, FIM.minusDays(3))));

        RelatorioSemanal relatorio = useCase.gerarRelatorio();

        assertEquals(6.67, relatorio.mediaGeralNotas());
        assertEquals(3, relatorio.totalAvaliacoes());
    }

    @Test
    @DisplayName("Agrupa a quantidade de avaliações por dia de envio")
    void agrupaQuantidadePorDia() {
        LocalDateTime ontem = FIM.minusDays(1);
        LocalDateTime anteontem = FIM.minusDays(2);
        when(feedbackGateway.buscarPorPeriodo(any(), any())).thenReturn(List.of(
                feedback(8, ontem.withHour(9)),
                feedback(6, ontem.withHour(15)),
                feedback(4, anteontem.withHour(10))));

        RelatorioSemanal relatorio = useCase.gerarRelatorio();

        assertEquals(
                Map.of(ontem.toLocalDate(), 2L, anteontem.toLocalDate(), 1L),
                relatorio.quantidadePorDia());
    }

    @Test
    @DisplayName("Sem feedback no período: gera relatório com contagens zeradas e média 0.0, sem exceção")
    void geraRelatorioVazioQuandoNaoHaFeedback() {
        when(feedbackGateway.buscarPorPeriodo(any(), any())).thenReturn(List.of());

        RelatorioSemanal relatorio = useCase.gerarRelatorio();

        assertEquals(0.0, relatorio.mediaGeralNotas());
        assertEquals(0, relatorio.totalAvaliacoes());
        assertTrue(relatorio.quantidadePorDia().isEmpty());
        assertTrue(relatorio.avaliacoes().isEmpty());
    }

    @Test
    @DisplayName("Mesmo sem feedback, o relatório é entregue à porta de publicação")
    void publicaRelatorioMesmoSemFeedback() {
        when(feedbackGateway.buscarPorPeriodo(any(), any())).thenReturn(List.of());
        when(publicadorGateway.publicar(any())).thenReturn("relatorio-semanal-2026-07-13.json");

        String nomeArquivo = useCase.executar();

        assertEquals("relatorio-semanal-2026-07-13.json", nomeArquivo);
        verify(publicadorGateway).publicar(any(RelatorioSemanal.class));
    }

    @Test
    @DisplayName("Consulta a janela dos últimos 7 dias e publica o relatório consolidado")
    void publicaRelatorioComJanelaDeSeteDias() {
        when(feedbackGateway.buscarPorPeriodo(eq(FIM.minusDays(7)), eq(FIM))).thenReturn(List.of(
                feedback(10, FIM.minusDays(1))));

        useCase.executar();

        ArgumentCaptor<RelatorioSemanal> captor = ArgumentCaptor.forClass(RelatorioSemanal.class);
        verify(publicadorGateway).publicar(captor.capture());
        RelatorioSemanal relatorio = captor.getValue();
        assertEquals(LocalDate.of(2026, 7, 6), relatorio.periodoInicio());
        assertEquals(LocalDate.of(2026, 7, 13), relatorio.periodoFim());
        assertEquals(10.0, relatorio.mediaGeralNotas());
        assertEquals(1, relatorio.avaliacoes().size());
        assertEquals("descricao 10", relatorio.avaliacoes().get(0).descricao());
    }

    @Test
    @DisplayName("Média com lista vazia é 0.0 (método de cálculo isolado)")
    void mediaListaVazia() {
        assertEquals(0.0, GerarRelatorioSemanalUseCase.calcularMediaNotas(List.of()));
        assertEquals(LocalDate.now(Clock.fixed(AGORA, ZoneOffset.UTC)), FIM.toLocalDate());
    }
}
