package br.com.fiap.techchallenge.report.infrastructure.persistence;

import br.com.fiap.techchallenge.report.domain.model.Feedback;
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

// exige Docker: Dev Services do Quarkus sobem um MySQL real via Testcontainers
@QuarkusTest
class FeedbackJdbcGatewayTest {

    @Inject
    DataSource dataSource;

    @Inject
    FeedbackJdbcGateway gateway;

    private final LocalDateTime now = LocalDateTime.of(2026, 7, 13, 11, 0);

    @BeforeEach
    void clearTable() throws SQLException {
        try (Connection con = dataSource.getConnection();
             Statement st = con.createStatement()) {
            st.executeUpdate("DELETE FROM feedback");
        }
    }

    private void insert(String description, int rating, LocalDateTime createdAt) throws SQLException {
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
        insert("dentro da janela", 8, now.minusDays(1));
        insert("no limite inicial (inclusivo)", 5, now.minusDays(7));
        insert("fora da janela (8 dias atrás)", 2, now.minusDays(8));
        insert("fora da janela (futuro)", 9, now.plusDays(1));

        List<Feedback> result = gateway.findByPeriod(now.minusDays(7), now);

        assertEquals(2, result.size());
        assertEquals("no limite inicial (inclusivo)", result.get(0).description());
        assertEquals("dentro da janela", result.get(1).description());
    }

    @Test
    @DisplayName("Mapeia corretamente descrição, nota e data de envio")
    void mapeiaColunasCorretamente() throws SQLException {
        LocalDateTime submittedAt = now.minusDays(2).withNano(0);
        insert("produto excelente", 10, submittedAt);

        List<Feedback> result = gateway.findByPeriod(now.minusDays(7), now);

        assertEquals(1, result.size());
        Feedback feedback = result.get(0);
        assertEquals("produto excelente", feedback.description());
        assertEquals(10, feedback.rating());
        assertEquals(submittedAt, feedback.submittedAt());
    }

    @Test
    @DisplayName("Retorna lista vazia quando não há avaliações no período")
    void retornaListaVaziaSemAvaliacoes() {
        List<Feedback> result = gateway.findByPeriod(now.minusDays(7), now);
        assertTrue(result.isEmpty());
    }
}
