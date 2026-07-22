package br.com.fiap.techchallenge.relatorio.application.gateway;

import br.com.fiap.techchallenge.relatorio.domain.exception.RelatorioException;
import br.com.fiap.techchallenge.relatorio.domain.model.RelatorioSemanal;

/**
 * Porta de saída para publicação do relatório semanal.
 *
 * <p>O caso de uso entrega o relatório como objeto de domínio; a
 * serialização (JSON) e o destino (Azure Blob Storage) são detalhes do
 * adapter de infraestrutura ({@code BlobStorageRelatorioGateway}).</p>
 */
public interface RelatorioPublicadorGateway {

    /**
     * Publica o relatório e retorna o nome do arquivo gerado.
     *
     * @throws RelatorioException em falha de serialização ou de upload
     */
    String publicar(RelatorioSemanal relatorio);
}
