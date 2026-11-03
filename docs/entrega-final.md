# Entrega e apresentação

## Situação atual

O projeto compila e os 34 testes passaram em 28/10/2026, sem falhas ou erros, conforme relatórios Surefire após execução no PowerShell do usuário. As 12 capturas com analista fixo foram inspecionadas. State, Command, Strategy, Observer e Composite já estão implementados; não é necessário desenvolvê-los novamente.

Foram corrigidos isolamento de comandos por incidente, cancelamento de pendências negadas, conclusão conforme resultados, revisão sem contenção, identificação de fases puladas e falha de reversão. A GUI oferece detalhes selecionáveis e preserva dados ao trocar tema. As interações ainda exigem conferência na janela; as capturas comprovam renderização, não todos os cliques.

## Arquivos para o professor

- README.md: requisitos, execução no Windows, testes e limitações.
- pom.xml, mvnw, mvnw.cmd e .mvn: construção pelo Maven.
- src/main: aplicação Java e configuração Spring AI/Ollama.
- src/test: testes e utilitário de capturas sem modelo real.
- docs/arquitetura.md: explicação dos cinco padrões, UML em imagens e Mermaid.
- docs/uml: cinco imagens UML prontas para abrir ou inserir em apresentação.
- docs/conferencia-interface.md: evidências visuais e limites da verificação.
- docs/tasks.md: tarefas de encerramento e pendências reais.

O DOCX antigo guia-de-apresentacao.docx não foi revisado e não é incluído no novo pacote. Este arquivo é o roteiro atual. Engenharia-reversa.md é um registro histórico, anterior às correções, identificado como tal.

## Preparar o pacote

Na pasta do pom.xml:

```powershell
.\preparar-entrega.ps1
```

O ZIP é criado em target/entrega. Ele contém somente fontes, configuração Maven e documentação; não contém .git, target, dependências baixadas, modelo ou credenciais. A primeira construção em outro computador pode precisar de internet para baixar Maven e dependências. O pacote ainda depende da conferência interativa e da demonstração com Ollama antes de ser tratado como entrega totalmente validada.

## Divisão sugerida para dez integrantes

| Integrante | Tema da fala | Mostrar |
|---|---|---|
| 1 | Problema e objetivo | Triagem de alertas e decisão humana |
| 2 | Tecnologias e modelo aberto | POM, YAML e integração local |
| 3 | State | IncidentState, estados e revisão sem contenção |
| 4 | Command | Fila por incidente, execução e desfazer |
| 5 | Strategy | Três planejadores e seleção em runtime |
| 6 | Observer | Barramento, auditoria, console e GUI |
| 7 | Composite | Grupos/folhas, severidade e árvore Swing |
| 8 | Fluxo e demonstração | Aprovar, negar, concluir revisão e resultados |
| 9 | Testes e evidências | 34 testes e capturas com analista fixo |
| 10 | Limitações e conclusão | Simulação, ausência de persistência e avaliação com IA |

A divisão é sugestão; preencher nomes e duração conforme a orientação do professor. Todos devem conhecer os cinco padrões, mesmo que apresentem somente um bloco.

## Falas curtas

**Objetivo:** Nosso agente recebe alertas fictícios, usa um modelo aberto para classificar o incidente e sugere ações. O operador decide antes da contenção.

**Tecnologias:** A aplicação é Java com Spring AI e interface Swing. O modelo configurado é Qwen 2.5 7B pelo Ollama. Não usamos chave de API para o modelo local.

**State:** Cada estado permite apenas suas operações. Aprovar pode liberar a contenção; concluir uma revisão sem ações encerra diretamente.

**Command:** Cada ferramenta é um objeto Java. O invocador controla fila, resultados e reversão por incidente e recusa contenção fora da fase autorizada.

**Strategy:** Podemos trocar o planejamento sem mudar o serviço: plano completo, investigação ReAct e aprovação humana para todo plano não vazio.

**Observer:** O mesmo evento alimenta console, auditoria e GUI. Um observador com falha não interrompe os demais.

**Composite:** A árvore usa uma interface comum para grupos e folhas. Assim contamos evidências e agregamos severidade recursivamente.

**Validação:** Os testes usam respostas controladas, sem Ollama. Eles verificam regras e falhas; as capturas mostram os estados da GUI. Isso não mede a qualidade da IA.

**Limites:** As ações são simuladas em memória. A correlação atual organiza evidências no fluxo; não há ingestão externa nem contenção real. A classificação pelo modelo pode errar, por isso existe decisão humana.

## Demonstração a ensaiar

1. Mostrar cenário, estratégia e árvore.
2. Triar com Ollama quando instalado e explicar o veredito que realmente aparecer.
3. Se houver ações, mostrar a espera e aprovar ou negar.
4. Mostrar resultado e desfazer uma ação reversível simulada.
5. Mostrar os testes/capturas para os ramos que o modelo não produziu ao vivo.
6. Explicar revisão sem ações, falso positivo e falhas parciais.

Não prometer saída idêntica da IA em execuções diferentes. Não apresentar a resposta do analista fixo como resultado do modelo real.

## O que falta

- Conferência interativa listada em conferencia-interface.md.
- Instalação do Ollama e demonstração com modelo real, adiada pelo usuário.
- Atribuição de nomes, duração e ensaio da equipe.
- Opcional: abrir os blocos Mermaid no visualizador utilizado pela equipe; as cinco imagens dos padrões já podem ser abertas diretamente.