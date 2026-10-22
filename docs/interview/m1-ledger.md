# Roteiro de entrevista: M1, `dbank-ledger`

Respostas de referência para estudar. A ideia é reescrever cada uma com as suas palavras e treinar em voz alta, em cerca de 30 segundos.

## Apresentação em 1 minuto

O Dbank é o núcleo transacional de um banco digital em Java 21 e Spring Boot. O primeiro serviço, o `ledger`, guarda contas e lançamentos de partida dobrada imutáveis e move dinheiro entre contas com quatro garantias: o dinheiro não some nem duplica, repetir a mesma transação não move dinheiro duas vezes (idempotência), duas operações concorrentes na mesma conta não se sobrescrevem (locking otimista) e um erro no meio desfaz tudo (transação).

A arquitetura é Hexagonal e é imposta por testes de arquitetura (ArchUnit). A persistência é Postgres com Flyway, a API é REST com erros em `problem+json` e contrato OpenAPI versionado, e os testes de integração rodam contra um Postgres real com Testcontainers, incluindo transferências concorrentes. Os próximos passos são o serviço de pagamentos, a mensageria com Kafka e outbox, e o CI/CD.

## Números do M1

| O quê | Quantidade |
|---|---|
| testes unitários | 53 |
| regras de arquitetura (ArchUnit) | 10 |
| testes de integração (Postgres real) | 34 |
| migrações Flyway | 2 (`V1` esquema, `V2` versão da conta) |
| endpoints | 4 |

## Perguntas e respostas

**1. Por que dinheiro não usa `double`? Como você representou?**
O `double` é binário e não representa 0,1 exatamente: `0.1 + 0.2` dá `0.30000000000000004`. Em milhões de lançamentos o erro se acumula e o saldo deixa de fechar. Representei em `long` com centavos, dentro de um `record Money`: a conta é inteira e exata, `Math.addExact` falha em overflow, o construtor recusa valor negativo e o tipo próprio impede misturar centavos com outro `long`.

**2. O que é partida dobrada e que problema ela resolve?**
Cada movimentação gera ao menos um débito e um crédito, e a soma dos débitos é igual à dos créditos na mesma transação. O dinheiro não aparece nem some, e o saldo é auditável, porque é a soma dos lançamentos. No código, `LedgerTransaction` valida o balanço no construtor, e os lançamentos são imutáveis: erro se corrige com novo lançamento.

**3. Por que a entidade JPA é separada da de domínio? Qual o custo?**
O JPA exige construtor sem argumentos, campos mutáveis e classe não-`record`, e o domínio quer imutabilidade e invariantes no construtor. Misturar acopla o domínio ao Hibernate e ao esquema. O custo são os mappers: mais código, risco de esquecer um campo (mitigado com testes) e uma conversão a cada leitura e gravação. Num CRUD sem regra de negócio não compensa.

**4. Locking otimista ou pessimista para saldo? Quando o outro?**
Otimista para contas comuns: uma `version` e `UPDATE ... WHERE version = ?`, sem segurar lock e sem deadlock, e o chamador refaz a operação inteira se houver conflito. Pessimista (`SELECT ... FOR UPDATE`) quando a disputa é alta e previsível, como uma conta de lojista muito movimentada, travando as contas sempre em ordem de id. Outra saída é a atualização atômica no SQL (`SET balance = balance - ? WHERE balance >= ?`).

**5. O que o ArchUnit protege que o code review não protege?**
O review olha o diff, e a regra de arquitetura é uma propriedade do grafo inteiro de dependências. O ArchUnit roda a cada build, não cansa e, com lista de permitidos, reprova até a dependência que ninguém previu. Protege contra a erosão arquitetural. O review continua necessário para nomes, clareza e design.

**6. Por que a porta se chama `AccountRepository` e não `AccountJpaRepository`?**
A porta é definida pelo que o núcleo precisa, na linguagem do domínio, e não pelo que a tecnologia oferece. A `application` declara a interface e o adapter a implementa, o que inverte a dependência. Trocar o JPA não toca em `domain` nem em `application`, e os testes usam um fake em memória.

**7. Por que, depois de um `ConcurrentUpdateException`, a nova tentativa precisa reler a conta?**
A decisão "há saldo suficiente?" foi tomada com um dado que já não vale. Gravar de novo só com a versão nova passaria pelo conflito e reintroduziria o lost update, escondido. A retentativa refaz a transação inteira (reler, revalidar, regravar), e isso é seguro porque o `TransactionRunner` já desfez tudo.

**8. Por que saldo insuficiente é 422 e conflito de versão é 409?**
O 422 diz que o pedido é válido, mas uma regra de negócio o impede, e repetir igual continua falhando até o saldo mudar. O 409 diz que o estado mudou no meio do caminho e que tentar de novo, relendo, pode funcionar. O cliente precisa de status diferentes porque a conduta é diferente: um não repete, o outro repete.

**9. Por que o teste de concorrência afirma o estado final e não conta os conflitos?**
O número de conflitos depende do agendamento das threads, então um teste que o afirma é instável. O estado final é determinístico: cada thread retenta até conseguir, e o saldo termina em exatamente 200, com 8 lançamentos. Teste bom afirma o que sempre tem que ser verdade.

**10. Por que Postgres real nos testes de integração, e não H2?**
O H2 não é o Postgres: tipos como `uuid` e `timestamptz`, mensagens de constraint, `ON CONFLICT` e a semântica de lock e de isolamento mudam. Um teste verde no H2 pode falhar em produção. O Testcontainers entrega o mesmo motor, descartável, ao custo do Docker e de uns 30 segundos de partida, pagos uma vez por execução com um container compartilhado.

**11. Por que o contrato da API fica num arquivo no Git, se o `springdoc` já o gera?**
O arquivo torna qualquer mudança de API visível no code review, e quem consome pode fixar uma versão. O teste de deriva transforma "esqueci de atualizar a documentação" em falha de build. O arquivo também é a base para gerar clientes, aplicar lint e publicar a documentação no CI.

**12. Por que a regra do `domain` diz "só pode depender de..." e não "não pode depender de Spring"?**
A lista de permitidos falha para o desconhecido: uma biblioteca nova no domínio reprova sem que ninguém precise lembrar de atualizar uma lista de proibidos. Negar por padrão é o padrão mais seguro.

**13. Por que `long` em centavos e não `BigDecimal`?**
O ledger só soma, subtrai e compara, e isso é exato, rápido e sem as armadilhas de escala, de `equals` sensível a casas decimais e de arredondamento do `BigDecimal`. Para juros, taxas ou câmbio eu calcularia em `BigDecimal` com `RoundingMode` explícito e converteria para centavos na borda. Com várias moedas, o `Money` passaria a levar a moeda.

## Limites que devo reconhecer

- Uma única moeda, sem câmbio.
- Sem autenticação: entra no M6.
- O `ledger` ainda é chamado por REST de apoio: a ligação por eventos (Kafka, outbox) é o M3.
- Paginação do extrato por offset: o cursor fica para quando medirmos páginas profundas.
- A conta `treasury` do perfil `dev` tem saldo finito, por simplificação.

## Palavras que o entrevistador espera ouvir

*lost update*, *invariante*, *defesa em profundidade*, *idempotência*, *inversão de dependência*, *erosão arquitetural*, *flaky*, *fail by default*.
