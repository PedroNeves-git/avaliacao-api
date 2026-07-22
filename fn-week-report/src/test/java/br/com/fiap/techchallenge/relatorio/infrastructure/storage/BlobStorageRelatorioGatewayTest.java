package br.com.fiap.techchallenge.relatorio.infrastructure.storage;

import br.com.fiap.techchallenge.relatorio.domain.model.AvaliacaoResumo;
import br.com.fiap.techchallenge.relatorio.domain.model.RelatorioSemanal;
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

/**
 * Testes unitários do adapter {@link BlobStorageRelatorioGateway}: nome do
 * arquivo e contrato do JSON serializado, sem tocar o Azure (o cliente de
 * Blob Storage só é criado no momento do upload).
 */
class BlobStorageRelatorioGatewayTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final BlobStorageRelatorioGateway gateway =
            new BlobStorageRelatorioGateway("UseDevelopmentStorage=true", "relatorios", mapper);

    private RelatorioSemanal relatorio() {
        LocalDateTime dataEnvio = LocalDateTime.of(2026, 7, 12, 9, 30);
        return new RelatorioSemanal(
                "Relatório semanal de avaliações — período de 2026-07-06 a 2026-07-13",
                LocalDateTime.of(2026, 7, 13, 11, 0),
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 13),
                10.0,
                1,
                Map.of(dataEnvio.toLocalDate(), 1L),
                Map.of("CRITICA", 1L),
                List.of(new AvaliacaoResumo("descricao 10", "CRITICA", dataEnvio)));
    }

    @Test
    @DisplayName("Nome do arquivo publicado é relatorio-semanal-{periodoFim}.json")
    void nomeDoArquivoUsaPeriodoFim() {
        assertEquals("relatorio-semanal-2026-07-13.json",
                BlobStorageRelatorioGateway.nomeArquivo(relatorio()));
    }

    @Test
    @DisplayName("Serializa o JSON com os campos do contrato do relatório")
    void serializaJsonComCamposDoContrato() {
        String conteudo = gateway.serializar(relatorio());

        assertTrue(conteudo.contains("\"mediaGeralNotas\""));
        assertTrue(conteudo.contains("\"quantidadePorDia\""));
        assertTrue(conteudo.contains("\"quantidadePorUrgencia\""));
        assertTrue(conteudo.contains("\"dataEnvio\""));
        assertTrue(conteudo.contains("\"urgencia\" : \"CRITICA\"")
                || conteudo.contains("\"urgencia\":\"CRITICA\""));
    }
}
