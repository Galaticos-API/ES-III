package br.fatec.esiii.socagent.domain;

/**
 * Severidade de um alerta, alinhada aos niveis usuais de um SOC.
 * O peso permite comparacao e agregacao na arvore de evidencias.
 */
public enum Severity {

    INFO(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int weight;

    Severity(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }

    /** Severidade mais alta entre duas. Usada na agregacao recursiva do Composite. */
    public Severity max(Severity other) {
        return other == null || this.weight >= other.weight ? this : other;
    }

    /** Acoes de contencao so podem ser propostas a partir deste nivel. */
    public boolean requiresContainment() {
        return weight >= HIGH.weight;
    }
}
