> Registro da análise de 21/10/2026. Os achados abaixo descrevem a versão anterior às correções. Consulte entrega-final.md para a revisão de 26/10/2026.

# Engenharia reversa — SOC Agent

Análise em 21/10/2026. Referência: commit `7714156`. Método: inspeção do código, configuração, documentação e testes existentes. Este documento descreve a implementação encontrada; não representa uma nova especificação aprovada.

## Objetivo e limites

Aplicação desktop acadêmica para analisar alertas de segurança, produzir um veredito e propor ações, com aprovação humana antes da contenção. Existem execução gráfica e demonstração em terminal. A contenção e a coleta forense são simuladas em memória: não há integração efetiva com firewall ou EDR. Não foram encontrados API HTTP, banco de dados, autenticação ou ingestão de alertas externos.

## Arquitetura recuperada

| Módulo | Responsabilidade | Principais referências em `src/main/java/br/fatec/esiii/socagent/` |
|---|---|---|
| Inicialização | Inicializar Spring e selecionar apresentação | `SocAgentApplication`, `gui/GuiLauncher`, `service/HeadlessDemoRunner` |
| Domínio | Alertas, indicadores, severidade e veredito | `domain/Alert`, `Ioc`, `Severity`, `TriageVerdict`, `MitreTechnique` |
| Ciclo de vida | Transições e decisão humana | `state/Incident`, `IncidentState`, `IncidentStates` |
| Orquestração | Abrir, triar, aprovar, negar e desfazer | `service/IncidentTriageService` |
| Planejamento | Selecionar e construir plano | `strategy/TriagePlanner`, três planejadores e `service/PlannerRegistry` |
| IA | Consultar modelo e converter respostas | `ai/LlmThreatAnalyst`, `AnalystResponses`, `strategy/ThreatAnalyst` |
| Execução | Traduzir sugestões, enfileirar e executar | `strategy/CommandFactory`, `command/CommandInvoker` e comandos concretos |
| Integrações locais | Simular contenção e consultar técnicas | `command/SimulatedContainmentGateway`, `mitre/MitreRepository` |
| Eventos | Publicar mudanças e manter auditoria | `observer/AgentEventBus`, `AuditTrailListener`, `ConsoleListener` |
| Apresentação | Cenários, controles, eventos e evidências | `gui/SocDashboardFrame`, `component/`, `theme/`, `gui/tree/` |

Stack declarada no projeto: Java 21, Maven Wrapper, Spring Boot 3.5.3, Spring AI 1.0.0, Swing e FlatLaf. Modelo padrão configurado: `qwen2.5:7b`, via Ollama em `http://localhost:11434`. Endereço e modelo podem ser alterados por variáveis de ambiente; execução exclusivamente local depende dessa configuração.

## Fluxo funcional

1. O operador escolhe um dos quatro cenários fictícios e uma estratégia.
2. O serviço abre um incidente com um ou mais alertas e identificador sequencial em memória.
3. O planejador consulta o analista para classificar e sugerir ferramentas.
4. `CommandFactory` aceita apenas `isolate_host`, `block_ip`, `collect_forensics` e `lookup_mitre`.
5. O serviço registra o veredito; falso positivo encerra o incidente sem executar o plano.
6. Outros resultados passam à correlação. Um plano vazio encerra o incidente, inclusive quando o veredito pede revisão humana: limitação atual.
7. Planos não vazios que exigem aprovação ou têm veredito não acionável aguardam decisão. Planos de leitura acionáveis podem executar diretamente.
8. Aprovar libera a fila e encerra; negar encerra o incidente. A implementação de negação não limpa a fila.
9. A reversão consulta a pilha de comandos reversíveis. A pilha atual é global ao invocador.

Na estratégia ReAct, ferramentas sem aprovação são executadas durante o planejamento, diretamente por `candidate.execute()`. Essas execuções não percorrem o histórico e os eventos de execução do invocador. O ciclo tem limite de cinco passos.

## Estados e regras

