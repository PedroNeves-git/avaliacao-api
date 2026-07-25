package br.com.fiap.techchallenge.report.application.gateway;

import br.com.fiap.techchallenge.report.domain.exception.ReportException;
import br.com.fiap.techchallenge.report.domain.model.Feedback;

import java.time.LocalDateTime;
import java.util.List;

public interface FeedbackGateway {

    // início inclusivo, fim exclusivo
    List<Feedback> findByPeriod(LocalDateTime start, LocalDateTime end);
}
