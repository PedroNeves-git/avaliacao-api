package com.fiap.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fiap.domain.dto.FeedbackRequest;
import com.fiap.domain.entity.Feedback;
import com.fiap.infrastructure.messaging.QueueCriticalRatings;
import com.fiap.infrastructure.repository.FeedbackRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;

@ApplicationScoped
public class ProcessFeedbackUseCase {

    @Inject
    FeedbackRepository repository;

    @Inject

    QueueCriticalRatings queueProducer;

    Logger logger = LoggerFactory.getLogger(ProcessFeedbackUseCase.class);

    @Transactional
    public void execute(FeedbackRequest request) {
        Feedback feedback = new Feedback(request.description(), request.rating());
        repository.persist(feedback);

        if (feedback.getRating() < 6) {
            sendFeedbackToQueue(feedback);
        }
    }

    private void sendFeedbackToQueue(Feedback feedback) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            String feedbackJson = objectMapper.writeValueAsString(feedback);
            queueProducer.send(feedbackJson);
        } catch (Exception e) {
            logger.error("Error sending feedback to queue: {}", e.getMessage());
        }
    }
}