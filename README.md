# Agente de Triagem de Incidentes de Segurança

Agente autônomo que classifica alertas de segurança, correlaciona evidências e
propõe ações de contenção — com **aprovação humana obrigatória** antes de
qualquer ação destrutiva.

Construído com Spring AI e modelos de linguagem abertos executados localmente.
Nenhuma chamada a serviço externo, nenhuma chave de API.

> Trabalho da disciplina de Engenharia de Software III — FATEC

---

## O que este projeto demonstra

Cinco padrões de projeto do GoF aplicados onde o domínio realmente os exige:

| Padrão | Onde vive | O que resolve aqui |
|---|---|---|
| **State** | `state/` | Ciclo de vida do incidente; torna impossível conter um host antes da aprovação |
| **Command** | `command/` | Ações de resposta enfileiráveis, auditáveis e reversíveis |
| **Strategy** | `strategy/` | Três políticas de planejamento trocáveis em tempo de execução |
| **Observer** | `observer/` | Trilha de auditoria, log de console e painel alimentados pelos mesmos eventos |
| **Composite** | `gui/tree/` | Árvore de evidências na interface gráfica |

A justificativa detalhada de cada escolha, com os diagramas UML, está em
**[docs/arquitetura.md](docs/arquitetura.md)**.

---

## Stack — tudo software livre

| Camada | Componente | Licença |
|---|---|---|
| Framework | Spring Boot 3.5.3 + Spring AI 1.0.0 | Apache 2.0 |
| Runtime do modelo | Ollama | MIT |
| Modelo de linguagem | Qwen 2.5 7B | Apache 2.0 |
| Interface gráfica | Swing (OpenJDK) | GPLv2 + Classpath Exception |
| Build | Maven | Apache 2.0 |
| Testes | JUnit 5 + AssertJ | EPL 2.0 / Apache 2.0 |
| Diagramas | Mermaid | MIT |

O modelo padrão é o **Qwen 2.5 7B**, sob Apache 2.0 — licença aprovada pela OSI.
O Llama 3.1 funciona igualmente bem e pode ser configurado, mas sua licença é
*source-available*, não open source no sentido estrito: traz restrição de uso
acima de 700 milhões de usuários mensais e obrigação de atribuição.

---

## Pré-requisitos

- JDK 21
- Ollama
- ~5 GB de disco para o modelo

```bash
brew install openjdk@21 ollama
```

---

## Como executar

**1. Suba o Ollama e baixe o modelo**

```bash
ollama serve
```

```bash
ollama pull qwen2.5:7b
```

**2. Rode a interface gráfica**

```bash
./mvnw spring-boot:run
```

**3. Ou rode em terminal, sem interface**

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=headless"
```

O agente para na aprovação, como pararia em produção. Para simular a decisão
humana e ver a contenção executar, acrescente `--aprovar`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=headless --aprovar"
```

Para escolher o cenário, basta um trecho do nome — a linha de comando quebra o
argumento nos espaços:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=headless --cenario=Cifragem"
```

**4. Testes** — rodam offline, sem Ollama:

```bash
./mvnw test
```

---

## Usando a interface

1. Escolha um dos quatro cenários no seletor superior
2. Escolha a estratégia de planejamento — o efeito é visível no resultado
3. Clique em **Triar incidente**
4. O agente classifica, correlaciona e **para**, aguardando decisão
5. **Aprovar** executa a contenção; **Negar** encerra sem qualquer ação
6. **Desfazer** reverte a última contenção aplicada

O painel esquerdo mostra a árvore de evidências (Composite) e o direito a
sequência de eventos (Observer). Os botões habilitam e desabilitam conforme a
fase do incidente (State).

---

## Cenários incluídos

| Cenário | Técnica ATT&CK | Observado com Qwen 2.5 7B |
|---|---|---|
| Exfiltração via canal C2 | T1041 | Verdadeiro positivo, confiança 0,90 — contenção proposta |
| Força bruta em VPN | T1110 | — |
| Cifragem em massa | T1486 | — |
| Backup noturno | — | **Verdadeiro positivo, confiança 0,80 — classificação incorreta** |

Hosts, contas e endereços são fictícios. As linhas sem observação ainda não
foram medidas; a saída do modelo varia entre execuções.

### O falso positivo que o modelo não reconhece

O cenário "Backup noturno" descreve uma transferência legítima de 12 GB em
janela de manutenção. Um analista humano encerraria em segundos. O Qwen 2.5 7B
classifica como verdadeiro positivo e propõe **isolar o servidor de backup**.

Isso não é um defeito a esconder — é a demonstração mais direta do porquê da
arquitetura. O modelo errou; a barreira de aprovação impediu o estrago. Se o
agente tivesse autonomia para executar, teria derrubado a infraestrutura de
backup da organização durante a janela de manutenção.

A causa raiz é falta de contexto: o agente recebe o alerta, mas não sabe que
`BKP-SRV-01` é um servidor de backup nem que existe janela de manutenção
aprovada. Um SOC real resolve isso com enriquecimento — inventário de ativos e
calendário de mudanças alimentando o prompt. Fica registrado como evolução
natural do projeto.

---

## Configuração

Em `src/main/resources/application.yml`:

```yaml
soc-agent:
  planner: human-in-the-loop   # ou plan-then-execute, react
  minimum-confidence: 0.6
  default-approver: analista.local

spring:
  ai:
    ollama:
      chat:
        options:
          model: qwen2.5:7b
```

Também por variável de ambiente: `SOC_AGENT_MODEL` e `OLLAMA_BASE_URL`.

---

## Decisões de projeto que valem destaque

**O modelo não executa nada.** Ele classifica, justifica e sugere *nomes* de
ferramenta. A `CommandFactory` é o único ponto onde uma sugestão vira ação, e
opera por lista de permissão — uma ferramenta alucinada pelo modelo é
descartada com registro em log, nunca interpretada. Há teste automatizado para
esse caso.

**A barreira de aprovação é redundante de propósito.** O `State` só libera a
transição para contenção após decisão humana, e o `CommandInvoker` recusa
comandos aprováveis fora dessa fase. Se um planejador futuro esquecer de marcar
uma ação como destrutiva, a outra camada ainda barra.

**Falha degrada para revisão humana.** Se o modelo não responde, responde fora
do formato ou devolve classificação desconhecida, o veredito vira
`NEEDS_HUMAN_REVIEW`. Um agente de segurança que não consegue concluir precisa
escalar, nunca adivinhar.

**Nenhum observador derruba o agente.** O barramento isola exceções de
observadores individualmente: um erro de renderização na interface não pode
custar a trilha de auditoria.

---

## Estrutura

```
src/main/java/br/fatec/esiii/socagent/
├── domain/      Alert, Ioc, Severity, MitreTechnique, TriageVerdict
├── state/       Incident (Context) + IncidentState e estados concretos
├── command/     AgentCommand, CommandInvoker, ContainmentGateway
├── strategy/    TriagePlanner, três planejadores, CommandFactory
├── observer/    AgentEventBus, listeners de auditoria e console
├── ai/          LlmThreatAnalyst — integração Spring AI
├── mitre/       MitreRepository — base ATT&CK offline
├── gui/         SocDashboardFrame (Swing)
│   └── tree/    EvidenceNode, EvidenceLeaf, EvidenceGroup
└── service/     IncidentTriageService, PlannerRegistry, cenários
```

---

## Licença

MIT — veja [LICENSE](LICENSE).
