package br.com.fiap.techchallenge.report.infrastructure.storage;

import br.com.fiap.techchallenge.report.domain.model.FeedbackSummary;
import br.com.fiap.techchallenge.report.domain.model.WeeklyReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlobStorageReportGatewayTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final BlobStorageReportGateway gateway =
            new BlobStorageReportGateway("UseDevelopmentStorage=true", "reports", mapper);

    private WeeklyReport report() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 7, 12, 9, 30);
        return new WeeklyReport(
                "Relatório semanal de avaliações — período de 2026-07-06 a 2026-07-13",
                LocalDateTime.of(2026, 7, 13, 11, 0),
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 13),
                10.0,
                1,
                Map.of(submittedAt.toLocalDate(), 1L),
                List.of(new FeedbackSummary("descricao 10", submittedAt)));
    }

    @Test
    @DisplayName("Nome do arquivo publicado é relatorio-semanal-{periodoFim}.json")
    void nomeDoArquivoUsaPeriodoFim() {
        assertEquals("relatorio-semanal-2026-07-13.json",
                BlobStorageReportGateway.fileName(report()));
    }

    @Test
    @DisplayName("Serializa o JSON com os campos do contrato do relatório")
    void serializaJsonComCamposDoContrato() {
        String content = gateway.serialize(report());

        assertTrue(content.contains("\"mediaGeralNotas\""));
        assertTrue(content.contains("\"quantidadePorDia\""));
        assertTrue(content.contains("\"dataEnvio\""));
        assertTrue(content.contains("\"descricao\" : \"descricao 10\"")
                || content.contains("\"descricao\":\"descricao 10\""));
    }
}
