# Dbank

Núcleo transacional de um banco digital: um backend que move dinheiro entre contas com consistência garantida, mesmo sob falhas e alto volume. Cobre a parte difícil de um banco (livro-razão, transferência, consistência), e não o banco inteiro.

Projeto em Java 21, Spring Boot, Postgres, Kafka e Redis, construído passo a passo. O progresso está no [CHECKLIST.md](CHECKLIST.md) e o desenho completo na [spec](docs/superpowers/specs/2026-10-20-dbank-design.md).

## Garantias

- **O dinheiro nunca some nem duplica.** Toda movimentação é um par de lançamentos de partida dobrada, imutáveis, na mesma transação.
- **Idempotência.** Repetir o mesmo `transactionId` não move dinheiro duas vezes.
- **Sem atualização perdida.** Locking otimista por versão: duas operações concorrentes na mesma conta nunca se sobrescrevem.
- **Tudo ou nada.** Um erro no meio desfaz a transferência inteira.
- **Pedidos repetidos não duplicam.** No `payments`, o cabeçalho `Idempotency-Key` faz um retry devolver a transferência original.

## Padrões do projeto

Impostos por teste (ArchUnit), então um desvio reprova o build:

- **Hexagonal.** `domain` e `application` só dependem de si mesmos e do JDK; os adapters não são públicos nem se conhecem; `@Entity` e controllers ficam nos seus adapters.
- **Acesso a dados só por Spring Data JPA.** SQL nativo existe, mas declarado com `@Query` dentro do repositório; JDBC direto (`JdbcTemplate`, `JdbcClient`, `java.sql`) é proibido em código de produção.
- **Serviços não compartilham código**, só contratos (REST e eventos).
- **Testes** no padrão `given/when/then`: unitários sem Docker e de integração contra um Postgres real.

## Status

| Marco | Entrega | Situação |
|---|---|---|
| M0 | Setup de qualidade (lint, cobertura, análise estática) | feito |
| M1 | `dbank-ledger`: domínio, persistência, REST, documentação, testes de integração e de arquitetura | feito |
| M2 | `dbank-payments`: domínio, idempotência com Redis, persistência, REST e testes | em andamento (M2.1 a M2.8 feitos) |
| M3 a M9 | Kafka, CI, observabilidade, resiliência, Kubernetes | planejado |

Existem o `dbank-ledger` e o `dbank-payments`. Ainda não conversam entre si: a ligação por Kafka é o M3.

## Arquitetura

```
Cliente ─REST─▶ payments ──outbox──▶ Kafka ──▶ ledger ──outbox──▶ Kafka ──▶ payments (conclui)
                                                                       └──▶ notifications
```

Cada serviço tem seu banco e segue a Hexagonal: `domain`, `application` (casos de uso e portas) e `adapter` (REST, persistência). Detalhes na [spec](docs/superpowers/specs/2026-10-20-dbank-design.md).

## Como rodar

Pré-requisitos: Docker e um JDK para executar o Gradle (o projeto compila com Java 21 via toolchain).

Suba o Postgres e o Redis:

```bash
docker compose up -d
```

Em um volume do Postgres que já existia, crie uma vez o banco do `payments`:

```bash
docker compose exec postgres psql -U dbank -d postgres -c "CREATE DATABASE payments"
```

Suba cada serviço no perfil `dev`, em terminais separados. O perfil `dev` habilita o Swagger UI e, no `ledger`, cria uma conta `treasury` com saldo para você testar:

```bash
./gradlew :dbank-ledger:bootRun --args='--spring.profiles.active=dev'
```

```bash
./gradlew :dbank-payments:bootRun --args='--spring.profiles.active=dev'
```

| Serviço | Porta | Swagger UI | Guia |
|---|---|---|---|
| `dbank-ledger` | 8081 | `http://localhost:8081/swagger-ui.html` | [README do ledger](dbank-ledger/README.md) |
| `dbank-payments` | 8082 | `http://localhost:8082/swagger-ui.html` | [README do payments](dbank-payments/README.md) |

## Testes e qualidade

| Comando | O que roda |
|---|---|
| `./gradlew test` | testes unitários e regras de arquitetura (ArchUnit), rápidos e sem Docker |
| `./gradlew integrationTest` | testes de integração contra um Postgres real (Testcontainers, exige Docker) |
| `./gradlew check` | tudo: formatação, Checkstyle, SpotBugs, testes e cobertura mínima |
| `./gradlew spotlessApply` | corrige a formatação |

## Documentação

- Contratos das APIs: [`ledger`](docs/api/ledger-openapi.yaml) e [`payments`](docs/api/payments-openapi.yaml)
- Desenho e decisões: [spec](docs/superpowers/specs/2026-10-20-dbank-design.md)
- Plano e pendências técnicas: [CHECKLIST.md](CHECKLIST.md)
- Roteiro de estudo para entrevista: [docs/interview/m1-ledger.md](docs/interview/m1-ledger.md)
