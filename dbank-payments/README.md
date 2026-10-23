# dbank-payments

Recebe pedidos de transferência e guarda o estado. O dinheiro só se move no `ledger`: aqui a transferência nasce `PENDING` e é concluída depois, quando o `ledger` responde por Kafka. Os valores são sempre inteiros em centavos.

## Endpoints

| Método e caminho | O que faz | Sucesso |
|---|---|---|
| `POST /v1/transfers` | pede uma transferência; exige o cabeçalho `Idempotency-Key` | 202 com `Location` |
| `GET /v1/transfers/{id}` | consulta o estado atual | 200 |

O contrato completo, com exemplos, está em [`docs/api/payments-openapi.yaml`](../docs/api/payments-openapi.yaml).

## Por que 202 e não 201

A transferência ainda não aconteceu: o pedido foi aceito para processamento assíncrono. O cliente acompanha pelo `GET`, que mostra `PENDING` até o `ledger` responder.

## Estados da transferência

`PENDING` vira `COMPLETED` ou `FAILED`, e nunca volta atrás. Repetir o mesmo resultado não muda nada. Um resultado contraditório (concluir uma transferência que falhou) é erro.

## Idempotência

O cabeçalho `Idempotency-Key` identifica o **pedido**, e não o conteúdo. O app gera uma chave por intenção do usuário e a reenvia sem mudar em cada tentativa.

| O que chega | O que o serviço faz |
|---|---|
| chave nova | cria a transferência `PENDING` e registra a chave |
| chave já vista, mesmo pedido | devolve a transferência original, com o estado atual, e o cabeçalho `Idempotency-Replayed: true` |
| chave já vista, pedido diferente | recusa com 422 |
| chaves diferentes, pedidos idênticos | duas transferências, porque são duas intenções |

A reserva da chave é atômica: um `INSERT ... ON CONFLICT DO NOTHING` no Postgres, declarado como consulta nativa no repositório JPA. Pedidos simultâneos com a mesma chave são serializados pelo índice único do banco, e só um cria a transferência.

## Onde o Redis entra

O Redis guarda em cache o registro "chave, assinatura, transferência", que nunca muda depois de criado. Por isso o cache não precisa de invalidação.

- Só entra no cache o que o banco confirmou como "já existia", nunca uma reserva recém-feita, que ainda poderia ser desfeita.
- Se o Redis falhar, o pedido segue pelo banco como se o cache estivesse vazio.
- O prazo é de 24 horas, configurável em `dbank.idempotency.cache.ttl`.

## Integração com o ledger (Kafka)

- **Saída:** ao aceitar um pedido novo, o serviço grava o evento `TransferRequested` na tabela `outbox_event`, na mesma transação da `Transfer`. Um relay agendado (`dbank.outbox`) publica as linhas pendentes em `payments.transfer-requested.v1`, com a conta de origem como chave, e só as marca como enviadas depois da confirmação do broker. Uma repetição com a mesma `Idempotency-Key` não gera evento novo.
- **Entrada:** `TransferResultConsumer` lê `ledger.transfer-result.v1` e aplica `TransferSettled` ou `TransferRejected` à transferência. Repetir o mesmo resultado não grava nada.
- **Concorrência:** a `Transfer` tem `version` (locking otimista). Dois resultados contraditórios simultâneos não se sobrescrevem: o segundo falha, a mensagem é reentregue e a contradição aparece.
- **Falhas:** erro transitório é retentado com espera crescente. Mensagem ilegível, resultado contraditório ou de transferência inexistente vão para `ledger.transfer-result.v1.payments.dlt`. Detalhes em [`docs/events`](../docs/events/README.md).

## Erros

Todos seguem o RFC 9457 (`application/problem+json`).

| Status | `type` | Quando |
|---|---|---|
| 400 | `urn:dbank:problem:invalid-request` ou validação | cabeçalho ausente, valor não positivo, mesma conta nos dois lados |
| 404 | `urn:dbank:problem:transfer-not-found` | transferência inexistente |
| 422 | `urn:dbank:problem:idempotency-key-reuse` | mesma chave com um pedido diferente |

## Testando pelo Swagger

Com o serviço rodando no perfil `dev`, abra `http://localhost:8082/swagger-ui.html`.

1. `POST /v1/transfers` com o cabeçalho `Idempotency-Key` igual a `teste-1` e o corpo de exemplo. A resposta é 202 com `PENDING`.
2. Repita exatamente o mesmo pedido: 202 de novo, com `Idempotency-Replayed: true` e o mesmo `id`.
3. Mude o valor e mantenha a chave: 422.
4. `GET /v1/transfers/{id}` com o `id` devolvido.

O Swagger UI só é habilitado no perfil `dev`.

## Estrutura

```
domain/        regras, sem Spring nem JPA: shared (Money), transfer (Transfer e seus tipos)
application/   idempotency, port.in, port.out e service
adapter/in/    rest (controller, DTOs, contrato OpenAPI), messaging (consumidor do resultado) e scheduling (relay da outbox)
adapter/out/   persistence (transferência, chaves e outbox, em JPA), cache (Redis) e messaging (publicador Kafka)
config/        ligação dos beans
```

## Banco

O Flyway cria as tabelas `transfer` (com `version`), `idempotency_key` e `outbox_event` em `src/main/resources/db/migration`. O serviço usa o banco `payments`, no mesmo Postgres do compose. Em um volume novo, o script de `docker/postgres` o cria. Em um volume que já existe, crie-o uma vez:

```bash
docker compose exec postgres psql -U dbank -d postgres -c "CREATE DATABASE payments"
```

## Contrato da API

O arquivo `docs/api/payments-openapi.yaml` é versionado, e um teste de integração reprova o build se a API mudar sem ele. Para atualizá-lo depois de uma mudança intencional:

```bash
./gradlew :dbank-payments:integrationTest -PupdateContract
```
