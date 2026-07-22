package br.com.fiap.techchallenge.relatorio.infrastructure.storage;

import br.com.fiap.techchallenge.relatorio.application.gateway.RelatorioPublicadorGateway;
import br.com.fiap.techchallenge.relatorio.domain.exception.RelatorioException;
import br.com.fiap.techchallenge.relatorio.domain.model.RelatorioSemanal;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Adapter de publicação da porta {@link RelatorioPublicadorGateway}:
 * serializa o relatório em JSON e o envia ao Azure Blob Storage usando o
 * SDK oficial ({@code azure-storage-blob}), sem extensão Quarkiverse adicional.
 *
 * <p>O cliente é criado de forma tardia (apenas no momento do upload),
 * para que a aplicação suba e os testes rodem sem credenciais reais.</p>
 */
@ApplicationScoped
public class BlobStorageRelatorioGateway implements RelatorioPublicadorGateway {

    private final String connectionString;
    private final String containerName;
    private final ObjectMapper objectMapper;

    @Inject
    public BlobStorageRelatorioGateway(
            @ConfigProperty(name = "azure.storage.connection-string") String connectionString,
            @ConfigProperty(name = "azure.storage.container-name", defaultValue = "relatorios") String containerName,
            ObjectMapper objectMapper) {
        this.connectionString = connectionString;
        this.containerName = containerName;
        this.objectMapper = objectMapper;
    }

    /**
     * Serializa o relatório como {@code relatorio-semanal-{data}.json} e faz
     * upload para o container configurado, criando o container caso não
     * exista e sobrescrevendo o blob se já houver um com o mesmo nome.
     *
     * @return o nome do arquivo publicado
     * @throws RelatorioException se houver falha de serialização, autenticação ou upload
     */
    @Override
    public String publicar(RelatorioSemanal relatorio) {
        String nomeArquivo = nomeArquivo(relatorio);
        String conteudoJson = serializar(relatorio);
        try {
            BlobContainerClient container = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient()
                    .getBlobContainerClient(containerName);
            container.createIfNotExists();

            BlobClient blob = container.getBlobClient(nomeArquivo);
            blob.upload(BinaryData.fromString(conteudoJson), true);
            blob.setHttpHeaders(new BlobHttpHeaders().setContentType("application/json"));
            return nomeArquivo;
        } catch (RuntimeException e) {
            throw new RelatorioException(
                    "Falha ao publicar o relatório '" + nomeArquivo + "' no Azure Blob Storage: " + e.getMessage(), e);
        }
    }

    /** Nome do blob publicado: {@code relatorio-semanal-{periodoFim}.json}. */
    static String nomeArquivo(RelatorioSemanal relatorio) {
        return "relatorio-semanal-" + relatorio.periodoFim() + ".json";
    }

    String serializar(RelatorioSemanal relatorio) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(relatorio);
        } catch (JsonProcessingException e) {
            throw new RelatorioException("Falha ao serializar o relatório semanal em JSON", e);
        }
    }
}
