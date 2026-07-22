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
 * (somente SELECT) à tabela {@code avaliacoes}, via JDBC puro.
 *
 * <p>O nome da tabela e das colunas segue o script {@code db/schema.sql} da raiz
 * do repositório (tabela {@code avaliacoes}: {@code id}, {@code descricao},
 * {@code nota}, {@code urgencia}, {@code data_envio}).</p>
 */
@ApplicationScoped
public class FeedbackJdbcGateway implements FeedbackGateway {

    private static final String SQL_BUSCAR_POR_PERIODO = """
            SELECT descricao, nota, urgencia, data_envio
              FROM avaliacoes
             WHERE data_envio >= ? AND data_envio < ?
             ORDER BY data_envio
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
                            rs.getString("descricao"),
                            rs.getInt("nota"),
                            rs.getString("urgencia"),
                            rs.getObject("data_envio", LocalDateTime.class)));
                }
            }
            return feedbacks;
        } catch (SQLException e) {
            throw new RelatorioException(
                    "Falha ao consultar as avaliações no banco de dados: " + e.getMessage(), e);
        }
    }
}
