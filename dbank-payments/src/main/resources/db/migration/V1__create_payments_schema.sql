CREATE TABLE transfer (
    id              UUID        PRIMARY KEY,
    from_account_id UUID        NOT NULL,
    to_account_id   UUID        NOT NULL,
    amount_cents    BIGINT      NOT NULL,
    status          VARCHAR(9)  NOT NULL,
    failure_reason  VARCHAR(20),
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_transfer_amount_positive CHECK (amount_cents > 0),
    CONSTRAINT ck_transfer_distinct_accounts CHECK (from_account_id <> to_account_id),
    CONSTRAINT ck_transfer_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_transfer_failure_reason
        CHECK (failure_reason IS NULL OR failure_reason IN ('INSUFFICIENT_FUNDS', 'ACCOUNT_NOT_FOUND')),
    CONSTRAINT ck_transfer_reason_iff_failed CHECK ((status = 'FAILED') = (failure_reason IS NOT NULL))
);

CREATE TABLE idempotency_key (
    key         VARCHAR(255) PRIMARY KEY,
    fingerprint VARCHAR(64)  NOT NULL,
    transfer_id UUID         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_idempotency_key_transfer
        FOREIGN KEY (transfer_id) REFERENCES transfer (id) DEFERRABLE INITIALLY DEFERRED
);
