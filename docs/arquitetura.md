# Arquitetura e Padrões de Projeto

Agente de triagem de incidentes de segurança construído com Spring AI e modelos
abertos executados localmente via Ollama.

Categoria de caso de uso: **Security Agents** — investigar alertas, correlacionar
eventos e acionar contenção, com governança estrita e aprovação humana
obrigatória para ações críticas.

---

## 1. Visão geral em camadas

```mermaid
flowchart TB
    subgraph GUI["Camada de apresentação"]
        Frame["SocDashboardFrame<br/><i>Swing</i>"]
        Tree["EvidenceNode<br/><b>COMPOSITE</b>"]
    end

    subgraph APP["Camada de orquestração"]
        Service["IncidentTriageService"]
        Registry["PlannerRegistry"]
    end

    subgraph PATTERNS["Núcleo de domínio"]
        State["Incident + IncidentState<br/><b>STATE</b>"]
        Cmd["AgentCommand + CommandInvoker<br/><b>COMMAND</b>"]
        Strat["TriagePlanner<br/><b>STRATEGY</b>"]
        Bus["AgentEventBus<br/><b>OBSERVER</b>"]
    end

    subgraph INFRA["Infraestrutura"]
        Llm["LlmThreatAnalyst<br/><i>Spring AI</i>"]
        Ollama["Ollama<br/>qwen2.5:7b"]
        Gateway["ContainmentGateway<br/><i>EDR / firewall / NAC</i>"]
        Mitre["MitreRepository<br/><i>ATT&amp;CK offline</i>"]
    end

    Frame --> Service
    Frame --> Tree
    Frame -. observa .-> Bus
    Service --> Registry
    Registry --> Strat
    Service --> State
    Service --> Cmd
    State -. publica .-> Bus
    Cmd -. publica .-> Bus
    Strat --> Llm
    Llm --> Ollama
    Cmd --> Gateway
    Cmd --> Mitre
    Tree --> State
```

**Regra de dependência:** o núcleo de domínio não conhece Spring AI, Swing nem
Ollama. As setas para a infraestrutura atravessam interfaces (`ThreatAnalyst`,
`ContainmentGateway`), o que permite testar todos os padrões sem modelo e sem
rede — os 24 testes automatizados rodam offline.

---

## 2. Onde cada padrão está

| Padrão | Pacote | Papel na arquitetura |
|---|---|---|
| **State** | `state` | Ciclo de vida do incidente; barra ação destrutiva antes da aprovação |
| **Command** | `command` | Cada ação de resposta como objeto: enfileirável, auditável, reversível |
| **Strategy** | `strategy` | Três políticas de planejamento intercambiáveis em tempo de execução |
| **Observer** | `observer` | Difusão de eventos para auditoria, console e painel |
| **Composite** | `gui.tree` | Árvore de evidências exibida na GUI |

---

## 3. STATE — ciclo de vida do incidente

### 3.1 Diagrama de classes

```mermaid
classDiagram
    class Incident {
        -IncidentState state
        -List~Alert~ alerts
        -AgentEventBus eventBus
        +triage(TriageVerdict)
        +correlate()
        +requestApproval()
        +approve(String)
        +deny(String, String)
        +close(String)
        +phase() IncidentPhase
        +allowsContainmentCommands() boolean
        -transitionTo(IncidentState, String)
    }

    class IncidentState {
        <<interface>>
        +phase() IncidentPhase
        +triage(Incident, TriageVerdict) IncidentState
        +correlate(Incident) IncidentState
        +requestApproval(Incident) IncidentState
        +approve(Incident, String) IncidentState
        +deny(Incident, String, String) IncidentState
        +close(Incident, String) IncidentState
        +allowsContainmentCommands() boolean
    }

    class ReceivedState
    class TriagingState
    class CorrelatingState
    class AwaitingApprovalState
    class ContainingState
    class ClosedState

    Incident o--> IncidentState : delega comportamento
    IncidentState <|.. ReceivedState
    IncidentState <|.. TriagingState
    IncidentState <|.. CorrelatingState
    IncidentState <|.. AwaitingApprovalState
    IncidentState <|.. ContainingState
    IncidentState <|.. ClosedState
```

**Decisão de projeto.** Todas as operações são declaradas na interface com
implementação padrão que lança `IllegalTransitionException`. Cada estado
concreto sobrescreve **apenas** o que permite. Consequência: transição ilegal
deixa de ser um `if` espalhado pelo serviço e passa a ser impossível por
construção. Não existe nenhum `switch` sobre a fase no código — acrescentar
uma fase significa criar uma classe, sem tocar no `Incident`.

### 3.2 Diagrama de máquina de estados

