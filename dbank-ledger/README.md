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
adapter/in/    rest: controllers, DTOs e contrato OpenAPI
adapter/out/   persistence: entidades JPA, mappers e repositórios
config/        ligação dos beans
```

O banco é versionado pelo Flyway em `src/main/resources/db/migration`. O perfil `dev` acrescenta `db/dev`, que cria a conta `treasury`.

## Contrato da API

O arquivo `docs/api/ledger-openapi.yaml` é versionado e um teste de integração reprova o build se a API mudar sem ele. Para atualizá-lo depois de uma mudança intencional:

```bash
./gradlew integrationTest -PupdateContract
```

Revise o `git diff` do arquivo antes de commitar.
