# Conferência da interface sem Ollama

Conferência em 28/10/2026. Imagens geradas pelo usuário com verificar-interface.ps1 e analista fixo. Inspeção visual realizada nas 12 capturas de target/ui-snapshots. Nenhuma imagem representa classificação feita pelo modelo real.

| Estado | Tema claro | Tema escuro | Resultado observado |
|---|---|---|---|
| Vazio | Conferido | Conferido | Sem incidente, sem eventos, Desfazer desabilitado |
| Aguardando aprovação | Conferido | Conferido | Fase de espera destacada; Aprovar e Negar visíveis; nova triagem desabilitada |
| Encerrado com execução | Conferido | Conferido | Caminho completo; ações no log; duas ações reversíveis; Desfazer habilitado |
| Revisão sem ações | Conferido | Conferido | Encerrado; contenção não realizada; decisão identificada como revisão |
| Negado | Conferido | Conferido | Negação e encerramento no log; contenção não realizada; sem ações reversíveis |
| Falso positivo | Conferido | Conferido | Encerramento após triagem; correlação, aprovação e contenção não realizadas |

Nos dois temas, os painéis de detalhes estão presentes e os controles principais aparecem dentro da janela de 1280 × 800 usada pelo utilitário. Linhas longas na árvore e no registro continuam resumidas pelo espaço disponível; o texto completo é apresentado no painel inferior ao selecionar uma evidência ou clicar em um evento. A presença desses painéis foi conferida, mas a interação de seleção e cópia não é comprovada pelas capturas estáticas.

## Conferência interativa que ainda falta

- Selecionar uma evidência longa e conferir seu texto completo no painel inferior.
- Clicar em evento longo, selecionar o texto e copiar.
- Trocar tema com decisão pendente e conferir preservação do incidente, cenário e eventos.
- Redimensionar a janela até o tamanho mínimo e ajustar divisores/rolagens.
- Aprovar, negar e desfazer pela própria GUI; os comportamentos do domínio já têm testes e as telas usam cenários fixos.

Não instalar ferramentas extras para esta conferência. O script de capturas usa apenas as dependências Java do próprio projeto. A validação com Ollama e novos alertas permanece separada.