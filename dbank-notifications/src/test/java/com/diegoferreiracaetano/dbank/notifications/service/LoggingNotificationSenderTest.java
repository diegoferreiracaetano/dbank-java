package com.diegoferreiracaetano.dbank.notifications.service;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class LoggingNotificationSenderTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(LoggingNotificationSender.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attachAppender() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void givenAMessage_whenSending_thenShouldLogItWithTheAccount() {
        var accountId = UUID.randomUUID();

        new LoggingNotificationSender().send(accountId, "hello");

        assertThat(appender.list).singleElement().satisfies(event -> {
            assertThat(event.getFormattedMessage())
                    .contains(accountId.toString())
                    .contains("hello");
        });
    }
}
