package br.com.fiap.techchallenge.relatorio.infrastructure.persistence;

import br.com.fiap.techchallenge.relatorio.domain.model.Feedback;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de integração do {@link FeedbackJdbcGateway}: os Dev Services do
 * Quarkus sobem um MySQL real via Testcontainers (exige Docker em
 * execução) e o schema é criado por {@code src/test/resources/db/init.sql}.
 */
@QuarkusTest
class FeedbackJdbcGatewayTest {

    @Inject
    DataSource dataSource;

    @Inject
    FeedbackJdbcGateway gateway;

    private final LocalDateTime agora = LocalDateTime.of(2026, 7, 13, 11, 0);

    @BeforeEach
    void limparTabela() throws SQLException {
        try (Connection con = dataSource.getConnection();
             Statement st = con.createStatement()) {
            st.executeUpdate("DELETE FROM feedback");
        }
    }

    private void inserir(String description, int rating, LocalDateTime createdAt) throws SQLException {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO feedback (description, rating, createdAt) VALUES (?, ?, ?)")) {
            ps.setString(1, description);
            ps.setInt(2, rating);
            ps.setObject(3, createdAt);
            ps.executeUpdate();
        }
    }

    @Test
    @DisplayName("Retorna somente as avaliações dentro da janela de 7 dias")
    void filtraPelaJanelaDeSeteDias() throws SQLException {
        inserir("dentro da janela", 8, agora.minusDays(1));
        inserir("no limite inicial (inclusivo)", 5, agora.minusDays(7));
        inserir("fora da janela (8 dias atrás)", 2, agora.minusDays(8));
        inserir("fora da janela (futuro)", 9, agora.plusDays(1));

        List<Feedback> resultado = gateway.buscarPorPeriodo(agora.minusDays(7), agora);

        assertEquals(2, resultado.size());
        assertEquals("no limite inicial (inclusivo)", resultado.get(0).descricao());
        assertEquals("dentro da janela", resultado.get(1).descricao());
    }

    @Test
    @DisplayName("Mapeia corretamente descrição, nota e data de envio")
    void mapeiaColunasCorretamente() throws SQLException {
        LocalDateTime dataEnvio = agora.minusDays(2).withNano(0);
        inserir("produto excelente", 10, dataEnvio);

        List<Feedback> resultado = gateway.buscarPorPeriodo(agora.minusDays(7), agora);

        assertEquals(1, resultado.size());
        Feedback feedback = resultado.get(0);
        assertEquals("produto excelente", feedback.descricao());
        assertEquals(10, feedback.nota());
        assertEquals(dataEnvio, feedback.dataEnvio());
    }

    @Test
    @DisplayName("Retorna lista vazia quando não há avaliações no período")
    void retornaListaVaziaSemAvaliacoes() {
        List<Feedback> resultado = gateway.buscarPorPeriodo(agora.minusDays(7), agora);
        assertTrue(resultado.isEmpty());
    }
}
