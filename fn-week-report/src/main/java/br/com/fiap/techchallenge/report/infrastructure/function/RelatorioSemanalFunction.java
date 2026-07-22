package br.com.fiap.techchallenge.relatorio.infrastructure.function;

import br.com.fiap.techchallenge.relatorio.application.usecase.GerarRelatorioSemanalUseCase;
import br.com.fiap.techchallenge.relatorio.domain.exception.RelatorioException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import jakarta.inject.Inject;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entrypoint (driving adapter) da função serverless de relatório semanal.
 *
 * <p>Disparada por Timer Trigger toda segunda-feira às 08:00 (horário de
 * Brasília). As expressões NCRONTAB do Azure Functions rodam em UTC e o
 * Brasil é UTC-3 o ano todo, portanto o agendamento é {@code 0 0 11 * * MON}
 * (11:00 UTC). Não há endpoint HTTP.</p>
 *
 * <p>O entrypoint é fino: apenas recebe o trigger, delega ao
 * {@link GerarRelatorioSemanalUseCase} e loga o resultado. Falhas são
 * logadas e propagadas para que a execução seja marcada como falha no Azure.</p>
 */
public class RelatorioSemanalFunction {

    @Inject
    GerarRelatorioSemanalUseCase gerarRelatorioSemanalUseCase;

    @FunctionName("relatorioSemanal")
    public void run(
            @TimerTrigger(name = "timerInfo", schedule = "0 0 11 * * MON") String timerInfo,
            final ExecutionContext context) {

        Logger logger = context.getLogger();
        logger.info("Iniciando a geração do relatório semanal de avaliações.");
        try {
            String nomeArquivo = gerarRelatorioSemanalUseCase.executar();
            logger.info("Relatório semanal publicado com sucesso no Blob Storage: " + nomeArquivo);
        } catch (RelatorioException e) {
            logger.log(Level.SEVERE, "Falha ao gerar o relatório semanal: " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro inesperado ao gerar o relatório semanal", e);
            throw new RelatorioException("Erro inesperado ao gerar o relatório semanal", e);
        }
    }
}
