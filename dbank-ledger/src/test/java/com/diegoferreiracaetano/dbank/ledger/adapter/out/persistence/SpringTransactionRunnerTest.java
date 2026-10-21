package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.diegoferreiracaetano.dbank.ledger.application.port.out.ConcurrentUpdateException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;

class SpringTransactionRunnerTest {

    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final SpringTransactionRunner runner = new SpringTransactionRunner(transactionManager);

    @Test
    void givenOptimisticLockFailure_whenRunning_thenShouldTranslateToConcurrentUpdate() {
        assertThatThrownBy(() -> runner.run(() -> {
                    throw new OptimisticLockingFailureException("stale");
                }))
                .isInstanceOf(ConcurrentUpdateException.class);
    }

    @Test
    void givenAnyOtherFailure_whenRunning_thenShouldPropagateItUntouched() {
        assertThatThrownBy(() -> runner.run(() -> {
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenSuccessfulAction_whenRunning_thenShouldCommit() {
        runner.run(() -> {});

        verify(transactionManager).commit(any());
    }
}
