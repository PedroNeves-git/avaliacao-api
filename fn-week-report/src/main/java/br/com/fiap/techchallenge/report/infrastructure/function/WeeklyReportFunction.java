package br.com.fiap.techchallenge.report.infrastructure.function;

import br.com.fiap.techchallenge.report.application.usecase.GenerateWeeklyReportUseCase;
import br.com.fiap.techchallenge.report.domain.exception.ReportException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import jakarta.inject.Inject;

import java.util.logging.Level;
import java.util.logging.Logger;

public class WeeklyReportFunction {

    @Inject
    GenerateWeeklyReportUseCase generateWeeklyReportUseCase;

    // cron roda em UTC; 11:00 UTC = 08:00 em Brasília (UTC-3)
    // nome do gatilho mantido em português: já configurado e testado no Azure
    @FunctionName("relatorioSemanal")
    public void run(
            @TimerTrigger(name = "timerInfo", schedule = "0 0 11 * * MON") String timerInfo,
            final ExecutionContext context) {

        Logger logger = context.getLogger();
        logger.info("Iniciando a geração do relatório semanal de avaliações.");
        try {
            String fileName = generateWeeklyReportUseCase.execute();
            logger.info("Relatório semanal publicado com sucesso no Blob Storage: " + fileName);
        } catch (ReportException e) {
            logger.log(Level.SEVERE, "Falha ao gerar o relatório semanal: " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro inesperado ao gerar o relatório semanal", e);
            throw new ReportException("Erro inesperado ao gerar o relatório semanal", e);
        }
    }
}
