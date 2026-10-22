# Roteiro de entrevista: M2, `dbank-payments`

Respostas de referência para estudar. A ideia é reescrever cada uma com as suas palavras e treinar em voz alta, em cerca de 30 segundos. As perguntas do M1 estão em [m1-ledger.md](m1-ledger.md).

## Apresentação em 1 minuto

O Dbank é o núcleo transacional de um banco digital, em Java 21 e Spring Boot. O `ledger` guarda contas e lançamentos de partida dobrada imutáveis, com idempotência, locking otimista e atomicidade. O `payments` é a porta de entrada das transferências: recebe o pedido, responde 202 e guarda o estado, que só se resolve quando o `ledger` responder.

O ponto central do `payments` é a idempotência. O app envia uma chave por intenção do usuário, e uma repetição devolve a transferência original em vez de criar outra. A reserva da chave é atômica no Postgres, e o Redis entra na frente apenas como cache de registros imutáveis, sem nunca decidir nada: se ele cair, o serviço continua pelo banco. Os dois serviços seguem a Hexagonal imposta por ArchUnit, têm contrato OpenAPI versionado e são testados contra Postgres e Redis reais. O próximo passo é ligá-los por Kafka com outbox.

## Números do M2

| O quê | `payments` | Projeto todo |
|---|---|---|
| testes unitários | 52 | 105 |
| regras de arquitetura (ArchUnit) | 10 | 21 |
| testes de integração (Postgres e Redis reais) | 44, em 12 classes | 78 |
| migrações Flyway | 1 | 3 |
| endpoints | 2 | 6 |

## Perguntas e respostas

**1. Por que o pedido devolve 202 e não 201?**
O 201 diz que o recurso foi criado e está pronto. Aqui a transferência ainda não aconteceu: o dinheiro só se move no `ledger`, de forma assíncrona. O 202 diz "aceito para processamento", e o cliente acompanha pelo `GET`, que mostra `PENDING` até a resposta. No Android é o `WorkManager`: o `enqueue` volta na hora e você observa o estado.

**2. Como funciona a chave de idempotência? Quais são as quatro situações?**
O app gera uma chave por intenção e a reenvia igual a cada tentativa. Chave nova cria a transferência e registra a chave. Chave repetida com o mesmo pedido devolve a original. Chave repetida com pedido diferente é recusada com 422. Chaves diferentes com pedidos idênticos criam duas transferências.

**3. Por que a chave identifica o pedido e não o conteúdo?**
Porque a idempotência protege contra a repetição acidental da mesma intenção, e não contra intenções parecidas. Se deduplicássemos por conteúdo, quem quisesse pagar duas contas iguais de R$ 100 teria a segunda engolida em silêncio, o que é pior que o duplicado que queríamos evitar.

**4. Por que a reserva é um `INSERT ... ON CONFLICT` e não um `SELECT` seguido de `INSERT`?**
Entre o `SELECT` e o `INSERT` existe uma janela em que outro pedido faz a mesma verificação, e os dois inserem. Com a operação única, quem serializa é o índice único do banco, que vê todos os pedidos de todas as instâncias. Há um bônus: no Postgres uma violação de constraint aborta a transação, mas o `ON CONFLICT DO NOTHING` não gera erro, então a transação segue saudável. No Room é o `OnConflictStrategy.IGNORE`.

**5. Para que serve a chave estrangeira adiada?**
O caso de uso reserva a chave antes de salvar a transferência, e uma chave estrangeira normal reclamaria porque a transferência ainda não existe. Com `DEFERRABLE INITIALLY DEFERRED`, o Postgres só confere no commit, quando as duas linhas já estão na mesma transação. Em vez de inverter o código para agradar o banco, ensinamos o banco a verificar na hora certa.

**6. Por que o Redis é só um cache, e por que só se guarda o que o banco confirmou?**
O registro "chave, assinatura, transferência" nunca muda depois de criado, então o cache não precisa de invalidação. O Redis não decide nada, porque dois sistemas decidindo a mesma coisa divergem. E só guardamos o que o banco confirmou como "já existia", porque uma reserva recém-feita vive numa transação que pode ser desfeita, e o cache registraria uma verdade que o banco depois negaria.

**7. Por que a queda do Redis não derruba o pedido?**
Porque cache é uma otimização, e a verdade mora no banco. Qualquer erro do Redis vira um aviso no log e o fluxo segue como se o cache estivesse vazio. Se o cache derrubasse o pedido, o Redis passaria de acelerador a ponto único de falha. Isso está provado por teste com o acesso ao Redis quebrado.

**8. Por que as transições da `Transfer` são idempotentes, e o que é uma contradição?**
A entrega dos eventos é "pelo menos uma vez", então a mesma notícia pode chegar duas vezes, e tratá-la como erro quebraria o fluxo normal. Já concluir uma transferência que falhou não é duplicata, é um sinal de que dois serviços discordam, e isso tem que aparecer. A `Transfer` é imutável, e o `switch` sobre o estado é exaustivo: um estado novo no enum faz o projeto deixar de compilar até a regra ser decidida.

**9. Por que não há chave estrangeira entre o `payments` e as contas do `ledger`?**
Cada serviço tem o seu banco, e uma restrição não atravessa bancos. A integridade entre serviços é por contrato: se a conta não existir, o `ledger` recusa, e a transferência vira `FAILED` com `ACCOUNT_NOT_FOUND`. Acoplar pelo esquema tiraria a independência dos serviços.

**10. Por que cada serviço tem o seu `Money` em vez de uma biblioteca compartilhada?**
Uma biblioteca de domínio compartilhada acopla os deploys: mudar o `Money` obrigaria a republicar tudo, que é o monólito distribuído. O `Money` do `ledger` soma e subtrai, e o do `payments` nunca faz conta. Duplicar umas 30 linhas é mais barato que escolher a abstração errada. Compartilhamos a configuração de build, e os contratos entre serviços, nunca o modelo de domínio.

**11. Por que JPA com consulta nativa, e como o ArchUnit garante o padrão?**
O JPA é a ferramenta para o agregado, e para a única operação que ele não expressa (a reserva atômica) usamos SQL declarado com `@Query` dentro do repositório, em vez de uma segunda tecnologia de acesso. Uma regra de ArchUnit proíbe `java.sql`, `javax.sql` e `org.springframework.jdbc` em código de produção, nos dois serviços. O Konsist faria o mesmo no Kotlin, mas só analisa Kotlin.

## Limites que devo reconhecer

- A transferência fica `PENDING` para sempre: a ligação com o `ledger` por Kafka é o M3.
- A chave de idempotência é global: sem autenticação, dois clientes que escolhessem a mesma chave colidiriam.
- As chaves não expiram no Postgres, só no cache.
- A `Transfer` não tem `version`: duas transições contraditórias quase simultâneas deixariam a última escrita vencer.
- O cache só é preenchido na primeira repetição, e não na reserva nova.
- O health agrega o Redis, então um Redis fora aparece como `DOWN` mesmo com o serviço funcionando degradado. Separar liveness de readiness é trabalho de observabilidade.

## Palavras que o entrevistador espera ouvir

*idempotência*, *chave de idempotência*, *janela de corrida*, *atomicidade*, *cache-aside*, *dado imutável*, *degradação graciosa*, *202 Accepted*, *máquina de estados*, *consistência eventual*, *bounded context*, *monólito distribuído*, *restrição adiada*.
