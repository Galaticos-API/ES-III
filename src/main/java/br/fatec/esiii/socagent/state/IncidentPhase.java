package br.fatec.esiii.socagent.state;

/**
 * Fases do ciclo de vida de um incidente de seguranca.
 *
 * <p>Existe separada das classes de estado para que a GUI e os relatorios
 * possam referenciar a fase sem depender da implementacao do comportamento.
 */
public enum IncidentPhase {

    RECEIVED("Recebido"),
    TRIAGING("Em triagem"),
    CORRELATING("Correlacionando"),
    AWAITING_APPROVAL("Aguardando aprovacao"),
    CONTAINING("Em contencao"),
    CLOSED("Encerrado");

    private final String label;

    IncidentPhase(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean isTerminal() {
        return this == CLOSED;
    }
}
