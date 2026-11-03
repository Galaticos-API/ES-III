# Agente de triagem de incidentes de segurança

Trabalho de Engenharia de Software III: aplicação Java da categoria Security Agents, com Spring AI e modelo aberto via Ollama. Demonstra State, Command, Strategy, Observer e Composite em uma GUI Swing.

O agente classifica alertas fictícios e propõe ações. Contenção e coleta são simuladas em memória. A decisão humana precede comandos de contenção. Não há firewall/EDR real, banco de dados ou autenticação.

## Requisitos

- JDK 21; o projeto compila com alvo Java 21. A execução validada neste computador usou Microsoft JDK 25.
- Maven Wrapper incluído. Internet na primeira execução para baixar Maven e dependências.
- Para inferência real: Ollama iniciado e modelo qwen2.5:7b baixado.
- Para testes e capturas com analista fixo: Ollama não é necessário.

Spring Boot 3.5.3, Spring AI 1.0.0, FlatLaf 3.6 e fonte Inter 4.1 estão declarados no pom.xml. Qwen 2.5 7B é o modelo padrão escolhido; o código deste projeto tem licença MIT. Dependências e modelo têm licenças próprias.

## Executar no Windows

Abra o PowerShell na pasta que contém pom.xml:

```powershell
cd "C:\Users\moret\Documents\GitHub\ES-III\ES-III"
java -version
```

Se o Java não estiver no PATH, configure somente o terminal atual para seu JDK. Exemplo do computador usado no desenvolvimento:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\ms-25.0.0"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Execute os testes, sem precisar do Ollama:

```powershell
.\mvnw.cmd test
```

Instale/abra o Ollama e baixe o modelo quando for validar a IA:

```powershell
ollama pull qwen2.5:7b
```

Se o serviço não estiver ativo, execute ollama serve em outro terminal. Depois abra a GUI:

```powershell
.\mvnw.cmd spring-boot:run
```

Se o modelo estiver indisponível, o sistema pode pedir revisão humana sem ações. Isso demonstra o tratamento de falha, não uma classificação feita pela IA.

No Linux/macOS, usar ./mvnw nos mesmos objetivos Maven.

## Verificar interface sem Ollama

```powershell
.\verificar-interface.ps1
```

O script configura o JDK, executa testes e gera 12 imagens em target/ui-snapshots com analista fixo: seis estados nos temas claro e escuro. Para outro JDK, use o parâmetro -JdkPath. Se a política do PowerShell bloquear o script, é possível iniciá-lo em um processo separado:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\verificar-interface.ps1
```

Na GUI, selecione uma evidência ou clique em um evento para ler e copiar seu detalhe completo. A troca de tema preserva o incidente e os eventos e fica bloqueada durante a consulta. Com decisão pendente, aprovar ou negar antes de iniciar outra triagem.

## Executar em terminal

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=headless"
```

Por padrão, a demonstração para na decisão humana. Para simular aprovação, acrescente --aprovar no argumento:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=headless --aprovar"
```

## Configuração

Em src/main/resources/application.yml: estratégia, confiança mínima e aprovador padrão. Estratégias: human-in-the-loop, plan-then-execute e react. Variáveis de ambiente OLLAMA_BASE_URL e SOC_AGENT_MODEL alteram endpoint e modelo; o padrão é localhost:11434 e qwen2.5:7b. Usar um endpoint remoto muda a condição de execução exclusivamente local.

## Documentação e entrega

- [Arquitetura, cinco padrões e UML](docs/arquitetura.md).
- [Conferência visual das 12 capturas](docs/conferencia-interface.md).
- [Roteiro e divisão sugerida para dez integrantes](docs/entrega-final.md).
- [Tasks e pendências reais](docs/tasks.md).
- [Engenharia reversa histórica](docs/engenharia-reversa.md).

Para gerar ZIP de fontes e documentação:

```powershell
.\preparar-entrega.ps1
```

O pacote é criado em target/entrega e não contém builds, dependências, modelo, credenciais ou .git. O DOCX antigo de apresentação não integra esse pacote; o roteiro atual é entrega-final.md.

## Validação

A suíte automatizada passou com 34 testes, sem falhas nem erros. A triagem foi validada no Windows com Spring AI e Ollama (`qwen2.5:7b`). Na interface, foram conferidos os fluxos de aprovação, recusa e reversão das ações simuladas.

Para rodar os testes no Windows: `.\mvnw.cmd test`.