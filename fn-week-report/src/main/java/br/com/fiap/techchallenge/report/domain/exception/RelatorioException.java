package br.com.fiap.techchallenge.relatorio.domain.exception;

/**
 * Exceção de negócio da função de relatório semanal.
 *
 * <p>Lançada quando ocorre falha na consulta ao banco de dados ou na
 * publicação do relatório no Azure Blob Storage. É capturada e logada
 * pelo entrypoint ({@code RelatorioSemanalFunction}) antes de ser propagada
 * ao runtime do Azure Functions, para que a execução seja marcada como
 * falha e possa ser monitorada.</p>
 */
public class RelatorioException extends RuntimeException {

    public RelatorioException(String message) {
        super(message);
    }

    public RelatorioException(String message, Throwable cause) {
        super(message, cause);
    }
}
