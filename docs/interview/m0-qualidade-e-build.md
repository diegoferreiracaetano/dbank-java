# Roteiro de consulta: M0 e M2.1, qualidade e build

Respostas de referência, para consulta. Perguntas sobre como o projeto garante qualidade e como o build está organizado.

## Em 30 segundos

Um único comando, `./gradlew check`, roda formatação, Checkstyle, SpotBugs, testes, cobertura mínima e testes de integração. A configuração vive em um plugin de convenção (`dbank.spring-service`, no `build-logic`) que cada serviço aplica com uma linha, e as versões ficam em um catálogo (`libs.versions.toml`). O CI só precisa chamar o `check`.

## Perguntas e respostas

**1. Por que um compilador estrito (`-Xlint:all -Werror`)?**
Porque aviso ignorado vira bug. Logo na primeira rodada ele achou um problema real: toda exceção em Java é `Serializable`, e sem `serialVersionUID` a serialização pode quebrar entre versões. O Kotlin não dá esse aviso.

**2. O que cada ferramenta de qualidade faz?**
O Spotless formata, e `spotlessApply` corrige sozinho. O Checkstyle impõe convenções de estilo. O SpotBugs analisa o bytecode atrás de bugs reais, como acesso a `null`, o que importa porque o Java não tem null-safety. O JaCoCo mede a cobertura. São camadas diferentes, e nenhuma substitui a outra.

**3. Por que Palantir Java Format e não o google-java-format?**
O do Google força 2 espaços de indentação, e o código já usava 4. O Palantir mantém 4 espaços e linhas de até 120 colunas, então quase nada do código existente mudou.

**4. Por que a cobertura mínima mede só `domain` e `application`?**
É onde mora a regra de negócio, e os testes unitários a cobrem sem Docker. Os adapters são cobertos pelos testes de integração, que têm outro propósito. Medir tudo junto misturaria os dois e esconderia lacunas.

**5. Por que o `integrationTest` é separado do `test`?**
Rapidez e custo: o `test` roda em segundos e sem Docker, e o `integrationTest` sobe um Postgres e um Redis reais. É o `test` contra o `androidTest` do Android, com containers no lugar do emulador.

**6. Por que um catálogo de versões?**
Uma fonte única de versões e plugins, compartilhada entre os módulos e o `build-logic`. Sem ele, cada módulo teria a sua cópia e as versões divergiriam.

**7. Por que um plugin de convenção no `build-logic`?**
Para a configuração de build não se repetir entre os serviços. O `ledger` caiu de 160 para 17 linhas de build, e o `payments` nasceu com todo o pipeline de qualidade só por aplicar o plugin. Foi feito antes do segundo serviço para ele não nascer duplicado.

**8. Por que um `build-logic` (included build) e não o `buildSrc`?**
Uma mudança no `buildSrc` invalida o cache de todas as tarefas de todos os módulos, e o `build-logic` invalida só o que depende dele. Com a configuração destinada a crescer, o `build-logic` escala melhor.

**9. Como provar que uma refatoração de build não quebrou nada, sem rodar os testes?**
Comparando o grafo de tarefas do `check` antes e depois, com `--dry-run`. Se as listas forem idênticas (as mesmas tarefas, na mesma ordem), a fiação é a mesma. Foi o que fizemos no M2.1: 24 tarefas, idênticas.

**10. O que a falha enganosa do SpotBugs ensinou?**
Que o código de saída "4" parecia "achou bugs", mas ele estava quebrando por dentro: o plugin de gestão de dependências do Spring travava o `commons-lang3` numa versão antiga só na configuração do SpotBugs. A lição é não confiar no resumo, e sim ler o erro real, e depois provar que a ferramenta funciona plantando um bug de propósito.

**11. Por que o clone limpo é a prova final?**
Porque o `check` na sua pasta pode passar por causa de arquivos que nunca foram para o Git. Já aconteceu: o `.gitignore` escondia pacotes chamados `out`, e três interfaces nunca foram commitadas. Um clone novo com o `check` completo pega isso.

## Palavras que o entrevistador espera ouvir

*quality gate*, *análise estática*, *convention plugin*, *included build*, *catálogo de versões*, *cobertura sobre regra de negócio*, *piramide de testes*, *reprodutibilidade do build*.
