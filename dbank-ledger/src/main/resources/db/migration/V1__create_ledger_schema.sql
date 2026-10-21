CREATE TABLE account (
    id            UUID   PRIMARY KEY,
    balance_cents BIGINT NOT NULL,
    CONSTRAINT ck_account_balance_non_negative CHECK (balance_cents >= 0)
);

CREATE TABLE ledger_transaction (
    id         UUID        PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE ledger_entry (
    id             UUID        PRIMARY KEY,
    transaction_id UUID        NOT NULL REFERENCES ledger_transaction (id),
    account_id     UUID        NOT NULL REFERENCES account (id),
    direction      VARCHAR(6)  NOT NULL,
    amount_cents   BIGINT      NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_ledger_entry_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_ledger_entry_amount_positive CHECK (amount_cents > 0)
);

CREATE INDEX ix_ledger_entry_transaction ON ledger_entry (transaction_id);
CREATE INDEX ix_ledger_entry_account_created ON ledger_entry (account_id, created_at DESC);
