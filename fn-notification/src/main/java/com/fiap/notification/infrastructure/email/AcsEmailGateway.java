package com.fiap.notification.infrastructure.email;

import com.fiap.notification.gateway.AlertMessage;
import com.fiap.notification.gateway.EmailGateway;
import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailAddress;
import com.azure.communication.email.models.EmailMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
public class AcsEmailGateway implements EmailGateway {

    private static final Logger LOG = Logger.getLogger(AcsEmailGateway.class);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String connectionString;
    private final String sender;
    private final List<String> administrators;

    @Inject
    public AcsEmailGateway(
            @ConfigProperty(name = "acs.email.connection-string") String connectionString,
            @ConfigProperty(name = "acs.email.sender") String sender,
            @ConfigProperty(name = "acs.email.administrators") List<String> administrators) {
        this.connectionString = connectionString;
        this.sender = sender;
        this.administrators = administrators;
    }

    @Override
    public void send(AlertMessage message) {
        EmailClient client = new EmailClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        EmailMessage email = new EmailMessage()
                .setSenderAddress(sender)
                .setToRecipients(administrators.stream().map(EmailAddress::new).toList())
                .setSubject("[CRITICAL Feedback] Urgency " + message.urgency())
                .setBodyPlainText(buildBody(message));

        client.beginSend(email, null).waitForCompletion();
        LOG.infof("Alert email sent to %d administrator(s)", administrators.size());
    }

    private String buildBody(AlertMessage message) {
        return """
                A critical feedback was registered on the platform.

                Urgency:     %s
                Sent at:     %s
                Description: %s
                """.formatted(
                message.urgency(),
                message.createdAt().format(DATE_FORMAT),
                message.description());
    }
}
