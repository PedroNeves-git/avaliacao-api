package com.fiap.notification.infrastructure.function;

import com.fiap.notification.domain.Feedback;
import com.fiap.notification.usecase.NotifyAdminUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.QueueTrigger;
import jakarta.inject.Inject;

import java.util.logging.Level;

public class CriticalFeedbackFunction {

    @Inject
    NotifyAdminUseCase notifyAdminUseCase;

    @Inject
    ObjectMapper objectMapper;

    @FunctionName("notifyCritical")
    public void run(
            @QueueTrigger(name = "message", queueName = "critical-feedback", connection = "AzureWebJobsStorage")
            String message,
            final ExecutionContext context) {

        context.getLogger().info("Message received from queue critical-feedback");

        Feedback feedback = parse(message, context);
        notifyAdminUseCase.notify(feedback);
    }

    private Feedback parse(String message, ExecutionContext context) {
        try {
            FeedbackEvent event = objectMapper.readValue(message, FeedbackEvent.class);
            return new Feedback(event.id(), event.description(), event.rating(), event.createdAt());
        } catch (Exception e) {
            context.getLogger().log(Level.SEVERE, "Invalid queue payload: " + message, e);
            throw new IllegalArgumentException("Invalid queue payload", e);
        }
    }
}