```mermaid
stateDiagram-v2
    [*] --> RECEIVED
    RECEIVED --> TRIAGING : triage(verdict)

    TRIAGING --> CORRELATING : correlate()
    TRIAGING --> CLOSED : close() <br/><i>falso positivo</i>

    CORRELATING --> AWAITING_APPROVAL : requestApproval()
    CORRELATING --> CLOSED : close() <br/><i>sem ação aplicável</i>

    AWAITING_APPROVAL --> CONTAINING : approve(analista)
    AWAITING_APPROVAL --> CLOSED : deny(analista, motivo)

    CONTAINING --> CLOSED : close(resumo)
    CLOSED --> [*]

    note right of AWAITING_APPROVAL
        Barreira de governança.
        Única transição para CONTAINING
        exige decisão humana explícita.
    end note

    note right of CONTAINING
        Único estado em que
        allowsContainmentCommands()
        devolve true.
    end note
```

---

## 4. COMMAND — ações de resposta

```mermaid
classDiagram
    class AgentCommand {
        <<interface>>
        +name() String
        +description() String
        +execute() CommandResult
        +requiresApproval() boolean
        +undoable() boolean
        +undo() CommandResult
    }

    class CommandInvoker {
        -Deque~AgentCommand~ queue
        -Deque~AgentCommand~ undoStack
        -List~ExecutionRecord~ history
        +enqueue(Incident, AgentCommand)
        +execute(Incident, AgentCommand) ExecutionRecord
        +executeQueue(Incident) List~ExecutionRecord~
        +undoLast(Incident) ExecutionRecord
    }

    class IsolateHostCommand {
        +requiresApproval() boolean
        +undoable() boolean
        +undo() CommandResult
    }
    class BlockIpCommand {
        +requiresApproval() boolean
        +undoable() boolean
        +undo() CommandResult
    }
    class CollectForensicsCommand {
        +execute() CommandResult
    }
    class LookupMitreCommand {
        +execute() CommandResult
    }

    class ContainmentGateway {
        <<interface>>
        +isolateHost(String, String) String
        +restoreHost(String) String
        +blockIp(String, String) String
        +unblockIp(String) String
        +collectForensics(String) String
    }

    AgentCommand <|.. IsolateHostCommand
    AgentCommand <|.. BlockIpCommand
    AgentCommand <|.. CollectForensicsCommand
    AgentCommand <|.. LookupMitreCommand

    CommandInvoker o--> AgentCommand : enfileira e executa
    CommandInvoker ..> Incident : consulta o State
    CommandInvoker ..> AgentEventBus : publica

    IsolateHostCommand --> ContainmentGateway
    BlockIpCommand --> ContainmentGateway
    CollectForensicsCommand --> ContainmentGateway
```

**Três decisões que valem a defesa oral:**

1. **Comandos são código Java, nunca texto gerado pelo modelo.** O modelo
   escolhe *qual* comando invocar e com *quais parâmetros*; o que cada comando
   faz é fixado em tempo de compilação.
2. **A barreira de aprovação vive no `CommandInvoker`, não em cada comando.**
   Um comando novo já nasce protegido — não há como esquecer de checar.
3. **`undo()` é ação compensatória real.** Reverter uma contenção equivocada
   rápido vale tanto quanto aplicá-la; `IsolateHostCommand.undo()` reconecta o
   host de fato.

---

## 5. STRATEGY — políticas de planejamento

```mermaid
classDiagram
    class TriagePlanner {
        <<interface>>
        +id() String
        +displayName() String
        +plan(Incident) Plan
    }

    class Plan {
        <<record>>
        +TriageVerdict verdict
        +List~AgentCommand~ commands
        +boolean requiresApproval
    }

    class PlanThenExecutePlanner {
        +plan(Incident) Plan
    }
    class ReActPlanner {
        -int MAX_STEPS
        +plan(Incident) Plan
    }
    class HumanInTheLoopPlanner {
        -TriagePlanner delegate
        +plan(Incident) Plan
    }

    class ThreatAnalyst {
        <<interface>>
        +classify(Incident) TriageVerdict
        +proposePlan(Incident) List~ProposedAction~
        +proposeNext(Incident, List~String~) Optional~ProposedAction~
    }

    class CommandFactory {
        -ALLOWED_TOOLS Set~String~
        +create(ProposedAction, String) Optional~AgentCommand~
    }

    class LlmThreatAnalyst {
        -ChatClient chatClient
    }

    class PlannerRegistry {
        -TriagePlanner active
        +activate(String)
        +available() List~TriagePlanner~
    }

    TriagePlanner <|.. PlanThenExecutePlanner
    TriagePlanner <|.. ReActPlanner
    TriagePlanner <|.. HumanInTheLoopPlanner
    HumanInTheLoopPlanner o--> PlanThenExecutePlanner : decora
    TriagePlanner ..> Plan : produz

    PlanThenExecutePlanner --> ThreatAnalyst
    ReActPlanner --> ThreatAnalyst
    ThreatAnalyst <|.. LlmThreatAnalyst

    PlanThenExecutePlanner --> CommandFactory
    ReActPlanner --> CommandFactory
    CommandFactory ..> AgentCommand : traduz

    PlannerRegistry o--> TriagePlanner : seleciona em runtime
```

