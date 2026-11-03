# Tasks de encerramento

Atualizado em 28/10/2026. Escopo: aplicação Java com Spring AI, modelo aberto, State, Command, Strategy, Observer e Composite na GUI, explicados com UML. O backlog inicial de 20 itens incluía evoluções; ele não deve ser usado como lista de requisitos obrigatórios.

| ID | Entrega | Estado e evidência |
|---|---|---|
| T01 | Isolamento dos comandos e cancelamento ao negar | Concluída: implementação e regressões na suíte aprovada |
| T02 | Resultados de sucesso, falha e execução parcial | Concluída: implementação e testes do serviço aprovados |
| T03 | Revisão humana sem ações, sem fase de contenção | Concluída: implementação, testes e capturas conferidos |
| T04 | Compilação e testes sem Ollama | Concluída: 34 testes, zero falhas/erros nos relatórios Surefire |
| T05 | Renderização dos seis estados nos dois temas | Concluída: 12 capturas inspecionadas; detalhes em conferencia-interface.md |
| T06 | UML e correspondência com o Java | Documentação revisada; cinco imagens UML geradas e conferidas; Mermaid atualizado. Blocos Mermaid não renderizados neste executor |
| T07 | README e pacote de fontes | Instruções consolidadas; preparar-entrega.ps1 gera ZIP sem .git, builds ou credenciais |
| T08 | Interações da GUI | Pendente: clique/seleção/cópia, troca de tema, redimensionamento e botões na janela |
| T09 | Integração e cenários com IA | Pendente: instalar/iniciar Ollama, baixar modelo e demonstrar classificação/plano reais |
| T10 | Apresentação da equipe | Roteiro e divisão sugerida prontos; falta atribuir nomes e ensaiar |

Não incluir login, banco, versão web ou firewall real como pendências obrigatórias. As ações do projeto são simuladas em memória. Testes com analista falso não substituem a execução de T09.