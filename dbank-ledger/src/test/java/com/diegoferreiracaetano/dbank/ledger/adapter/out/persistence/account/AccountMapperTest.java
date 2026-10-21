package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import org.junit.jupiter.api.Test;

class AccountMapperTest {

    @Test
    void givenAccount_whenMappingToEntity_thenShouldKeepIdAndBalance() {
        var account = new Account(AccountId.generate(), Money.ofCents(750));

        var entity = AccountMapper.toEntity(account);

        assertThat(entity.getId()).isEqualTo(account.id().value());
        assertThat(entity.getBalanceCents()).isEqualTo(750);
    }

    @Test
    void givenAccount_whenMappingToEntityAndBack_thenShouldReturnEqualAccount() {
        var account = new Account(AccountId.generate(), Money.ofCents(750));

        var result = AccountMapper.toDomain(AccountMapper.toEntity(account));

        assertThat(result).isEqualTo(account);
    }

    @Test
    void givenNewAccount_whenMappingToEntity_thenShouldHaveNoVersion() {
        var entity = AccountMapper.toEntity(new Account(AccountId.generate(), Money.ofCents(750)));

        assertThat(entity.getVersion()).isNull();
    }

    @Test
    void givenAccountWithVersion_whenMappingToEntityAndBack_thenShouldKeepVersion() {
        var account = new Account(AccountId.generate(), Money.ofCents(750), 5L);

        var result = AccountMapper.toDomain(AccountMapper.toEntity(account));

        assertThat(result.version()).isEqualTo(5L);
    }
}
