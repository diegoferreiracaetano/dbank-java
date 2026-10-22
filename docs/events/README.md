# Event contracts

Events are the only thing `payments` and `ledger` share. Each schema is the source of truth for its topic.

| Topic | Producer | Consumer | Schema |
|---|---|---|---|
| `payments.transfer-requested.v1` | payments | ledger | [transfer-requested.v1.schema.json](transfer-requested.v1.schema.json) |
| `ledger.transfer-result.v1` | ledger | payments | [transfer-result.v1.schema.json](transfer-result.v1.schema.json) |

## Envelope

Every message is a JSON text with `eventId`, `eventType`, `occurredAt` and a `payload`. Amounts are integer cents. The message key is the source account id, so events of the same account keep their order inside a partition.

## Delivery and versioning

Delivery is at least once: consumers must be idempotent, using `eventId` and the business identifiers. A backward-compatible change (a new optional field) keeps the version. An incompatible change creates a new topic with the `.v2` suffix, and both run side by side until the consumers migrate.
