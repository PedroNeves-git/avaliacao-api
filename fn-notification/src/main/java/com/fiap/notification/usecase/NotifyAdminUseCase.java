package com.fiap.notification.usecase;

import com.fiap.notification.domain.Feedback;
import com.fiap.notification.gateway.AlertMessage;
import com.fiap.notification.gateway.EmailGateway;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NotifyAdminUseCase {

    private static final Logger LOG = Logger.getLogger(NotifyAdminUseCase.class);

    private final EmailGateway emailGateway;

    public NotifyAdminUseCase(EmailGateway emailGateway) {
        this.emailGateway = emailGateway;
    }

    public void notify(Feedback feedback) {
        if (!feedback.isCritical()) {
            LOG.infof("Feedback %s is %s; notification skipped", feedback.id(), feedback.urgency());
            return;
        }
        LOG.infof("Critical feedback %s detected; notifying administrators", feedback.id());
        emailGateway.send(AlertMessage.from(feedback));
        LOG.infof("Notification sent for feedback %s", feedback.id());
    }
}
