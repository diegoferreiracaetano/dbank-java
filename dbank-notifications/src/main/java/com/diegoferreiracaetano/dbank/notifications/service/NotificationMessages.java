package com.diegoferreiracaetano.dbank.notifications.service;

final class NotificationMessages {

    private NotificationMessages() {}

    static String of(TransferResultEvent event) {
        return switch (event.outcome()) {
            case SETTLED -> "Your transfer " + event.transferId() + " was completed.";
            case REJECTED -> "Your transfer " + event.transferId() + " was declined: " + describe(event.reason()) + ".";
        };
    }

    private static String describe(String reason) {
        return switch (reason == null ? "" : reason) {
            case "INSUFFICIENT_FUNDS" -> "insufficient funds";
            case "ACCOUNT_NOT_FOUND" -> "account not found";
            default -> "it could not be processed";
        };
    }
}
