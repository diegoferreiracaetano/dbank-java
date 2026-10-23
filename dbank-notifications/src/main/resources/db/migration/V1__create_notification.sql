CREATE TABLE notification (
    event_id    UUID        PRIMARY KEY,
    transfer_id UUID        NOT NULL,
    account_id  UUID        NOT NULL,
    message     TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
