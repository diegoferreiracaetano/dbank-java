# dbank-test-support

Código de teste compartilhado pelos serviços. É só dependência de teste: o `dbank.spring-service` o coloca no `testImplementation`, então nenhum serviço o carrega em produção.

| Classe | Para que serve |
|---|---|
| `Containers` | as imagens de Postgres, Redis e Kafka num lugar só (o Kafka fica fixado em `apache/kafka:3.8.0`) |
| `DeadLetterCollector` | lê a fila de mensagens mortas de um serviço; o tópico vem da propriedade `dbank.test.dead-letter-topic`, que o `IntegrationTestBase` de cada serviço define |
| `HexagonalRules` | as regras de arquitetura do `ledger` e do `payments` (domínio puro, adapters que não se conhecem, entidades e controllers nos seus adapters) |
| `DataAccessRules` | a regra de acesso a dados por Spring Data JPA, sem JDBC direto, que vale para os três serviços |

Os serviços usam as regras com `ArchTests.in(HexagonalRules.class)` dentro do seu próprio `ArchitectureTest` e acrescentam só o que é específico deles.

## O que não entra aqui

Modelo de domínio (`Money`, transições) e a outbox continuam em cada serviço. Compartilhar o domínio acoplaria os deploys, e a outbox tem só dois usos, abaixo da regra dos três.
