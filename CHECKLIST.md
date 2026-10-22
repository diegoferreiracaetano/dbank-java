# Dbank — Checklist

Um item por vez. Marcar `[x]` só depois que o Diego testou e confirmou. Spec: `docs/superpowers/specs/2026-10-20-dbank-design.md`.

## M0 — Setup de qualidade

Meta: um único comando, `./gradlew check`, que o CI vai chamar sem lógica própria. Configuração direto no módulo; vira convention plugin quando o `dbank-payments` entrar (M2).

- [x] **M0.1 Catálogo de versões.** `gradle/libs.versions.toml` com plugins, ferramentas e versões.
  - Aprende: version catalog é recurso do Gradle (o Android o adotou), com uma fonte única de versões.
- [x] **M0.2 Compilador estrito.** `-Xlint:all -Werror`. Já achou o `serialVersionUID` que faltava na exceção.
- [x] **M0.3 Spotless (Palantir).** Formatação verificada no `check`; `spotlessApply` corrige.
- [x] **M0.4 Checkstyle e SpotBugs.** Regras enxutas em `config/checkstyle/`; SpotBugs com relatório HTML e XML.
  - Aprende: SpotBugs analisa bytecode e cobre a falta de null-safety do Java.
- [x] **M0.5 JaCoCo.** Mínimo de 80% de linhas e 70% de branches em `domain` e `application`.
- [x] **M0.6 `integrationTest` separado.** Equivalente ao `androidTest`, com containers no lugar do emulador. Ainda sem testes (entram no M1.9).

## M1 — `dbank-ledger` sozinho

Meta: um serviço Hexagonal que guarda contas e lança débitos e créditos em partida dobrada, com saldo seguro sob concorrência. Sem Kafka e sem `payments` ainda.

- [x] **M1.1 Esqueleto.** Projeto Gradle com Java 21 e Spring Boot, módulo `dbank-ledger`, pacote `com.diegoferreiracaetano.dbank.ledger`, Postgres via Docker Compose, aplicação subindo.
  - Aprende: estrutura de módulos, por que um banco por serviço.
- [x] **M1.2 Domínio: `Money` e `Account`.** Value object de dinheiro (centavos em `long`, sem `double`), conta com regra de saldo. Java puro, sem Spring.
  - Aprende: por que nunca usar ponto flutuante para dinheiro, e como um domínio isolado é testado sem framework.
- [x] **M1.3 Domínio: `LedgerEntry` e partida dobrada.** Toda movimentação gera um débito e um crédito de mesmo valor; a soma dos lançamentos é zero. Lançamentos são imutáveis.
  - Aprende: o que é ledger e por que não se atualiza saldo "no escuro".
- [x] **M1.4 Portas e caso de uso.** `PostTransferUseCase` (porta de entrada) e `AccountRepository` / `LedgerEntryRepository` (portas de saída), com teste usando fakes.
  - Aprende: o que são portas e adaptadores na prática.
- [x] **M1.5 Flyway.** `V1` com `account` e `ledger_entry`, constraints e índices justificados.
  - Aprende: migrações versionadas, constraints como última linha de defesa.
- [x] **M1.6 Adapter JPA.** Entidades JPA separadas do domínio, mapper entre os dois, implementação das portas de saída.
  - Aprende: por que a entidade JPA não é a entidade de domínio.
- [x] **M1.7 Locking otimista.** Campo `version` em `account`, tratamento de conflito e teste de concorrência com duas transferências na mesma conta.
  - Aprende: otimista vs pessimista, e o que acontece sob corrida.
- [x] **M1.8 Adapter REST de apoio.** Criar conta, consultar saldo e extrato, com OpenAPI e erros em `problem+json`. Serve para exercitar o serviço até o Kafka entrar no M3.
  - Aprende: adapter de entrada fino, sem regra de negócio.
- [x] **M1.9 Testes de integração.** Testcontainers com Postgres cobrindo repositório e fluxo de transferência. Inclui os testes permanentes que no M1.6 e M1.7 foram provados só por testes descartáveis: ida e volta no banco, rollback real, gravação velha rejeitada com `ConcurrentUpdateException` e transferências concorrentes sem perda de atualização.
  - Aprende: teste de integração sem mock de banco, e por que não usar H2 no lugar do Postgres.
- [x] **M1.10 Documentação da API.** Contrato OpenAPI completo (título, versão, tags, resumos, descrições, exemplos e os erros `problem+json` de cada operação, corrigindo o `201` do `POST /v1/accounts`), Swagger UI habilitado só no perfil `dev` com exemplos prontos para testar, o contrato versionado em `docs/api/ledger-openapi.yaml` com um teste que reprova se divergir, e um README com o passo a passo para rodar e testar.
  - Aprende: contrato como fonte de verdade e como impedir que a documentação envelheça.
- [x] **M1.11 ArchUnit.** Regras: `domain` e `application` não dependem de `adapter` nem de Spring.
  - Aprende: como a arquitetura é imposta por teste, e não por convenção.
