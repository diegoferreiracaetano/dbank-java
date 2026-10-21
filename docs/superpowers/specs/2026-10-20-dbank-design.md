# Dbank — Design

Data: 2026-10-20 · Pacote base: `com.diegoferreiracaetano.dbank`

## 1. Objetivos

### 1.1 Objetivo do software

**Dbank é o núcleo transacional de um banco digital: um backend que move dinheiro entre contas com consistência garantida, mesmo sob falhas e alto volume.**

- O cliente pede uma transferência (`POST /v1/transfers`).
- O sistema garante que o dinheiro nunca some nem duplica:
  - Reenviar o mesmo pedido não cobra duas vezes (idempotência).
  - Falha no meio do caminho não deixa débito sem crédito (outbox, saga e consumidores idempotentes).
  - Saldo insuficiente rejeita a transferência de forma explícita, sem estado ambíguo.
- Todo movimento fica em um livro-razão imutável de partida dobrada, permitindo auditar o saldo a qualquer momento.
- O cliente acompanha o estado da transferência (`PENDING → COMPLETED | FAILED`) e é notificado ao final.

Problema resolvido: processar pagamentos de forma assíncrona e escalável sem abrir mão de correção.

Fora de escopo: login de usuário final completo, multi-moeda, estorno, KYC, antifraude, conta corrente, crédito e regulação. O Dbank cobre a parte difícil de um banco (ledger, transferência, consistência), não o banco inteiro.

### 1.2 Objetivo do projeto (entrevista)

Provar, com código rodando, domínio do que a vaga pede: Java, Spring Boot, JPA/Hibernate, Redis, Kafka, REST, microsserviços, Hexagonal, processos assíncronos, CI/CD, DevOps, ambiente crítico/alta volumetria e noções de cloud. Cada decisão precisa ter resposta pronta para "por que fez assim?".

Critérios de sucesso verificáveis:

1. `docker compose up` sobe tudo e uma transferência de ponta a ponta funciona.
2. Um teste prova que reenviar a mesma `Idempotency-Key` ou o mesmo evento não duplica o lançamento.
3. Um teste derruba o consumidor no meio do fluxo e o sistema converge para o estado correto.
4. O CI roda build, testes com Testcontainers e ArchUnit a cada push.
5. Do M6 em diante, o README traz número real de carga (tx/s e p95 medidos).

Regra de corte: toda decisão técnica deve ajudar um desses dois objetivos; o que não ajudar fica fora.

## 2. Serviços e fluxo

| Serviço | Responsabilidade | Arquitetura |
|---|---|---|
| `dbank-payments` | API de transferência, idempotência, estado da saga | Hexagonal completa |
| `dbank-ledger` | Contas, saldo, lançamentos de partida dobrada, extrato | Hexagonal completa |
| `dbank-notifications` | Consome eventos finais e notifica (adapter simulado por log) | Simples, em camadas |

```
Cliente ─REST─▶ payments ──outbox──▶ Kafka ──▶ ledger ──outbox──▶ Kafka ──▶ payments (conclui)
                  │                                                    └──▶ notifications
                Postgres + Redis                                      Postgres + Redis
```

Fluxo de uma transferência:

1. `POST /v1/transfers` com `Idempotency-Key`. O `payments` valida a chave (Redis, com constraint única no Postgres como garantia final), grava a transferência `PENDING` e o evento `TransferRequested` na outbox, na mesma transação.
2. O relay da outbox publica no Kafka, com a conta de origem como chave de partição para preservar ordem.
3. O `ledger` consome, aplica débito e crédito com locking otimista e grava os lançamentos mais `TransferSettled` ou `TransferRejected` (saldo insuficiente), também via outbox.
4. O `payments` consome o resultado e fecha a transferência. O `notifications` consome o mesmo evento.

Garantias:

- Entrega at-least-once, com consumidores idempotentes por `eventId` (tabela de eventos processados).
- Sem transação distribuída: consistência eventual com compensação explícita.
- Falha repetida de consumo: retry com backoff e, depois, DLQ.

Abordagem escolhida: Transactional Outbox + saga coreografada por Kafka. Alternativas descartadas: orquestração central (acopla ao orquestrador, mostra menos mensageria por eventos) e REST síncrono entre serviços (fraco para volumetria e processos assíncronos).

