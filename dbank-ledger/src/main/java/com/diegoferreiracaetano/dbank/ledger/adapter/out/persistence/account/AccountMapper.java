package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.account;

import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;

final class AccountMapper {

    private AccountMapper() {}

    static AccountEntity toEntity(Account account) {
        return new AccountEntity(account.id().value(), account.balance().cents(), account.version());
    }

    static Account toDomain(AccountEntity entity) {
        return new Account(new AccountId(entity.getId()), Money.ofCents(entity.getBalanceCents()), entity.getVersion());
    }
}
