package com.diegoferreiracaetano.dbank.payments.config;

import com.diegoferreiracaetano.dbank.payments.application.port.in.ApplyTransferResultUseCase;
import com.diegoferreiracaetano.dbank.payments.application.port.in.GetTransferUseCase;
import com.diegoferreiracaetano.dbank.payments.application.port.in.PublishPendingEventsUseCase;
import com.diegoferreiracaetano.dbank.payments.application.port.in.RequestTransferUseCase;
import com.diegoferreiracaetano.dbank.payments.application.port.out.EventPublisher;
import com.diegoferreiracaetano.dbank.payments.application.port.out.IdempotencyStore;
import com.diegoferreiracaetano.dbank.payments.application.port.out.OutboxStore;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransactionRunner;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransferEvents;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransferRepository;
import com.diegoferreiracaetano.dbank.payments.application.service.ApplyTransferResultService;
import com.diegoferreiracaetano.dbank.payments.application.service.GetTransferService;
import com.diegoferreiracaetano.dbank.payments.application.service.PublishPendingEventsService;
import com.diegoferreiracaetano.dbank.payments.application.service.RequestTransferService;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
class PaymentsConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RequestTransferUseCase requestTransferUseCase(
            TransferRepository transfers,
            IdempotencyStore idempotency,
            TransferEvents events,
            TransactionRunner runner,
            Clock clock) {
        return new RequestTransferService(transfers, idempotency, events, runner, clock);
    }

    @Bean
    PublishPendingEventsUseCase publishPendingEventsUseCase(
            OutboxStore outbox,
            EventPublisher publisher,
            TransactionRunner runner,
            Clock clock,
            @Value("${dbank.outbox.batch-size}") int batchSize) {
        return new PublishPendingEventsService(outbox, publisher, runner, clock, batchSize);
    }

    @Bean
    ApplyTransferResultUseCase applyTransferResultUseCase(TransferRepository transfers, TransactionRunner runner) {
        return new ApplyTransferResultService(transfers, runner);
    }

    @Bean
    GetTransferUseCase getTransferUseCase(TransferRepository transfers) {
        return new GetTransferService(transfers);
    }
}
