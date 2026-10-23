# Roteiro de consulta: M3, `payments` e `ledger` por Kafka

Respostas de referência, para consulta. As perguntas anteriores estão em [m0](m0-qualidade-e-build.md), [m1](m1-ledger.md) e [m2](m2-payments.md).

## Em 1 minuto

O pedido de transferência entra no `payments`, que responde 202 e guarda a transferência como `PENDING`. O dinheiro só se move no `ledger`, e os dois serviços conversam por Kafka, sem transação distribuída. Cada serviço grava o evento numa **outbox**, na mesma transação da mudança de estado, e um relay publica depois. A entrega é "pelo menos uma vez", então cada consumidor é idempotente: o `ledger` guarda o resultado de toda transferência, inclusive das recusadas, e o `payments` aplica o resultado por transições idempotentes, com `version` contra escritas contraditórias. Falha transitória é retentada com espera exponencial, e o que não tem conserto vai para uma fila de mensagens mortas por consumidor (`<tópico>.<serviço>.dlt`) com a mensagem original e a causa. Um teste de ponta a ponta sobe os dois serviços como processos reais e prova o fluxo, inclusive com o Kafka pausado no momento do pedido.

## Números do M3

| O quê | Valor |
|---|---|
| tópicos de negócio / de mensagens mortas | 2 / 2, todos com 3 partições |
| testes unitários (projeto todo) | 146 |
| regras de arquitetura (ArchUnit) | 21 |
| testes de integração (Postgres, Redis e Kafka reais) | 112 |
| testes ponta a ponta (processos reais) | 5 |
| migrações Flyway | 7 (4 no `ledger`, 3 no `payments`) |
| tabelas de apoio ao Kafka | `outbox_event` nos dois, `transfer_outcome` no `ledger` |

## Perguntas e respostas

**1. Por que uma outbox? Por que não publicar direto depois de gravar?**
Gravar no banco e publicar no Kafka são dois sistemas, e nenhuma transação cobre os dois (o problema da escrita dupla). Gravar e depois publicar perde o evento se o processo cair no meio. Publicar e depois gravar manda um evento para uma transferência que pode não existir. Com a outbox, o evento é uma linha na mesma transação da mudança de estado: ou os dois existem, ou nenhum. A publicação vira trabalho de um relay, que pode falhar e tentar de novo sem afetar o pedido.

**2. O Kafka não tem transações? Por que isso não resolve?**
As transações do Kafka cobrem Kafka para Kafka (ler, processar e escrever em tópicos). Elas não incluem um banco relacional. A outbox não é uma cópia da fila: é uma lista de "falta enviar" que mora no banco do serviço.

**3. Relay agendado ou CDC (Debezium)?**
O relay com consulta periódica é simples e mostra o padrão de forma explícita, sem operar o Kafka Connect. O CDC lê o log de transações do Postgres e publica sozinho, com menos latência e sem polling, mas exige operar mais infraestrutura e normalmente ainda usa uma tabela de outbox. Em produção com volume alto, o CDC é a evolução natural.

**4. Para que serve o `FOR UPDATE SKIP LOCKED`?**
Com duas instâncias do serviço, cada uma trava o seu lote e pula as linhas já travadas pela outra, então ninguém publica a mesma linha ao mesmo tempo e ninguém espera. O custo é perder a ordem global entre instâncias. Para transferências, que o `ledger` trata de forma independente e idempotente, isso é aceitável.

**5. Exatamente uma vez ou pelo menos uma vez?**
Pelo menos uma vez, com consumidor idempotente. O offset do Kafka e a gravação no banco não são atômicos, então uma mensagem pode chegar duas vezes. Em vez de perseguir "exatamente uma vez" na infraestrutura, o efeito é tornado seguro em quem consome.

**6. Como o `ledger` garante que repetir a mensagem não move o dinheiro duas vezes?**
Guarda o resultado de cada transferência numa tabela com a chave primária no id da transferência, na mesma transação do lançamento. Ao receber de novo, devolve o resultado guardado.

**7. Por que guardar também as recusas?**
Sem isso, uma transferência recusada por saldo insuficiente seria reavaliada na repetição. Se a conta tivesse sido recarregada nesse meio tempo, a repetição viraria sucesso, e o `payments` já teria marcado `FAILED`. Guardar a recusa garante que o `ledger` sempre responda a mesma coisa.

