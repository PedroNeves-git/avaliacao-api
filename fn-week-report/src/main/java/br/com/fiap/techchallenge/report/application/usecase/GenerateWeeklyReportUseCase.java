package br.com.fiap.techchallenge.report.application.usecase;

import br.com.fiap.techchallenge.report.application.gateway.FeedbackGateway;
import br.com.fiap.techchallenge.report.application.gateway.ReportPublisherGateway;
import br.com.fiap.techchallenge.report.domain.exception.ReportException;
import br.com.fiap.techchallenge.report.domain.model.Feedback;
import br.com.fiap.techchallenge.report.domain.model.FeedbackSummary;
import br.com.fiap.techchallenge.report.domain.model.WeeklyReport;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class GenerateWeeklyReportUseCase {

    static final int WINDOW_DAYS = 7;

    private final FeedbackGateway feedbackGateway;
    private final ReportPublisherGateway reportPublisherGateway;
    private final Clock clock;

    @Inject
    public GenerateWeeklyReportUseCase(FeedbackGateway feedbackGateway,
                                       ReportPublisherGateway reportPublisherGateway) {
        this(feedbackGateway, reportPublisherGateway, Clock.systemUTC());
    }

    GenerateWeeklyReportUseCase(FeedbackGateway feedbackGateway,
                                ReportPublisherGateway reportPublisherGateway,
                                Clock clock) {
        this.feedbackGateway = feedbackGateway;
        this.reportPublisherGateway = reportPublisherGateway;
        this.clock = clock;
    }

    public String execute() {
        return reportPublisherGateway.publish(generateReport());
    }

    // janela dos últimos WINDOW_DAYS dias; sem avaliações no período, gera relatório com média 0.0
    public WeeklyReport generateReport() {
        LocalDateTime end = LocalDateTime.now(clock);
        LocalDateTime start = end.minusDays(WINDOW_DAYS);

        List<Feedback> feedbacks = feedbackGateway.findByPeriod(start, end);

        return new WeeklyReport(
                "Relatório semanal de avaliações — período de %s a %s"
                        .formatted(start.toLocalDate(), end.toLocalDate()),
                end,
                start.toLocalDate(),
                end.toLocalDate(),
                calculateAverageRating(feedbacks),
                feedbacks.size(),
                countByDay(feedbacks),
                feedbacks.stream().map(FeedbackSummary::from).toList());
    }

    static double calculateAverageRating(List<Feedback> feedbacks) {
        if (feedbacks.isEmpty()) {
            return 0.0;
        }
        double average = feedbacks.stream()
                .mapToInt(Feedback::rating)
                .average()
                .orElse(0.0);
        return BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    static Map<LocalDate, Long> countByDay(List<Feedback> feedbacks) {
        return feedbacks.stream().collect(Collectors.groupingBy(
                feedback -> feedback.submittedAt().toLocalDate(),
                TreeMap::new,
                Collectors.counting()));
    }
}
