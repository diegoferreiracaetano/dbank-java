# dbank-ledger

Livro-razão do Dbank: contas, lançamentos imutáveis de partida dobrada e transações idempotentes. Os valores são sempre inteiros em centavos.

## Endpoints

| Método e caminho | O que faz | Sucesso |
|---|---|---|
| `POST /v1/accounts` | abre uma conta com saldo zero | 201 com `Location` |
| `GET /v1/accounts/{id}` | consulta o saldo | 200 |
| `GET /v1/accounts/{id}/entries?page=&size=` | extrato, do mais recente ao mais antigo (`size` de 1 a 100, padrão 20) | 200 |
| `POST /v1/transactions` | lança uma transação: debita uma conta e credita outra | 204 |

O contrato completo, com exemplos, está em [`docs/api/ledger-openapi.yaml`](../docs/api/ledger-openapi.yaml).

## Integração com o payments (Kafka)

- **Entrada:** `TransferRequestedConsumer` lê `payments.transfer-requested.v1` e chama o caso de uso `ProcessTransferRequestUseCase`, que **devolve** `Settled` ou `Rejected(motivo)` em vez de lançar exceção. Saldo insuficiente e conta inexistente são recusas, e não erros.
- **Idempotência:** o resultado de cada transferência é guardado em `transfer_outcome`, na mesma transação do lançamento. Uma mensagem repetida devolve o resultado guardado. Isso vale também para recusas: uma transferência recusada por saldo insuficiente continua recusada se a mensagem chegar de novo depois de a conta ser recarregada.
- **Saída:** o resultado vira um evento na tabela `outbox_event`, na mesma transação, e um relay agendado o publica em `ledger.transfer-result.v1` (`TransferSettled`, ou `TransferRejected` com o motivo). Uma mensagem repetida não gera evento novo.
- **Falhas:** erro transitório é retentado com espera crescente. Mensagem ilegível vai para `payments.transfer-requested.v1.ledger.dlt`. Detalhes em [`docs/events`](../docs/events/README.md).

O `POST /v1/transactions` continua independente desse fluxo e segue lançando exceções (422, 404).

## Erros

Todos seguem o RFC 9457 (`application/problem+json`), com `type`, `title`, `status` e `detail`.

| Status | `type` | Quando |
|---|---|---|
| 400 | `urn:dbank:problem:invalid-request` ou validação | valor não positivo, UUID inválido, mesma conta nos dois lados |
| 404 | `urn:dbank:problem:account-not-found` | conta inexistente |
| 409 | `urn:dbank:problem:concurrent-update` | outra operação alterou a conta no meio; repita a operação inteira |
| 422 | `urn:dbank:problem:insufficient-funds` | saldo insuficiente |

## Testando pelo Swagger

Com o serviço rodando no perfil `dev`, abra `http://localhost:8081/swagger-ui.html`.

1. `POST /v1/accounts` e copie o `id` devolvido.
2. `POST /v1/transactions` com `fromAccountId` igual à conta `treasury` (`00000000-0000-0000-0000-000000000001`), `toAccountId` igual ao id copiado, um `transactionId` novo (`uuidgen`) e `amountCents` igual a `1000`.
3. `GET /v1/accounts/{id}` mostra o saldo e `GET /v1/accounts/{id}/entries` mostra o lançamento.
4. Repita o mesmo `transactionId`: a resposta é 204 e o saldo não muda.

O Swagger UI só é habilitado no perfil `dev`.

## Estrutura

```
domain/        regras, sem Spring nem JPA: shared, account, ledger
application/   casos de uso e portas: port.in, port.out, service
adapter/in/    rest (controllers, DTOs, contrato OpenAPI), messaging (consumidor do pedido) e scheduling (relay da outbox)
adapter/out/   persistence (entidades JPA, mappers, repositórios, resultados e outbox) e messaging (publicador Kafka)
config/        ligação dos beans
```

O banco é versionado pelo Flyway em `src/main/resources/db/migration`. O perfil `dev` acrescenta `db/dev`, que cria a conta `treasury`.

## Testes

- **Unitários** (`src/test`): domínio, casos de uso com fakes em memória, mappers e controllers com `@WebMvcTest`.
- **Arquitetura** (`src/test`, ArchUnit): `domain` e `application` só dependem de si mesmos e do JDK, os adapters não se conhecem nem são públicos, e `@Entity` e controllers ficam nos seus adapters.
- **Integração** (`src/integrationTest`): Postgres 16 real via Testcontainers, cobrindo constraints, locking otimista, rollback, extrato, idempotência, transferências concorrentes, o fluxo HTTP e o contrato da API.

## Contrato da API

O arquivo `docs/api/ledger-openapi.yaml` é versionado e um teste de integração reprova o build se a API mudar sem ele. Para atualizá-lo depois de uma mudança intencional:

```bash
./gradlew integrationTest -PupdateContract
```

Revise o `git diff` do arquivo antes de commitar.
