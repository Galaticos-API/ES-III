package br.fatec.esiii.socagent.domain;

import java.util.List;

/**
 * Conclusao da triagem produzida pelo modelo de linguagem.
 *
 * <p>O modelo classifica e justifica, mas nao executa nada: a decisao de agir
 * pertence ao planejador (Strategy) e a execucao aos comandos (Command).
 */
public record TriageVerdict(
        Classification classification,
        double confidence,
        String rationale,
        List<String> techniqueIds) {

    public TriageVerdict {
        confidence = Math.clamp(confidence, 0.0, 1.0);
        techniqueIds = techniqueIds == null ? List.of() : List.copyOf(techniqueIds);
    }

    public enum Classification {
        TRUE_POSITIVE,
        FALSE_POSITIVE,
        NEEDS_HUMAN_REVIEW
    }

    public static TriageVerdict needsReview(String rationale) {
        return new TriageVerdict(Classification.NEEDS_HUMAN_REVIEW, 0.0, rationale, List.of());
    }

    /** Confianca insuficiente obriga revisao humana, independente da classificacao. */
    public boolean isActionable(double minimumConfidence) {
        return classification == Classification.TRUE_POSITIVE && confidence >= minimumConfidence;
    }
}
