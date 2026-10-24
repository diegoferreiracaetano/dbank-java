# Roteiro de consulta: M4, CI, imagens e `dbank-notifications`

Respostas de referência, para consulta. As perguntas anteriores estão em [m0](m0-qualidade-e-build.md), [m1](m1-ledger.md), [m2](m2-payments.md) e [m3](m3-kafka.md).

## Em 1 minuto

O pipeline do GitHub Actions não tem lógica própria: ele só chama os comandos do Gradle, o `check` e, depois dele, o `e2eTest`, mais o lint dos contratos com o Spectral. Cada serviço tem uma imagem Docker feita por um único `Dockerfile` parametrizado, em dois estágios, com o jar em camadas e um usuário sem privilégios, e o compose sobe tudo sob um profile. Entrou um terceiro serviço, o `notifications`, em camadas e não Hexagonal, que consome o resultado das transferências em um grupo de consumidores próprio, com idempotência pelo `eventId` e uma fila de mensagens mortas por consumidor. Com três serviços, a regra dos três pagou a extração do plugin de qualidade e do módulo de apoio a testes.

## Números do M4

| O quê | Valor |
|---|---|
| serviços / imagens Docker | 3 / 3 |
| testes unitários (projeto todo, sem arquitetura) | 168 |
| regras de arquitetura (ArchUnit) | 25 |
| testes de integração | 119 |
| testes ponta a ponta | 5 |
| jobs do CI | 3 (contratos, build, ponta a ponta) |
| filas de mensagens mortas | 3, uma por consumidor |

## Perguntas e respostas

**1. Por que o CI chama só `./gradlew check`?**
Porque tudo (formatação, Checkstyle, SpotBugs, testes, cobertura) já está no build. Se o YAML repetisse cada ferramenta, o build local e o do CI divergiriam, e uma ferramenta nova só valeria num dos dois. Com isso, o que passa na sua máquina passa no CI, e uma diferença é de ambiente.

**2. Por que o ponta a ponta fica fora do `check` e num job à parte?**
Sobe três containers e duas JVMs, então é lento. Ele só começa depois que o `build` passa (`needs`), para não gastar tempo num código que já reprovou.

**3. Para que o lint dos contratos?**
O OpenAPI é a fonte de verdade da API e é versionado. O Spectral falha em qualquer aviso, então um contrato sem resumo ou com erro de forma reprova o CI. Desliguei só a regra do contato, que não faz sentido sem uma organização por trás.

**4. Por que um Dockerfile em dois estágios, com camadas e usuário sem privilégios?**
O primeiro estágio compila dentro do Docker, então o build não depende de nada instalado na máquina, e é descartado. A imagem final tem só o JRE e o jar dividido em camadas: dependências quase nunca mudam e o código muda sempre, então uma reconstrução refaz só a camada pequena. O usuário sem privilégios limita o estrago se o processo for comprometido.

**5. Por que o Kafka precisou de dois endereços no compose?**
O broker se anunciava como `localhost:9092`, que serve a quem roda na máquina, mas dentro de um container `localhost` é o próprio container. O segundo endereço (`kafka:29092`) é o da rede do compose, e o primeiro continua valendo para o `bootRun`.

**6. Que problemas apareceram ao construir as imagens?**
Três, e todos com causa real: o daemon do Gradle estava fixado no JDK 25, que não existe na imagem de build (corrigi para 21, o mesmo do toolchain, o que também evitava o problema no CI); o `.dockerignore` tinha `**/out`, que escondia `port/out` e `adapter/out` do contexto de build, a mesma armadilha que o `.gitignore` já tinha mostrado; e dois builds paralelos disputavam o cache do Gradle, resolvido com `sharing=locked`.

**7. Por que o `notifications` é em camadas e não Hexagonal?**
Ele recebe um evento, monta um texto e o envia, sem regra de negócio que peça isolamento. Um listener chama um serviço, que chama um repositório. Escolher o desenho mais simples quando basta também é uma decisão de arquitetura. O ArchUnit impõe a dependência só para baixo e a ausência de ciclos.

**8. Como o `notifications` evita avisar duas vezes?**
O `eventId` é a chave primária da tabela de notificações, e a gravação usa `INSERT ... ON CONFLICT DO NOTHING`. Uma mensagem repetida devolve zero linhas, e o serviço não envia. Não há uma tabela separada de eventos processados.

**9. Gravar e enviar na mesma transação: qual é o limite?**
Se o envio falhar, a transação desfaz a gravação, e a retentativa recomeça do zero, sem notificação perdida. Mas se o envio der certo e o `commit` falhar, a retentativa avisa de novo. Um remetente real teria de aceitar uma chave de idempotência própria.

**10. O que é um grupo de consumidores, e por que o `notifications` recebeu mensagens antigas?**
Cada grupo mantém os seus próprios offsets, então dois serviços no mesmo tópico recebem cópias independentes. O `notifications` entrou com um grupo novo e `auto-offset-reset: earliest`, então leu o que já estava no tópico desde o início.

**11. Por que a fila de mensagens mortas é por consumidor?**
Dois serviços leem `ledger.transfer-result.v1`. Se os dois usassem o mesmo tópico de falhas, elas se misturariam e ninguém saberia de quem é cada uma. O nome `<tópico>.<serviço>.dlt` resolve, e vale para os três serviços.

**12. O que você extraiu pela regra dos três, e o que não?**
Extraí a configuração de build (`dbank.java-quality`) e o código de teste que se repetia (containers, coletor da fila de mensagens mortas, regras de arquitetura). Não extraí o modelo de domínio, porque compartilhá-lo acoplaria os deploys, nem a outbox, que existe só em dois serviços.

**13. Como provou a refatoração do build sem rodar os testes?**
Comparei o grafo de tarefas do `check` antes e depois com `--dry-run`: as mesmas 89 tarefas, na mesma ordem. Depois, ao acrescentar o novo módulo e o relatório de cobertura, a diferença foi exatamente essas tarefas novas e nada removido.

**14. Por que a cobertura dos testes de integração é um relatório e não uma barreira?**
O mínimo continua exigindo 80% das linhas e 70% dos branches onde há regra de negócio. A suíte de integração cobre os adapters e a configuração, e o relatório combinado mostra isso. Ainda não há um número que justifique reprovar o build por ele, então primeiro se observa.

## Limites que devo reconhecer

- O workflow do CI nunca rodou de verdade, porque não existe repositório remoto. Foi validado só na sintaxe e em cada comando, localmente. As ações estão fixadas por versão, e não por SHA.
- As imagens não são publicadas em nenhum registro.
- Uma mensagem na fila de mensagens mortas continua sendo uma transferência presa, sem alerta.
- A notificação pode ser duplicada se o envio der certo e o `commit` falhar.
- O ponta a ponta não observa o `notifications`, que não tem API de consulta.
- A outbox continua duplicada nos dois serviços que publicam.

## Palavras que o entrevistador espera ouvir

*pipeline como código*, *quality gate*, *imagem em camadas*, *multi-stage build*, *usuário sem privilégios*, *healthcheck*, *grupo de consumidores*, *idempotência*, *fila de mensagens mortas por consumidor*, *arquitetura em camadas*, *regra dos três*, *relatório de cobertura*.
