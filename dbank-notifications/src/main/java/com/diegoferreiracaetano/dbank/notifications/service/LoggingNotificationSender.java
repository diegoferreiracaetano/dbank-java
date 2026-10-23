package com.diegoferreiracaetano.dbank.notifications.service;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class LoggingNotificationSender implements NotificationSender {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void send(UUID accountId, String message) {
        LOG.info("Notification to account {}: {}", accountId, message);
    }
}
