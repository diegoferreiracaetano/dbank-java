# Dbank

Núcleo transacional de um banco digital: um backend que move dinheiro entre contas com consistência garantida, mesmo sob falhas e alto volume. Cobre a parte difícil de um banco (livro-razão, transferência, consistência), e não o banco inteiro.

Projeto em Java 21, Spring Boot, Postgres, Kafka e Redis, construído passo a passo. O progresso está no [CHECKLIST.md](CHECKLIST.md) e o desenho completo na [spec](docs/superpowers/specs/2026-10-20-dbank-design.md).

## Garantias

- **O dinheiro nunca some nem duplica.** Toda movimentação é um par de lançamentos de partida dobrada, imutáveis, na mesma transação.
- **Idempotência.** Repetir o mesmo `transactionId` não move dinheiro duas vezes.
- **Sem atualização perdida.** Locking otimista por versão: duas operações concorrentes na mesma conta nunca se sobrescrevem.
- **Tudo ou nada.** Um erro no meio desfaz a transferência inteira.

## Status

| Marco | Entrega | Situação |
|---|---|---|
| M0 | Setup de qualidade (lint, cobertura, análise estática) | feito |
| M1 | `dbank-ledger`: domínio, persistência, REST, documentação, testes de integração e de arquitetura | feito |
| M2 a M9 | `dbank-payments`, Kafka, CI, observabilidade, resiliência, Kubernetes | planejado |

Hoje existe apenas o `dbank-ledger`.

## Arquitetura

```
Cliente ─REST─▶ payments ──outbox──▶ Kafka ──▶ ledger ──outbox──▶ Kafka ──▶ payments (conclui)
                                                                       └──▶ notifications
```

Cada serviço tem seu banco e segue a Hexagonal: `domain`, `application` (casos de uso e portas) e `adapter` (REST, persistência). Detalhes na [spec](docs/superpowers/specs/2026-10-20-dbank-design.md).

## Como rodar

Pré-requisitos: Docker e um JDK para executar o Gradle (o projeto compila com Java 21 via toolchain).

Suba o Postgres:

```bash
docker compose up -d
```

Suba o serviço no perfil `dev`, que cria uma conta `treasury` com saldo para você testar:

```bash
./gradlew :dbank-ledger:bootRun --args='--spring.profiles.active=dev'
```

O Swagger UI fica em `http://localhost:8081/swagger-ui.html`. O passo a passo para testar está no [README do ledger](dbank-ledger/README.md).

## Testes e qualidade

| Comando | O que roda |
|---|---|
| `./gradlew test` | testes unitários e regras de arquitetura (ArchUnit), rápidos e sem Docker |
| `./gradlew integrationTest` | testes de integração contra um Postgres real (Testcontainers, exige Docker) |
| `./gradlew check` | tudo: formatação, Checkstyle, SpotBugs, testes e cobertura mínima |
| `./gradlew spotlessApply` | corrige a formatação |

## Documentação

- Contrato da API: [`docs/api/ledger-openapi.yaml`](docs/api/ledger-openapi.yaml)
- Desenho e decisões: [spec](docs/superpowers/specs/2026-10-20-dbank-design.md)
- Plano e pendências técnicas: [CHECKLIST.md](CHECKLIST.md)
