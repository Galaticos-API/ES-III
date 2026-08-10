package br.fatec.esiii.socagent.state;

import br.fatec.esiii.socagent.domain.TriageVerdict;

/**
 * Estado do incidente (padrao State).
 *
 * <p>Cada operacao devolve o proximo estado. As implementacoes padrao lancam
 * {@link IllegalTransitionException}, de modo que um estado concreto so precisa
 * sobrescrever as operacoes que realmente permite. Transicao ilegal deixa de
 * ser um {@code if} espalhado pelo servico e passa a ser impossivel por
 * construcao.
 *
 * <p>Os estados sao imutaveis e sem campos, portanto compartilhaveis como
 * instancias unicas.
 */
public interface IncidentState {

    IncidentPhase phase();

    default IncidentState triage(Incident incident, TriageVerdict verdict) {
        throw new IllegalTransitionException(phase(), "triar");
    }

    default IncidentState correlate(Incident incident) {
        throw new IllegalTransitionException(phase(), "correlacionar");
    }

    default IncidentState requestApproval(Incident incident) {
        throw new IllegalTransitionException(phase(), "solicitar aprovacao");
    }

    default IncidentState approve(Incident incident, String approver) {
        throw new IllegalTransitionException(phase(), "aprovar");
    }

    default IncidentState deny(Incident incident, String approver, String reason) {
        throw new IllegalTransitionException(phase(), "negar aprovacao");
    }

    default IncidentState contain(Incident incident) {
        throw new IllegalTransitionException(phase(), "conter");
    }

    default IncidentState close(Incident incident, String summary) {
        throw new IllegalTransitionException(phase(), "encerrar");
    }

    /** Indica se comandos de contencao podem ser executados nesta fase. */
    default boolean allowsContainmentCommands() {
        return false;
    }
}