## 3. Estrutura interna e dados

Hexagonal em `payments` e `ledger`, um módulo por serviço:

```
domain/        regras (sem Spring nem JPA), separado por conceito:
  shared/        Money e tipos comuns
  account/       Account, AccountId, InsufficientFundsException
  ledger/        LedgerEntry, LedgerTransaction e tipos relacionados
application/   casos de uso + portas
  port.in/       casos de uso (UseCase)
  port.out/      Repository, EventPublisher
  service/       implementação dos casos de uso
adapter/in/    rest/, kafka/ (consumers)
adapter/out/   persistence/ (JPA), messaging/ (outbox relay), cache/ (Redis)
```

A separação por responsabilidade vale dentro de cada camada, e os testes espelham os mesmos pacotes. Pastas só são criadas quando há código nelas.

ArchUnit impõe que `domain` e `application` não importem `adapter` nem Spring, e que não haja ciclos entre os pacotes de domínio (`shared` ← `account` ← `ledger`).

Dados (Postgres, um banco por serviço, migrações Flyway):

- `payments`: `transfer`, `idempotency_key` (unique), `outbox_event`.
- `ledger`: `account` (com `version` para locking otimista), `ledger_entry` (partida dobrada, imutável), `outbox_event`, `processed_event`.

Testes: unitários no domínio; integração com Testcontainers (Postgres, Kafka, Redis); ArchUnit; contrato entre `payments` e `ledger` (M7); carga com k6 (M6).

Stack: Java 21 (virtual threads), Spring Boot, JPA/Hibernate, Kafka, Redis, Postgres, Flyway, Resilience4j, Micrometer/OpenTelemetry, Keycloak, Docker Compose, GitHub Actions, Helm/kind, Terraform (AWS).

## 4. Roteiro de evolução

Cada marco deixa o projeto rodando e apresentável; dá para parar em qualquer um. Núcleo básico = M1 a M4. Do M5 em diante, só avança quando o Diego pedir.

| Marco | Entrega | Cobre da vaga |
|---|---|---|
| M1 | `ledger` sozinho: contas, lançamentos, locking otimista, Flyway | Java, Spring Boot, JPA, Hexagonal |
| M2 | `payments` sozinho: REST, `Idempotency-Key`, OpenAPI, `problem+json` | REST, Redis |
| M3 | Kafka entre os dois: outbox, consumidor idempotente, retry e DLQ | Mensageria, assíncrono |
| M4 | CI no GitHub Actions, Docker Compose e `notifications` | CI/CD, DevOps, microsserviços |
| M5 | Observabilidade: métricas, tracing, logs com correlation id | Ambiente crítico |
| M6 | Resiliência (Resilience4j), segurança (JWT/Keycloak), carga com k6 | Alta volumetria |
| M7 | Testes de contrato (Pact ou Spring Cloud Contract) e ArchUnit no CI | Qualidade |
| M8 | Kubernetes local (Helm/kind) e Terraform AWS (validado por plan/LocalStack) | Cloud |
| M9 | ADRs, diagrama C4, README com números reais | Apresentação |

Fora do roteiro por decisão (YAGNI): CQRS, gRPC, SonarQube/PIT.

## 5. Como trabalhamos

O foco do projeto é o Diego aprender; o código é construído em conjunto, não entregue pronto.

- O progresso vive no `CHECKLIST.md`, em itens pequenos, agrupados por marco.
- Um item por vez: o Claude explica o conceito e o porquê antes, o Diego escreve ou revisa o código junto, e o item só é marcado como feito depois que o Diego testa e confirma.
- Nenhum código de marco futuro é escrito antecipadamente, e o Claude não executa o roteiro sozinho.
- Cada marco termina com perguntas de entrevista sobre o que foi construído.
- Commits só ao fim de uma alteração grande, sem `Co-Authored-By`.
- Os testes rodados são os do item em curso; suítes completas ficam para o fechamento do marco.

## 6. Premissas

- Linguagem Java (a vaga pede Java), diferente do DBook em Kotlin.
- Cloud AWS apenas como infraestrutura como código; não há deploy real obrigatório.
- Repositório novo `dbank/`, com git próprio.
