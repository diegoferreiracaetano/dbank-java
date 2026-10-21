package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegoferreiracaetano.dbank.ledger.domain.ledger.EntryDirection;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LedgerEntryMapperTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void givenEntity_whenMappingToDomain_thenShouldKeepAllFields() {
        var id = UUID.randomUUID();
        var transactionId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        var entity = new LedgerEntryEntity(id, accountId, EntryDirection.DEBIT, 500, NOW);
        ReflectionTestUtils.setField(entity, "transactionId", transactionId);

        var entry = LedgerEntryMapper.toDomain(entity);

        assertThat(entry.id()).isEqualTo(id);
        assertThat(entry.transactionId().value()).isEqualTo(transactionId);
        assertThat(entry.accountId().value()).isEqualTo(accountId);
        assertThat(entry.direction()).isEqualTo(EntryDirection.DEBIT);
        assertThat(entry.amount().cents()).isEqualTo(500);
        assertThat(entry.createdAt()).isEqualTo(NOW);
    }
}
