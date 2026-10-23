CREATE TABLE transfer_outcome (
    transfer_id  UUID        PRIMARY KEY,
    status       VARCHAR(8)  NOT NULL,
    reason       VARCHAR(20),
    processed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_transfer_outcome_status CHECK (status IN ('SETTLED', 'REJECTED')),
    CONSTRAINT ck_transfer_outcome_reason
        CHECK (reason IS NULL OR reason IN ('INSUFFICIENT_FUNDS', 'ACCOUNT_NOT_FOUND')),
    CONSTRAINT ck_transfer_outcome_reason_iff_rejected CHECK ((status = 'REJECTED') = (reason IS NOT NULL))
);
