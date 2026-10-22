package com.diegoferreiracaetano.dbank.payments.adapter.out.persistence;

import com.diegoferreiracaetano.dbank.payments.application.port.out.TransactionRunner;
import java.util.function.Supplier;
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
    public <T> T run(Supplier<T> action) {
        return template.execute(status -> action.get());
    }
}