| Estado | Operações aceitas |
|---|---|
| RECEIVED | Triar → TRIAGING |
| TRIAGING | Correlacionar → CORRELATING; encerrar → CLOSED |
| CORRELATING | Solicitar aprovação → AWAITING_APPROVAL; encerrar → CLOSED |
| AWAITING_APPROVAL | Aprovar → CONTAINING; negar → CLOSED |
| CONTAINING | Executar contenção; encerrar → CLOSED |
| CLOSED | Estado terminal para transições do incidente |

O invocador consulta o estado antes de executar comandos que exigem aprovação. A reversão é uma operação separada, permitida pelo invocador mesmo após encerramento. O limiar padrão de confiança é 0,6. O aprovador padrão é `analista.local`, sem autenticação.

## Padrões de projeto

| Padrão | Aplicação observada |
|---|---|
| State | `Incident` delega operações a estados concretos e rejeita transições ilegais. |
| Command | Ações encapsuladas, fila, resultados, histórico e compensação. |
| Strategy | Planejar e executar; ReAct; humano no circuito. |
| Observer | Eventos distribuídos a console, auditoria e interface; falhas de ouvintes isoladas. |
| Composite | Grupos e folhas na árvore de evidências, com agregação de severidade. |

## Dados e integrações

`Alert` contém ID, origem, hostname, mensagem, severidade, instante e indicadores. `Ioc` contém tipo e valor. `TriageVerdict` contém classificação, confiança, justificativa e técnicas MITRE. `Incident` reúne alertas, estado, histórico de fases, veredito e decisão humana. `ProposedAction` transporta ferramenta e argumentos; não executa código. Auditoria, fila, histórico e contenção simulada permanecem em memória.

Os quatro cenários são exfiltração via C2, força bruta em VPN, cifragem em massa e backup noturno. A observação do README sobre classificação incorreta de backup é um resultado relatado anteriormente, não uma medição reproduzida nesta análise.

## Achados para o backlog

| ID | Evidência no código | Consequência e encaminhamento |
|---|---|---|
| A01 | `CommandInvoker` mantém fila, pilha e histórico únicos; `deny()` não remove pendências; GUI inicia nova triagem sem reset. | Comandos de incidentes distintos podem se misturar. Isolar por incidente e testar sequência negar A/aprovar B. |
| A02 | `approve()` ignora resultados de `executeQueue()` e encerra como contenção concluída. | Resultado final pode declarar sucesso após falhas. Consolidar resultados e refletir falha parcial. |
| A03 | `ReActPlanner` executa leitura diretamente no comando. | Histórico central incompleto. Encaminhar execuções ao mecanismo auditável. |
| A04 | `triage()` encerra plano vazio antes de verificar `isActionable()`. | Pedido de revisão pode terminar sem pendência humana. Definir tratamento explícito. |
| A05 | `undoLast()` retira o comando antes de chamar `undo()`, sem capturar exceção. | Falha de compensação pode perder a possibilidade de tentativa e não registrar resultado. |
| A06 | `CommandFactory` valida nome de ferramenta e presença de alguns argumentos, sem vincular todo alvo às evidências. | A instrução no prompt não garante alvo válido. Validar hostname, IP e técnica no código. |

Esses achados decorrem de análise estática e precisam de testes de regressão para confirmação executável. A correlação atual representa uma fase do fluxo e a organização de evidências; não foi identificado um motor dedicado de correlação de eventos externos.

## Verificação realizada

Foram identificados 24 métodos `@Test` em cinco classes de testes: State, Command, Strategy, Observer e Composite. `UiSnapshot` é um utilitário separado. A tentativa de executar `mvnw.cmd -o test` falhou ao iniciar o wrapper; `java` não está disponível no PATH desta sessão. Portanto, compilação, testes, interface e inferência não foram validados em execução. O backlog inclui preparar o ambiente e registrar essa validação.

As tasks detalhadas estão em [tasks.md](tasks.md). A análise não alterou o código-fonte.
