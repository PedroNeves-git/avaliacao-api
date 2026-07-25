package br.com.fiap.techchallenge.report.application.gateway;

import br.com.fiap.techchallenge.report.domain.exception.ReportException;
import br.com.fiap.techchallenge.report.domain.model.WeeklyReport;

public interface ReportPublisherGateway {

    // retorna o nome do arquivo gerado
    String publish(WeeklyReport report);
}
