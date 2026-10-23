package com.diegoferreiracaetano.dbank.notifications.service;

import com.diegoferreiracaetano.dbank.notifications.persistence.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final NotificationSender sender;

    NotificationService(NotificationRepository notifications, NotificationSender sender) {
        this.notifications = notifications;
        this.sender = sender;
    }

    @Transactional
    public void handle(TransferResultEvent event) {
        var message = NotificationMessages.of(event);
        var inserted = notifications.insertIfAbsent(event.eventId(), event.transferId(), event.accountId(), message);
        if (inserted == 0) {
            return;
        }
        sender.send(event.accountId(), message);
    }
}
