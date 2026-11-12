# Agente de triagem de incidentes de segurança

Trabalho de Engenharia de Software III: aplicação Java da categoria Security Agents, com Spring AI e modelo aberto via Ollama. Demonstra os padrões State, Command, Strategy, Observer e Composite em uma interface Swing.

O agente classifica alertas fictícios e propõe ações. A contenção e a coleta são simuladas em memória, e os comandos de contenção só são executados após decisão humana. O projeto não usa firewall ou EDR real, banco de dados ou autenticação.

## Repositórios individuais da equipe

Cada integrante registra, na pasta `engenharia3` do seu repositório individual, suas contribuições no projeto de Design Patterns da disciplina e no API.

| Integrante | Repositório individual |
| --- | --- |
| Daniel Dias Pereira | [DanielDPereira/bertoti](https://github.com/DanielDPereira/bertoti) |
| Gustavo Bueno da Silva | A confirmar |
| Gabriel | [GaelNotFound/bertoti](https://github.com/GaelNotFound/bertoti) |
| Emmanuel Basile Garakis Filho | A confirmar |
| Giovanni Moretto | [Giomoret/Bertoti](https://github.com/Giomoret/Bertoti) |
| Lucas | A confirmar |
| Heitor Silva | [heitors1337/bertoti](https://github.com/heitors1337/bertoti) |
| Cauan Gabriel | [LoadCG/bertoti](https://github.com/LoadCG/bertoti) |
| Rafael Matesco | A confirmar |
| Victor Hugo |  [vitorpdim/pdim](https://github.com/vitorpdim/bertoti) |

## Requisitos

- JDK 21 ou superior. O projeto compila para Java 21 e foi validado com Microsoft JDK 25.
- Maven Wrapper incluído. A primeira execução precisa de internet para baixar o Maven e as dependências.
- Para a triagem com IA: Ollama em execução e modelo `qwen2.5:7b` baixado.
- Os testes automatizados e as capturas com analista fixo não precisam do Ollama.

Spring Boot 3.5.3, Spring AI 1.0.0, FlatLaf 3.6 e a fonte Inter 4.1 estão declarados no `pom.xml`. Qwen 2.5 7B é o modelo padrão. O código do projeto tem licença MIT; as dependências e o modelo têm licenças próprias.

## Executar no Windows

Abra o PowerShell na pasta do projeto, onde está o `pom.xml`, e confira a versão do Java:

```powershell
java -version
```

Se o Java não estiver no `PATH` ou se o Maven Wrapper não encontrar o JDK, configure o terminal atual. Substitua o caminho abaixo pelo da sua instalação:

```powershell
$env:JAVA_HOME = "C:\caminho\para\o\jdk"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Para rodar os testes sem Ollama:

```powershell
.\mvnw.cmd test
```

Para usar a triagem com IA, instale e abra o Ollama e baixe o modelo:

```powershell
ollama pull qwen2.5:7b
```

Se o serviço não estiver ativo, execute `ollama serve` em outro terminal. Depois, abra a interface:

```powershell
.\mvnw.cmd spring-boot:run
```

Se o modelo estiver indisponível, o sistema pode pedir revisão humana sem propor ações. Esse é o tratamento de falha da aplicação.

No Linux e no macOS, use `./mvnw` com os mesmos objetivos Maven.

## Verificar a interface sem Ollama

```powershell
.\verificar-interface.ps1
```

O script configura o JDK, executa os testes e gera 12 imagens em `target/ui-snapshots`: seis estados nos temas claro e escuro, com respostas fixas para a triagem. Para indicar outro JDK, use o parâmetro `-JdkPath`. Se a política do PowerShell bloquear o script, é possível executá-lo em um processo separado:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\verificar-interface.ps1
```

Na interface, selecione uma evidência ou clique em um evento para ler e copiar o texto completo. A troca de tema preserva o incidente e os eventos, mas fica indisponível durante a consulta. Se houver uma decisão pendente, aprove ou negue a contenção antes de iniciar outra triagem.

## Executar em terminal

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=headless"
```

Por padrão, a demonstração para na decisão humana. Para simular a aprovação, acrescente `--aprovar`:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=headless --aprovar"
```

## Configuração

Em `src/main/resources/application.yml`, é possível configurar a estratégia, a confiança mínima e o aprovador padrão. As estratégias disponíveis são `human-in-the-loop`, `plan-then-execute` e `react`.

As variáveis de ambiente `OLLAMA_BASE_URL` e `SOC_AGENT_MODEL` permitem mudar o endereço e o modelo. Os valores padrão são `http://localhost:11434` e `qwen2.5:7b`. Se o endereço apontar para um serviço remoto, a inferência deixará de ser exclusivamente local.

## Documentação e entrega

- [Arquitetura, cinco padrões e UML](docs/arquitetura.md)
- [Conferência visual das 12 capturas](docs/conferencia-interface.md)
- [Roteiro e divisão sugerida para dez integrantes](docs/entrega-final.md)
- [Tarefas e pendências](docs/tasks.md)
- [Engenharia reversa](docs/engenharia-reversa.md)

Para gerar um ZIP com os fontes e a documentação:

```powershell
.\preparar-entrega.ps1
```

O pacote é criado em `target/entrega` e não inclui arquivos de build, dependências, o modelo, credenciais ou a pasta `.git`. O roteiro atual está em [`docs/entrega-final.md`](docs/entrega-final.md).

## Validação

A suíte automatizada passou com 34 testes, sem falhas nem erros. A triagem foi validada no Windows com Spring AI e Ollama (`qwen2.5:7b`). Na interface, foram conferidos os fluxos de aprovação, recusa e reversão das ações simuladas.

Para rodar os testes no Windows: `.\mvnw.cmd test`.
