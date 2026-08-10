package br.fatec.esiii.socagent.ai;

import java.util.List;

/**
 * Estruturas que o modelo preenche via saida estruturada do Spring AI.
 *
 * <p>Sao planas de proposito: modelos de 7B lidam mal com objetos aninhados e
 * mapas livres, e campos nomeados reduzem a chance de alucinacao de formato.
 */
public final class AnalystResponses {

    private AnalystResponses() {
    }

    /** Classificacao produzida pelo modelo. */
    public record VerdictResponse(
            String classification,
            Double confidence,
            String rationale,
            List<String> techniqueIds) {
    }

    /** Acao sugerida, com todos os argumentos possiveis como campos opcionais. */
    public record ActionResponse(
            String tool,
            String hostname,
            String ipAddress,
            String techniqueId,
            String reason) {
    }

    /** Plano completo devolvido em uma unica interacao. */
    public record PlanResponse(List<ActionResponse> actions) {
    }

    /** Proximo passo do ciclo ReAct. {@code done} encerra o ciclo. */
    public record NextStepResponse(Boolean done, ActionResponse action, String thought) {
    }
}
