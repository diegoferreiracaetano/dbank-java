package com.diegoferreiracaetano.dbank.payments.config;

import com.diegoferreiracaetano.dbank.payments.application.port.in.GetTransferUseCase;
import com.diegoferreiracaetano.dbank.payments.application.port.in.RequestTransferUseCase;
import com.diegoferreiracaetano.dbank.payments.application.port.out.IdempotencyStore;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransactionRunner;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransferRepository;
import com.diegoferreiracaetano.dbank.payments.application.service.GetTransferService;
import com.diegoferreiracaetano.dbank.payments.application.service.RequestTransferService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class PaymentsConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RequestTransferUseCase requestTransferUseCase(
            TransferRepository transfers, IdempotencyStore idempotency, TransactionRunner runner, Clock clock) {
        return new RequestTransferService(transfers, idempotency, runner, clock);
    }

    @Bean
    GetTransferUseCase getTransferUseCase(TransferRepository transfers) {
        return new GetTransferService(transfers);
    }
}
