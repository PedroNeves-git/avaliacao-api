package com.fiap.notification.gateway;

public interface EmailGateway {

    void send(AlertMessage message);
}
