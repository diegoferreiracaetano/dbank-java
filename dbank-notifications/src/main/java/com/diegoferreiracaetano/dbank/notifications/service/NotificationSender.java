package com.diegoferreiracaetano.dbank.notifications.service;

import java.util.UUID;

public interface NotificationSender {

    void send(UUID accountId, String message);
}
