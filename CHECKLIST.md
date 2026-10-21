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
- [ ] **M1.9 Testes de integração.** Testcontainers com Postgres cobrindo repositório e fluxo de transferência. Inclui os testes permanentes que no M1.6 e M1.7 foram provados só por testes descartáveis: ida e volta no banco, rollback real, gravação velha rejeitada com `ConcurrentUpdateException` e transferências concorrentes sem perda de atualização.
  - Aprende: teste de integração sem mock de banco.
- [ ] **M1.10 ArchUnit.** Regras: `domain` e `application` não dependem de `adapter` nem de Spring.
  - Aprende: como a arquitetura é imposta por teste, e não por convenção.
- [ ] **M1.11 Fechamento.** Rodar a suíte completa, revisar o README do serviço e responder as perguntas de entrevista abaixo.

### Perguntas de entrevista do M1

1. Por que dinheiro não usa `double`? Como você representou?
2. O que é partida dobrada e que problema ela resolve?
3. Por que a entidade JPA é separada da entidade de domínio? Qual o custo disso?
4. Locking otimista ou pessimista para saldo? Quando você escolheria o outro?
5. O que o ArchUnit protege que o code review não protege?

## M2 a M9

Detalhados quando o marco anterior fechar. Ver seção 4 da spec.

## Backlog técnico

Decisões adiadas de propósito, com o gatilho para retomá-las.

- **`IllegalArgumentException` virando 400.** Trocar por uma exceção de domínio dedicada quando existir um segundo caso além de "transferir para a mesma conta"; hoje qualquer bug que lance esse tipo viraria 400 em vez de 500.
- **`ApiExceptionHandler` único.** Dividir por assunto (negócio, framework, segurança) quando chegar a segurança (M6) ou passar de uns 8 handlers. Alternativa que escala melhor: exceções de negócio com um `kind` semântico e um único handler.
- **Paginação do extrato por offset.** Trocar por cursor (`created_at`, `id`) no M6, quando medirmos o custo de páginas profundas.
- **`save` de `LedgerTransaction` faz `SELECT` antes.** Id atribuído e sem `@Version` leva o Spring Data ao `merge`. Medir no M6 e, se pesar, usar `Persistable` ou `EntityManager.persist`.
- **Conta `treasury` com saldo finito.** Simplificação de dev, porque `Money` não admite negativo; um ledger real deixa a conta de origem ficar negativa.
