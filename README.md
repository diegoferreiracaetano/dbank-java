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
| M2 | `dbank-payments`: domínio, idempotência com Redis, persistência, REST e testes | feito |
| M3 | `payments` e `ledger` por Kafka: outbox, consumidores idempotentes, retentativa com DLQ e teste ponta a ponta | feito |
| M4 a M9 | CI, observabilidade, resiliência, Kubernetes | planejado |

Existem o `dbank-ledger` e o `dbank-payments`, que conversam por Kafka.

## Fluxo de uma transferência

1. `POST /v1/transfers` no `payments`, com `Idempotency-Key`. A transferência nasce `PENDING` e o evento `TransferRequested` é gravado na **outbox**, na mesma transação. A resposta é 202.
2. Um relay agendado publica as linhas pendentes da outbox no tópico `payments.transfer-requested.v1`, esperando a confirmação do broker. Se o Kafka estiver fora, a linha continua pendente e é reenviada.
3. O `ledger` consome o pedido, tenta a transferência e guarda o resultado (`SETTLED` ou `REJECTED` com o motivo) na mesma transação do lançamento. Uma mensagem repetida devolve o resultado guardado, e nunca move o dinheiro duas vezes.
4. O resultado vira um evento na outbox do `ledger` (`TransferSettled` ou `TransferRejected`), publicado em `ledger.transfer-result.v1`.
5. O `payments` consome o resultado e leva a transferência a `COMPLETED` ou `FAILED`, só gravando se o estado mudou. A `Transfer` tem `version`, então escritas contraditórias simultâneas falham em vez de a última vencer.

| Tópico | Quem publica | Quem consome | Mensagens mortas |
|---|---|---|---|
| `payments.transfer-requested.v1` | `payments` | `ledger` | `payments.transfer-requested.v1.dlt` |
| `ledger.transfer-result.v1` | `ledger` | `payments` | `ledger.transfer-result.v1.dlt` |

A entrega é "pelo menos uma vez". Falha transitória (banco fora, conflito de versão) é retentada com espera crescente, de 1s até 30s e no máximo 2 minutos. Mensagem ilegível, contradição entre os serviços ou resultado de transferência inexistente vão direto para o tópico `.dlt`, com a mensagem original e a causa nos cabeçalhos. Os esquemas dos eventos estão em [`docs/events`](docs/events/README.md).

Para ler a fila de mensagens mortas de um tópico:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic payments.transfer-requested.v1.dlt --from-beginning --property print.headers=true
```

O reprocessamento de uma mensagem morta é manual: leia a mensagem, corrija a causa e republique o JSON original no tópico de origem.

## Arquitetura

```
Cliente ─REST─▶ payments ──outbox──▶ Kafka ──▶ ledger ──outbox──▶ Kafka ──▶ payments (conclui)
                                                                       └──▶ notifications
```

Cada serviço tem seu banco e segue a Hexagonal: `domain`, `application` (casos de uso e portas) e `adapter` (REST, persistência). Detalhes na [spec](docs/superpowers/specs/2026-10-20-dbank-design.md).

## Como rodar

Pré-requisitos: Docker e um JDK para executar o Gradle (o projeto compila com Java 21 via toolchain).

Suba o Postgres, o Redis e o Kafka:

```bash
docker compose up -d
```

Em um volume do Postgres que já existia, crie uma vez o banco do `payments`:

```bash
docker compose exec postgres psql -U dbank -d postgres -c "CREATE DATABASE payments"
```

Suba cada serviço no perfil `dev`, em terminais separados. O `payments` só conclui as transferências com o `ledger` no ar. O perfil `dev` habilita o Swagger UI e, no `ledger`, cria uma conta `treasury` com saldo para você testar:

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
| `./gradlew integrationTest` | testes de integração contra Postgres, Redis e Kafka reais (Testcontainers, exige Docker) |
| `./gradlew check` | tudo: formatação, Checkstyle, SpotBugs, testes e cobertura mínima |
| `./gradlew e2eTest` | ponta a ponta: sobe Postgres, Redis e Kafka (Testcontainers) e os dois serviços como processos reais, e dirige o fluxo só por HTTP; fora do `check`, por ser lento |
| `./gradlew spotlessApply` | corrige a formatação |

## Documentação

- Contratos das APIs: [`ledger`](docs/api/ledger-openapi.yaml) e [`payments`](docs/api/payments-openapi.yaml)
- Contratos dos eventos entre os serviços: [`docs/events`](docs/events/README.md)
- Desenho e decisões: [spec](docs/superpowers/specs/2026-10-20-dbank-design.md)
- Plano e pendências técnicas: [CHECKLIST.md](CHECKLIST.md)
- Roteiros de consulta: [M0 e build](docs/interview/m0-qualidade-e-build.md), [M1](docs/interview/m1-ledger.md), [M2](docs/interview/m2-payments.md) e [M3](docs/interview/m3-kafka.md)