**`HumanInTheLoopPlanner` é Strategy que também decora** — envolve outro
planejador e força aprovação sem duplicar a lógica de planejamento.

**`CommandFactory` é o único ponto onde saída de modelo vira ação**, e opera por
lista de permissão. Um modelo que alucine `delete_all_logs` não encontra
tradutor: a ação é descartada com registro em log. Existe teste automatizado
para esse caso.

**`ReActPlanner` executa apenas comandos de leitura durante o ciclo** e acumula
os destrutivos para depois da aprovação. Sem essa separação, o ciclo adaptativo
contornaria a barreira de governança.

---

## 6. OBSERVER — difusão de eventos

```mermaid
classDiagram
    class AgentEventBus {
        -List~AgentEventListener~ listeners
        +subscribe(AgentEventListener)
        +unsubscribe(AgentEventListener)
        +publish(AgentEvent)
    }

    class AgentEventListener {
        <<interface>>
        +onEvent(AgentEvent)
        +supports(AgentEvent) boolean
        +listenerName() String
    }

    class AgentEvent {
        <<record>>
        +EventType type
        +String incidentId
        +String title
        +Instant occurredAt
    }

    class AuditTrailListener {
        +entries() List~String~
    }
    class ConsoleListener {
        +onEvent(AgentEvent)
    }
    class SocDashboardFrame {
        +onEvent(AgentEvent)
    }
    class EventListenerRegistrar {
        +EventListenerRegistrar(AgentEventBus, List)
    }

    AgentEventBus o--> AgentEventListener
    AgentEventBus ..> AgentEvent : difunde
    AgentEventListener <|.. AuditTrailListener
    AgentEventListener <|.. ConsoleListener
    AgentEventListener <|.. SocDashboardFrame
    EventListenerRegistrar ..> AgentEventBus : registra

    Incident ..> AgentEventBus : transições
    CommandInvoker ..> AgentEventBus : execuções
```

**Falha de um observador nunca interrompe a difusão para os demais.** Em
segurança, perder a trilha de auditoria por causa de um erro de renderização na
GUI seria inaceitável — o barramento captura a exceção, registra e continua.

A estrutura usa `CopyOnWriteArrayList` porque a GUI se inscreve pela *Event
Dispatch Thread* do Swing enquanto o agente publica de uma thread de trabalho.

---

## 7. COMPOSITE — árvore de evidências na GUI

```mermaid
classDiagram
    class EvidenceNode {
        <<interface>>
        +label() String
        +severity() Severity
        +children() List~EvidenceNode~
        +isLeaf() boolean
        +add(EvidenceNode)
        +leafCount() int
        +highestSeverity() Severity
        +depth() int
        +render(int) String
    }

    class EvidenceLeaf {
        <<record>>
        +String label
        +Severity severity
        +children() List~EvidenceNode~
    }

    class EvidenceGroup {
        -List~EvidenceNode~ children
        +add(EvidenceNode)
        +with(EvidenceNode) EvidenceGroup
    }

    class EvidenceTreeBuilder {
        +build(Incident) EvidenceNode
    }

    class SocDashboardFrame {
        -toSwingNode(EvidenceNode) TreeNode
    }

    EvidenceNode <|.. EvidenceLeaf
    EvidenceNode <|.. EvidenceGroup
    EvidenceGroup o--> EvidenceNode : filhos
    EvidenceTreeBuilder ..> EvidenceGroup : monta
    SocDashboardFrame ..> EvidenceNode : renderiza no JTree
```

Estrutura produzida em tempo de execução:

```
+ INC-0001 | Encerrado | host WKS-4471 [CRITICAL]
  + [ALR-1001] Volume de saída 480 MB para destino externo (suricata) [CRITICAL]
    - IP: 185.220.101.7 [CRITICAL]
  + [ALR-1002] Conexão TLS com certificado autoassinado (zeek) [HIGH]
    - IP: 185.220.101.7 [HIGH]
    - PROCESS: rundll32.exe [HIGH]
  + Técnicas MITRE ATT&CK [MEDIUM]
    - T1041 - Exfiltration Over C2 Channel (Exfiltration) [MEDIUM]
  + Veredito: TRUE_POSITIVE (confiança 0,90) [INFO]
    - <justificativa produzida pelo modelo> [INFO]
  + Decisão humana [INFO]
    - Aprovado por demo.operador [INFO]
```

