CREATE TABLE outbox_event (
    id          UUID         PRIMARY KEY,
    topic       VARCHAR(255) NOT NULL,
    message_key VARCHAR(64)  NOT NULL,
    payload     TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    sent_at     TIMESTAMPTZ
);

CREATE INDEX ix_outbox_event_pending ON outbox_event (created_at) WHERE sent_at IS NULL;
