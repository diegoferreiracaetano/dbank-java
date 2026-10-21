package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence;

import com.diegoferreiracaetano.dbank.ledger.application.port.out.ConcurrentUpdateException;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.TransactionRunner;
import org.springframework.dao.OptimisticLockingFailureException;
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
        try {
            template.executeWithoutResult(status -> action.run());
        } catch (OptimisticLockingFailureException e) {
            throw new ConcurrentUpdateException(e);
        }
    }
}
