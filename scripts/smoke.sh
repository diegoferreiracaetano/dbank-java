#!/usr/bin/env bash
set -euo pipefail

LEDGER=${LEDGER:-http://localhost:8081}
PAYMENTS=${PAYMENTS:-http://localhost:8082}
TREASURY=00000000-0000-0000-0000-000000000001

id_of() { sed -n 's/.*"id":"\([^"]*\)".*/\1/p'; }

from=$(curl -fsS -X POST "$LEDGER/v1/accounts" | id_of)
to=$(curl -fsS -X POST "$LEDGER/v1/accounts" | id_of)

curl -fsS -X POST "$LEDGER/v1/transactions" -H 'Content-Type: application/json' \
  -d "{\"transactionId\":\"$(uuidgen)\",\"fromAccountId\":\"$TREASURY\",\"toAccountId\":\"$from\",\"amountCents\":1000}"

transfer=$(curl -fsS -X POST "$PAYMENTS/v1/transfers" -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $(uuidgen)" \
  -d "{\"fromAccountId\":\"$from\",\"toAccountId\":\"$to\",\"amountCents\":300}" | id_of)

status=PENDING
for _ in $(seq 1 30); do
  status=$(curl -fsS "$PAYMENTS/v1/transfers/$transfer" | sed -n 's/.*"status":"\([^"]*\)".*/\1/p')
  [ "$status" = COMPLETED ] && break
  sleep 1
done

echo "transfer $transfer: $status"
[ "$status" = COMPLETED ]