- [x] **M1.12 Fechamento.** Rodar a suíte completa, revisar o README do serviço e responder as perguntas de entrevista abaixo.

### Perguntas de entrevista do M1

1. Por que dinheiro não usa `double`? Como você representou?
2. O que é partida dobrada e que problema ela resolve?
3. Por que a entidade JPA é separada da entidade de domínio? Qual o custo disso?
4. Locking otimista ou pessimista para saldo? Quando você escolheria o outro?
5. O que o ArchUnit protege que o code review não protege?

## M2 — `dbank-payments` sozinho

Meta: um segundo serviço Hexagonal que recebe pedidos de transferência com `Idempotency-Key` e guarda o estado (`PENDING`, `COMPLETED`, `FAILED`). Ainda sem Kafka: a ligação com o `ledger` é o M3. Os serviços não compartilham código, só contratos.

- [x] **M2.1 Convenções de build compartilhadas.** `build-logic` com um plugin de convenção (qualidade, testes e Spring Boot), aplicado primeiro ao `ledger`. Vem antes do segundo serviço para ele nascer sem duplicar configuração.
  - Aprende: convention plugin e included build, a mesma ideia do `build-logic` do Now in Android.
- [x] **M2.2 Esqueleto do `dbank-payments`.** Módulo, porta 8082, banco `payments` no mesmo Postgres do compose, Redis no compose, health `UP`.
- [x] **M2.3 Domínio.** `Transfer` com máquina de estados (`PENDING` para `COMPLETED` ou `FAILED`), `Money` e ids próprios.
  - Aprende: por que cada serviço tem seu próprio `Money` em vez de uma biblioteca compartilhada.
- [x] **M2.4 Casos de uso e portas.** Solicitar e consultar transferência, portas de repositório e de idempotência, fakes.
- [x] **M2.5 Flyway e adapter JPA.** `transfer` e `idempotency_key` com unicidade.
- [x] **M2.6 Idempotência com Redis.** `Idempotency-Key` com `SET NX` e TTL como caminho rápido, constraint única no Postgres como garantia final, e detecção de reuso da chave com corpo diferente.
  - Aprende: cache como otimização e o banco como fonte da verdade.
- [x] **M2.7 Adapter REST.** `POST /v1/transfers` (202), `GET /v1/transfers/{id}` e `problem+json`.
- [ ] **M2.8 Testes de resiliência do cache.** Os testes de integração com Postgres e Redis e as regras de arquitetura do `payments` já entraram nos itens M2.5 a M2.7. Faltam duas provas de ponta a ponta: o pedido continua funcionando com o Redis falhando, e a chave expirada no Redis volta a ser respondida pelo banco.
- [ ] **M2.9 Documentação.** OpenAPI versionado, README do serviço e README da raiz.
- [ ] **M2.10 Fechamento.** Clone limpo e perguntas de entrevista do M2.

## M3 a M9

Detalhados quando o marco anterior fechar. Ver seção 4 da spec.

## Backlog técnico

Decisões adiadas de propósito, cada uma com o marco em que volta.

| Item | Por que ficou | Volta em |
|---|---|---|
| Convention plugin para a configuração de build | com um só módulo não há o que compartilhar; com o segundo serviço a configuração se repete | M2 |
| `IllegalArgumentException` virando 400 | só um caso hoje (transferir para a mesma conta); qualquer bug que lance esse tipo viraria 400 em vez de 500 | M2, se o `payments` trouxer um segundo caso: criar uma exceção de domínio dedicada |
| `ApiExceptionHandler` único | quatro handlers da mesma natureza; dividir por assunto, ou mapear por um `kind` semântico | M6 (segurança), ou ao passar de uns 8 handlers |
| Paginação do extrato por offset | simples e correta; cara em páginas muito profundas | M6, com cursor (`created_at`, `id`) depois de medir |
| `save` de `LedgerTransaction` faz `SELECT` antes | id atribuído e sem `@Version` leva o Spring Data ao `merge` | M6, medir; se pesar, `Persistable` ou `EntityManager.persist` |
| Cobertura dos testes de integração fora do JaCoCo | o mínimo mede só `domain` e `application`, que os unitários já cobrem | M4, junto com o CI |
| Lint do contrato (Spectral) e publicação estática da documentação | dependem do CI | M4 |
| Conta `treasury` com saldo finito | simplificação de dev: `Money` não admite negativo, e um ledger real deixa a conta de origem ficar negativa | permanente, só no perfil `dev` |
| Testcontainers fixado em 1.21.4 | o Docker Engine 29 exige API 1.40 ou superior, e a 1.21.3 do BOM do Spring Boot 3.5.6 falava a 1.32 | remover o `extra["testcontainers.version"]` quando o BOM trouxer versão compatível |
| Limpeza das chaves de idempotência expiradas no Postgres | as chaves não expiram no banco, e só o cache (M2.6) terá prazo | M5 ou M6, com um job agendado que apague o que passou do prazo de retenção |
| Chave de idempotência global | sem autenticação, dois clientes que escolhessem a mesma chave colidiriam | M6: passa a valer por cliente |
