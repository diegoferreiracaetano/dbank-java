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

- Entrega at-least-once, com consumidores idempotentes: o `ledger` guarda o resultado de cada transferência (`transfer_outcome`) e o `payments` usa as transições idempotentes da `Transfer`.
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

## 7. Decisões tomadas no M1

O que o `dbank-ledger` ensinou e que a spec original não previa.

- **Tabelas do ledger:** `account`, `ledger_transaction` e `ledger_entry`. A `ledger_transaction` existe para que a chave primária única feche a corrida da idempotência. `outbox_event` e `processed_event` entram no M3.
- **Locking otimista:** `Account` carrega uma `version` anulável (`null` significa "ainda não gravada"), mapeada com `@Version`, e a coluna entrou pela `V2` do Flyway, sem editar a `V1`.
- **Transação como porta:** a `application` pede `TransactionRunner` e o `SpringTransactionRunner` traduz `OptimisticLockingFailureException` em `ConcurrentUpdateException`. Quem repete a operação é o chamador, e a repetição relê os dados.
- **Defesa em profundidade:** o domínio valida e o banco repete as regras em constraints (`CHECK`, chaves estrangeiras e chave primária).
- **Domínio por responsabilidade:** `shared`, `account` e `ledger`, com dependências no sentido ledger, account, shared. O ArchUnit impõe a regra com uma lista de permitidos para `domain` e `application`.
- **API REST de apoio:** erros em `problem+json` (404 conta inexistente, 409 conflito de versão, 422 saldo insuficiente, 400 pedido inválido). O contrato OpenAPI vive nas interfaces `AccountApi` e `TransactionApi`, as restrições do Bean Validation ficam nelas (um método que implementa não pode redeclará-las), e o arquivo `docs/api/ledger-openapi.yaml` é versionado e protegido por um teste de deriva.
- **Perfil `dev`:** habilita o Swagger UI e cria a conta `treasury` por uma migração repetível, para financiar contas por lançamentos reais.
- **Testes:** unitários com fakes; integração com um Postgres 16 compartilhado via Testcontainers; arquitetura com ArchUnit. O `check` passa a exigir Docker.

## 8. Decisões tomadas no M2

- **Build compartilhado:** a configuração de qualidade, testes e Spring Boot vive no plugin `dbank.spring-service`, em um `build-logic`. Os serviços compartilham configuração de build, nunca código de produção.
- **`Transfer`:** máquina de estados imutável. Repetir o mesmo resultado é inofensivo, e um resultado contraditório é erro. O motivo da falha é tipado. O `version` entrou no M3.
- **Idempotência:** a chave identifica o pedido, e não o conteúdo. A reserva é atômica, com `INSERT ... ON CONFLICT DO NOTHING` declarado como consulta nativa no repositório JPA, e devolve um resultado selado (`Claimed` ou `Existing`). A assinatura vem dos campos do pedido, a validação vem antes da reserva, e reserva e gravação ficam na mesma transação, com a chave estrangeira adiada.
- **Redis:** cache de registros imutáveis, sem invalidação. Só entra o que o banco confirmou, e qualquer falha do Redis cai para o banco. Um decorador `@Primary` fica na frente do adaptador do banco.
- **REST:** `202 Accepted`, e a repetição devolve o mesmo status com o cabeçalho `Idempotency-Replayed`. A mesma chave com outro pedido é 422.
- **Padrão de dados:** acesso só por Spring Data JPA, imposto por ArchUnit nos dois serviços.
- **Bancos:** um por serviço, no mesmo Postgres em desenvolvimento.

## 9. Decisões tomadas no M3

- **Kafka:** KRaft de um nó, fixado em `apache/kafka:3.8.0` (a 3.9.0 recusa o `0.0.0.0` em `advertised.listeners` que o Testcontainers injeta). Mensagens são texto JSON escrito por nós, segundo os esquemas de `docs/events`, e nunca classes Java serializadas, para os serviços não se acoplarem ao código um do outro. Tópicos com 3 partições e a conta de origem como chave.
- **Outbox nos dois serviços:** a mudança de estado e o evento vão na mesma transação, e um relay agendado publica com `FOR UPDATE SKIP LOCKED`, esperando a confirmação do broker. Repetição de pedido nunca gera evento novo. O CDC (Debezium) seria a evolução, ao custo de operar o Kafka Connect.
- **Idempotência no `ledger`:** o caso de uso do Kafka devolve `Settled` ou `Rejected(motivo)`, e o resultado de toda transferência, inclusive das recusadas, é guardado na mesma transação. Sem isso, uma recusa seria reavaliada na repetição e poderia virar sucesso depois de a conta ser recarregada. O `POST /v1/transactions` não mudou.
- **`payments` aplica o resultado** pelas transições idempotentes da `Transfer`, gravando só quando o estado muda. A `Transfer` ganhou `version` (locking otimista). Contradição entre os serviços é erro e vai para a DLQ.
- **Erros de consumo:** os consumidores lançam `MessageConversionException` para mensagem ilegível, em vez de descartá-la. Um `DefaultErrorHandler` retenta o transitório com espera exponencial (1s, ×2, teto de 30s, 2 minutos no total) e publica o que não se resolve em `<tópico>.dlt`. A retentativa é bloqueante, para preservar a ordem. Reprocessar a DLQ é manual.
- **Teste ponta a ponta:** o módulo `dbank-e2e` sobe Postgres, Redis e Kafka por Testcontainers e os dois `bootJar` como processos, e dirige o fluxo só por HTTP, inclusive com o Kafka pausado no momento do pedido. Fica fora do `check`, na tarefa `e2eTest`.
- **Duplicação consciente:** a outbox existe nos dois serviços, com código quase igual. A extração aguarda um terceiro uso (regra dos três).
