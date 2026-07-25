package br.com.fiap.techchallenge.report.infrastructure.storage;

import br.com.fiap.techchallenge.report.application.gateway.ReportPublisherGateway;
import br.com.fiap.techchallenge.report.domain.exception.ReportException;
import br.com.fiap.techchallenge.report.domain.model.WeeklyReport;
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

// cliente do Blob só é criado no upload, para os testes rodarem sem credenciais reais
@ApplicationScoped
public class BlobStorageReportGateway implements ReportPublisherGateway {

    private final String connectionString;
    private final String containerName;
    private final ObjectMapper objectMapper;

    @Inject
    public BlobStorageReportGateway(
            @ConfigProperty(name = "azure.storage.connection-string") String connectionString,
            @ConfigProperty(name = "azure.storage.container-name", defaultValue = "reports") String containerName,
            ObjectMapper objectMapper) {
        this.connectionString = connectionString;
        this.containerName = containerName;
        this.objectMapper = objectMapper;
    }

    @Override
    public String publish(WeeklyReport report) {
        String fileName = fileName(report);
        String jsonContent = serialize(report);
        try {
            BlobContainerClient container = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient()
                    .getBlobContainerClient(containerName);
            container.createIfNotExists();

            BlobClient blob = container.getBlobClient(fileName);
            blob.upload(BinaryData.fromString(jsonContent), true);
            blob.setHttpHeaders(new BlobHttpHeaders().setContentType("application/json"));
            return fileName;
        } catch (RuntimeException e) {
            throw new ReportException(
                    "Falha ao publicar o relatório '" + fileName + "' no Azure Blob Storage: " + e.getMessage(), e);
        }
    }

    static String fileName(WeeklyReport report) {
        return "relatorio-semanal-" + report.periodEnd() + ".json";
    }

    String serialize(WeeklyReport report) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
        } catch (JsonProcessingException e) {
            throw new ReportException("Falha ao serializar o relatório semanal em JSON", e);
        }
    }
}
