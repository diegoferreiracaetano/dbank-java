package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence;

import com.diegoferreiracaetano.dbank.ledger.application.port.out.TransactionRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate template;

    SpringTransactionRunner(PlatformTransactionManager transactionManager) {
        this.template = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(Runnable action) {
        template.executeWithoutResult(status -> action.run());
    }
}
