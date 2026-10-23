package com.diegoferreiracaetano.dbank.ledger.config;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.GetAccountUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.GetStatementUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.OpenAccountUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.PostTransferUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.ProcessTransferRequestUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.PublishPendingEventsUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.AccountRepository;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.EventPublisher;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.LedgerEntryRepository;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.LedgerTransactionRepository;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.OutboxStore;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.TransactionRunner;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.TransferOutcomes;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.TransferResultEvents;
import com.diegoferreiracaetano.dbank.ledger.application.service.GetAccountService;
import com.diegoferreiracaetano.dbank.ledger.application.service.GetStatementService;
import com.diegoferreiracaetano.dbank.ledger.application.service.OpenAccountService;
import com.diegoferreiracaetano.dbank.ledger.application.service.PostTransferService;
import com.diegoferreiracaetano.dbank.ledger.application.service.ProcessTransferRequestService;
import com.diegoferreiracaetano.dbank.ledger.application.service.PublishPendingEventsService;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
class LedgerConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    PostTransferUseCase postTransferUseCase(
            AccountRepository accounts,
            LedgerTransactionRepository transactions,
            TransactionRunner transactionRunner,
            Clock clock) {
        return new PostTransferService(accounts, transactions, transactionRunner, clock);
    }

    @Bean
    ProcessTransferRequestUseCase processTransferRequestUseCase(
            AccountRepository accounts,
            LedgerTransactionRepository transactions,
            TransferOutcomes outcomes,
            TransferResultEvents events,
            TransactionRunner transactionRunner,
            Clock clock) {
        return new ProcessTransferRequestService(accounts, transactions, outcomes, events, transactionRunner, clock);
    }

    @Bean
    PublishPendingEventsUseCase publishPendingEventsUseCase(
            OutboxStore outbox,
            EventPublisher publisher,
            TransactionRunner transactionRunner,
            Clock clock,
            @Value("${dbank.outbox.batch-size}") int batchSize) {
        return new PublishPendingEventsService(outbox, publisher, transactionRunner, clock, batchSize);
    }

    @Bean
    OpenAccountUseCase openAccountUseCase(AccountRepository accounts) {
        return new OpenAccountService(accounts);
    }

    @Bean
    GetAccountUseCase getAccountUseCase(AccountRepository accounts) {
        return new GetAccountService(accounts);
    }

    @Bean
    GetStatementUseCase getStatementUseCase(AccountRepository accounts, LedgerEntryRepository entries) {
        return new GetStatementService(accounts, entries);
    }
}
