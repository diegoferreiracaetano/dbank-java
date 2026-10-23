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

## Publicação: outbox

O produtor nunca publica direto. Ele grava o evento numa tabela `outbox_event`, na mesma transação da mudança de estado, e um relay agendado publica as linhas pendentes e as marca como enviadas. Se o broker estiver fora, a linha continua pendente. O relay trava as linhas com `FOR UPDATE SKIP LOCKED`, então várias instâncias do serviço não publicam a mesma linha ao mesmo tempo.

## Consumo: idempotência

Como a entrega é "pelo menos uma vez", o consumidor tolera repetição por conta própria:

- o `ledger` guarda o resultado de cada transferência (`transfer_outcome`, chave primária no id da transferência) e, ao receber de novo, devolve o resultado guardado, inclusive o de uma recusa;
- o `payments` aplica o resultado pelas transições idempotentes da `Transfer` e só grava quando o estado muda.

## Retentativa e mensagens mortas

Cada consumidor tem um tratador de erros com duas rotas:

| Tipo de erro | Exemplos | O que acontece |
|---|---|---|
| Transitório | banco fora, conflito de versão | nova tentativa com espera crescente (1s, dobrando, teto de 30s, no máximo 2 minutos) |
| Permanente | JSON ilegível, tipo de evento inesperado, motivo desconhecido, contradição entre os serviços, resultado de transferência inexistente | vai direto para `<tópico>.dlt` |

Se as tentativas se esgotarem, a mensagem também vai para o `.dlt`. Lá ela fica intacta, com os cabeçalhos do Spring Kafka que dizem de onde veio (`kafka_dlt-original-topic`, `kafka_dlt-original-offset`) e por que falhou (`kafka_dlt-exception-message`). A retentativa bloqueia a partição enquanto espera, o que preserva a ordem; os tempos ficam em `dbank.kafka.retry` e precisam somar menos que o `max.poll.interval.ms` do Kafka.
