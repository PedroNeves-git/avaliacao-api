package com.fiap.notification.usecase;

import com.fiap.notification.domain.Feedback;
import com.fiap.notification.domain.Urgency;
import com.fiap.notification.gateway.AlertMessage;
import com.fiap.notification.gateway.EmailGateway;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class NotifyAdminUseCaseTest {

    private final EmailGateway emailGateway = mock(EmailGateway.class);
    private final NotifyAdminUseCase useCase = new NotifyAdminUseCase(emailGateway);

    private static Feedback feedbackWithRating(int rating) {
        return new Feedback(1L, "very bad class", rating, LocalDateTime.now());
    }

    @Test
    void sendsEmailForCriticalFeedback() {
        Feedback feedback = feedbackWithRating(1);

        useCase.notify(feedback);

        ArgumentCaptor<AlertMessage> captor = ArgumentCaptor.forClass(AlertMessage.class);
        verify(emailGateway).send(captor.capture());
        AlertMessage sent = captor.getValue();
        assertEquals(Urgency.CRITICAL, sent.urgency());
        assertEquals(feedback.description(), sent.description());
        assertEquals(feedback.createdAt(), sent.createdAt());
    }

    @Test
    void doesNotSendEmailForModerateFeedback() {
        useCase.notify(feedbackWithRating(5));

        verify(emailGateway, never()).send(any());
    }

    @Test
    void doesNotSendEmailForNormalFeedback() {
        useCase.notify(feedbackWithRating(9));

        verify(emailGateway, never()).send(any());
    }
}
