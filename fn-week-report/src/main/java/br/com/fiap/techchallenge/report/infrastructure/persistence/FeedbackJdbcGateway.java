package br.com.fiap.techchallenge.relatorio.infrastructure.persistence;

import br.com.fiap.techchallenge.relatorio.application.gateway.FeedbackGateway;
import br.com.fiap.techchallenge.relatorio.domain.exception.RelatorioException;
import br.com.fiap.techchallenge.relatorio.domain.model.Feedback;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter de persistência da porta {@link FeedbackGateway}: acesso de leitura
 * (somente SELECT) à tabela {@code feedback}, via JDBC puro.
 *
 * <p>A tabela {@code feedback} é a mesma alimentada pela função de recebimento
 * de feedback ({@code func-feedback}) no banco {@code techchallenge}:
 * {@code id}, {@code description}, {@code rating}, {@code createdAt}.</p>
 */
@ApplicationScoped
public class FeedbackJdbcGateway implements FeedbackGateway {

    private static final String SQL_BUSCAR_POR_PERIODO = """
            SELECT description, rating, createdAt
              FROM feedback
             WHERE createdAt >= ? AND createdAt < ?
             ORDER BY createdAt
            """;

    private final DataSource dataSource;

    @Inject
    public FeedbackJdbcGateway(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Feedback> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        List<Feedback> feedbacks = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_BUSCAR_POR_PERIODO)) {

            statement.setObject(1, inicio);
            statement.setObject(2, fim);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    feedbacks.add(new Feedback(
                            rs.getString("description"),
                            rs.getInt("rating"),
                            rs.getObject("createdAt", LocalDateTime.class)));
                }
            }
            return feedbacks;
        } catch (SQLException e) {
            throw new RelatorioException(
                    "Falha ao consultar as avaliações no banco de dados: " + e.getMessage(), e);
        }
    }
}
