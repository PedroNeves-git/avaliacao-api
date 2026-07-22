package br.com.fiap.techchallenge.relatorio.application.gateway;

import br.com.fiap.techchallenge.relatorio.domain.exception.RelatorioException;
import br.com.fiap.techchallenge.relatorio.domain.model.Feedback;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Porta de saída para consulta das avaliações persistidas.
 *
 * <p>A camada de aplicação depende apenas desta interface; o acesso real ao
 * banco de dados é responsabilidade do adapter de infraestrutura
 * ({@code FeedbackJdbcGateway}).</p>
 */
public interface FeedbackGateway {

    /**
     * Busca todas as avaliações enviadas dentro da janela informada
     * (início inclusivo, fim exclusivo), em ordem cronológica.
     *
     * @throws RelatorioException se houver falha de conexão ou de consulta ao banco
     */
    List<Feedback> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim);
}
