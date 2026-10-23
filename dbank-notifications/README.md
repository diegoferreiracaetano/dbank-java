# dbank-notifications

Avisa o cliente do resultado de cada transferência. Consome `ledger.transfer-result.v1` em um grupo de consumidores próprio (`dbank-notifications`), então recebe uma cópia independente de cada mensagem, ao lado do `payments`. O aviso é simulado: um adaptador escreve no log, atrás de uma interface.

É um serviço em **camadas**, e não Hexagonal: o que ele faz é receber um evento, montar um texto e enviá-lo, sem regra de negócio que peça isolamento. A dependência vai só de cima para baixo, e o ArchUnit a impõe:

```
messaging/     listener Kafka: lê o JSON e chama o serviço
service/       NotificationService, o texto do aviso e o remetente (interface + adaptador de log)
persistence/   entidade e repositório (Spring Data JPA)
config/        tópicos, tratamento de erros e ligação dos beans
```

## Fluxo de um evento

1. O listener lê `TransferSettled` ou `TransferRejected` e o converte num `TransferResultEvent`.
2. O serviço compõe o texto (concluída, ou recusada com o motivo: saldo insuficiente, conta inexistente; um motivo novo cai num texto genérico).
3. Numa **única transação**, ele grava a notificação (`INSERT ... ON CONFLICT (event_id) DO NOTHING`) e chama o remetente.

## Idempotência

A chave primária da tabela `notification` é o `eventId`. Uma mensagem repetida bate na chave, o `INSERT` devolve zero linhas e o serviço não envia de novo. Não há uma tabela separada de eventos processados.

Como gravar e enviar estão na mesma transação, uma falha do remetente desfaz a gravação, e a retentativa começa do zero. O limite é inevitável: se o envio der certo e o `commit` falhar, a retentativa avisa o cliente de novo. Um remetente real precisaria aceitar uma chave de idempotência própria.

## Falhas

Erro transitório é retentado com espera exponencial (`dbank.kafka.retry`: 1s, dobrando, teto de 30s, 2 minutos no total). Mensagem ilegível ou de tipo desconhecido vai para `ledger.transfer-result.v1.notifications.dlt`, com a mensagem original e a causa nos cabeçalhos. Se as tentativas se esgotarem, a mensagem também vai para lá, e nenhuma notificação fica gravada. Detalhes em [`docs/events`](../docs/events/README.md).

## Banco

O Flyway cria a tabela `notification` em `src/main/resources/db/migration`. O serviço usa o banco `notifications`, no mesmo Postgres do compose. Em um volume novo, o script de `docker/postgres` o cria. Em um volume que já existe, crie-o uma vez:

```bash
docker compose exec postgres psql -U dbank -d postgres -c "CREATE DATABASE notifications"
```

## Testes

- **Unitários** (`src/test`): o texto do aviso, o serviço (com Mockito), a leitura da mensagem e o adaptador de log, mais as regras de arquitetura (camadas, ciclos, sem JDBC direto, entidades na camada de persistência).
- **Integração** (`src/integrationTest`): Postgres e Kafka reais via Testcontainers, cobrindo o aviso de concluída e de recusada, a mesma mensagem duas vezes, lixo indo para a fila de mensagens mortas, a retentativa de uma falha transitória e uma falha que nunca passa.

O `dbank-e2e` ainda não observa o `notifications`, porque o serviço não tem API de consulta.
