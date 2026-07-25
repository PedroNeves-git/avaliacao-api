package br.com.fiap.techchallenge.report.infrastructure.persistence;

import br.com.fiap.techchallenge.report.application.gateway.FeedbackGateway;
import br.com.fiap.techchallenge.report.domain.exception.ReportException;
import br.com.fiap.techchallenge.report.domain.model.Feedback;
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

// tabela "feedback" é a mesma alimentada pela func-feedback no banco techchallenge
@ApplicationScoped
public class FeedbackJdbcGateway implements FeedbackGateway {

    private static final String SQL_FIND_BY_PERIOD = """
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
    public List<Feedback> findByPeriod(LocalDateTime start, LocalDateTime end) {
        List<Feedback> feedbacks = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL_FIND_BY_PERIOD)) {

            statement.setObject(1, start);
            statement.setObject(2, end);

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
            throw new ReportException(
                    "Falha ao consultar as avaliações no banco de dados: " + e.getMessage(), e);
        }
    }
}
