package com.diegoferreiracaetano.dbank.payments.config;

import com.diegoferreiracaetano.dbank.payments.domain.transfer.InvalidTransferTransitionException;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferNotFoundException;
import java.time.Duration;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration(proxyBeanMethods = false)
class KafkaErrorHandlingConfiguration {

    @Bean
    DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, String> template,
            @Value("${dbank.kafka.retry.initial-interval}") Duration initialInterval,
            @Value("${dbank.kafka.retry.multiplier}") double multiplier,
            @Value("${dbank.kafka.retry.max-interval}") Duration maxInterval,
            @Value("${dbank.kafka.retry.max-elapsed-time}") Duration maxElapsedTime) {
        var recoverer = new DeadLetterPublishingRecoverer(
                template,
                (record, exception) -> new TopicPartition(record.topic() + ".payments.dlt", record.partition()));
        var backOff = new ExponentialBackOff(initialInterval.toMillis(), multiplier);
        backOff.setMaxInterval(maxInterval.toMillis());
        backOff.setMaxElapsedTime(maxElapsedTime.toMillis());
        var handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(InvalidTransferTransitionException.class, TransferNotFoundException.class);
        return handler;
    }
}
