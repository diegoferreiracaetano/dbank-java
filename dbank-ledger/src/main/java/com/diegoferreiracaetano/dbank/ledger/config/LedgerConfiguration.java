package com.diegoferreiracaetano.dbank.ledger.config;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.GetAccountUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.GetStatementUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.OpenAccountUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.PostTransferUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.AccountRepository;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.LedgerEntryRepository;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.LedgerTransactionRepository;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.TransactionRunner;
import com.diegoferreiracaetano.dbank.ledger.application.service.GetAccountService;
import com.diegoferreiracaetano.dbank.ledger.application.service.GetStatementService;
import com.diegoferreiracaetano.dbank.ledger.application.service.OpenAccountService;
import com.diegoferreiracaetano.dbank.ledger.application.service.PostTransferService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
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