**As operações recursivas são métodos padrão da interface**, então folha e
composto respondem identicamente a `leafCount()`, `highestSeverity()`,
`depth()` e `render()`. A severidade agrega de baixo para cima: um grupo
marcado como `LOW` contendo um IOC `CRITICAL` aparece como `CRITICAL` no
painel, sem que ninguém recalcule isso manualmente.

**A folha recusa filhos** lançando `UnsupportedOperationException`. É a variante
*transparente* do padrão descrita no GoF: interface uniforme, com a segurança
verificada em tempo de execução.

---

## 8. Fluxo completo — diagrama de sequência

```mermaid
sequenceDiagram
    actor Analista
    participant GUI as SocDashboardFrame
    participant Svc as IncidentTriageService
    participant Plan as TriagePlanner<br/>(STRATEGY)
    participant Llm as LlmThreatAnalyst
    participant Inc as Incident<br/>(STATE)
    participant Inv as CommandInvoker<br/>(COMMAND)
    participant Bus as AgentEventBus<br/>(OBSERVER)

    Analista->>GUI: seleciona cenário e estratégia
    GUI->>Svc: triage(incident)
    Svc->>Inc: open()
    Inc-->>Bus: INCIDENT_CREATED

    Svc->>Plan: plan(incident)
    Plan->>Llm: classify(incident)
    Llm-->>Plan: TriageVerdict
    Plan->>Llm: proposePlan(incident)
    Llm-->>Plan: lista de ações propostas
    Note over Plan: CommandFactory traduz<br/>por lista de permissão
    Plan-->>Svc: Plan(verdict, commands, requiresApproval)
    Svc-->>Bus: PLAN_CREATED

    Svc->>Inc: triage(verdict)
    Inc-->>Bus: STATE_CHANGED (Recebido → Em triagem)
    Svc->>Inc: correlate()
    Inc-->>Bus: STATE_CHANGED (→ Correlacionando)

    Svc->>Inv: enqueue(comandos)
    Inv-->>Bus: COMMAND_QUEUED (×N)

    Svc->>Inc: requestApproval()
    Inc-->>Bus: APPROVAL_REQUESTED
    Bus-->>GUI: habilita botão "Aprovar"

    Note over Analista,Inv: O agente para aqui.<br/>Nenhuma ação destrutiva ocorreu.

    Analista->>GUI: Aprovar contenção
    GUI->>Svc: approve(incident, analista)
    Svc->>Inc: approve(analista)
    Inc-->>Bus: STATE_CHANGED (→ Em contenção)

    Svc->>Inv: executeQueue(incident)
    Inv->>Inc: allowsContainmentCommands()?
    Inc-->>Inv: true
    Inv-->>Bus: COMMAND_EXECUTED (×N)

    Svc->>Inc: close(resumo)
    Inc-->>Bus: INCIDENT_CLOSED
    Bus-->>GUI: atualiza árvore e log
```

O ponto central do diagrama é a pausa antes da aprovação. Se o analista negar,
`deny()` leva direto a `CLOSED` e a fila **nunca** é executada.

---

## 9. Verificação da barreira de governança

Dois mecanismos independentes protegem a mesma invariante — nenhuma ação
destrutiva sem decisão humana:

| Camada | Mecanismo | Teste |
|---|---|---|
| State | `AwaitingApprovalState` é a única porta para `CONTAINING` | `IncidentStateTest.contencaoExigeAprovacao` |
| Command | `CommandInvoker` recusa comando aprovável fora da fase de contenção | `CommandInvokerTest.recusaSemAprovacao` |

A redundância é intencional: se um planejador futuro esquecer de marcar
`requiresApproval`, o State ainda barra; se um estado novo permitir a transição
por engano, o Invoker ainda recusa.

---

## 10. Padrões adicionais presentes

Além dos cinco exigidos, a arquitetura usa:

- **Decorator** — `HumanInTheLoopPlanner` envolve outro planejador
- **Factory** — `CommandFactory` traduz sugestão em comando por lista de permissão
- **Repository** — `MitreRepository` encapsula a base ATT&CK
- **Ports and Adapters** — `ThreatAnalyst` e `ContainmentGateway` isolam modelo e infraestrutura
- **Injeção de dependência** — todos os componentes são beans Spring