**8. Por que o caso de uso devolve `Rejected` em vez de lançar exceção?**
Saldo insuficiente é uma resposta válida, e não um erro de processamento. Se fosse exceção, o consumidor reentregaria a mensagem para sempre. Há também uma armadilha de transação: uma exceção que cruza a fronteira transacional marca a transação como "só rollback", e o `commit` posterior falha, mesmo que a exceção tenha sido capturada. Por isso a regra de negócio é avaliada dentro de uma única transação, e a recusa é um valor, não uma exceção.

**9. Como o `payments` aplica o resultado?**
Pelas transições idempotentes da `Transfer`: concluir uma transferência já concluída não faz nada, e só grava quando o estado muda. Um resultado contraditório (concluir uma transferência que falhou) é um sinal de que os dois serviços discordam, e vai para a fila de mensagens mortas em vez de ser aplicado em silêncio.

**10. Para que o `version` na `Transfer`?**
Duas mensagens contraditórias quase simultâneas deixariam a última escrita vencer em silêncio. Com locking otimista, a segunda falha, a mensagem é reentregue e na releitura a contradição aparece.

**11. Como funciona a retentativa e o que vai para a DLQ?**
Erro transitório (banco fora, conflito de versão) é retentado com espera exponencial (1s, dobrando, teto de 30s, no máximo 2 minutos). Erro permanente (JSON ilegível, evento inesperado, contradição, resultado de transferência inexistente) não adianta repetir e vai direto para a fila de mensagens mortas do consumidor (`<tópico>.<serviço>.dlt`, para dois consumidores do mesmo tópico não misturarem as falhas). Se as tentativas se esgotarem, também vai. Lá fica a mensagem intacta, com cabeçalhos que dizem o tópico de origem e a causa.

**12. Retentativa bloqueante ou com tópicos de retry?**
Bloqueante: a partição espera durante o backoff, o que preserva a ordem e é mais simples, ao custo de atrasar as mensagens atrás dela. O teto total precisa ficar abaixo do `max.poll.interval.ms`, senão o consumidor é expulso do grupo. Tópicos de retry (`@RetryableTopic`) evitam o bloqueio, mas perdem a ordem e acrescentam tópicos para operar.

**13. Por que a mensagem é um texto JSON e não uma classe Java serializada?**
A serialização JSON do Spring grava o nome da classe Java num cabeçalho, o que acoplaria os serviços ao código um do outro. Com texto JSON e esquemas versionados em `docs/events`, o contrato é o JSON. Mudança compatível mantém a versão, e mudança incompatível cria um tópico `.v2` que convive com o antigo.

**14. Por que a conta de origem é a chave da mensagem?**
Todas as mensagens de uma mesma conta caem na mesma partição, o que mantém a ordem entre elas. Duas mensagens com a mesma chave nunca se ultrapassam.

**15. Como você provou o fluxo inteiro?**
Um módulo de ponta a ponta sobe Postgres, Redis e Kafka por Testcontainers e os dois serviços como processos reais, e usa só HTTP. Os cenários são: concluída, saldo insuficiente, conta inexistente, mesma chave duas vezes, e uma transferência pedida com o Kafka pausado, que fica `PENDING` e conclui quando o Kafka volta, com o dinheiro movido uma vez só. Fica fora do `check` por ser lento, e o CI o roda em um passo separado.

**16. Algo deu errado no caminho?**
Duas coisas úteis. A imagem `apache/kafka:3.9.0` recusava o endereço `0.0.0.0` que o Testcontainers injeta, e a causa só apareceu lendo o log do container, não o resumo do erro; fixei a 3.8.0. E um `check` demorou 6 horas porque o Mac dormiu no meio da execução e o container estourou o prazo: a falha era do ambiente, e a reexecução com o computador acordado provou isso.

## Limites que devo reconhecer

- Mensagem na DLQ é uma transferência presa em `PENDING`, e hoje só se descobre lendo o tópico. Faltam alerta e uma ferramenta de reprocesso.
- As linhas enviadas da outbox nunca são apagadas.
- Com várias instâncias do relay não há ordem global entre eventos.
- A retentativa é bloqueante: uma mensagem em espera atrasa as seguintes da mesma partição.
- Reprocessar a DLQ é manual.
- A outbox existe nos dois serviços com código quase igual; a extração aguarda um terceiro uso.
- O ponta a ponta não cobre a queda do processo do `payments` entre o commit e a publicação, só o broker fora do ar.

## Palavras que o entrevistador espera ouvir

*escrita dupla*, *transactional outbox*, *at-least-once*, *consumidor idempotente*, *dead-letter queue*, *backoff exponencial*, *SKIP LOCKED*, *CDC*, *consistência eventual*, *saga coreografada*, *chave de partição*, *head-of-line blocking*, *locking otimista*, *contrato de evento versionado*.
