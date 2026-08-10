package br.fatec.esiii.socagent.state;

import br.fatec.esiii.socagent.domain.TriageVerdict;

/**
 * Estados concretos do incidente, agrupados como instancias unicas.
 *
 * <p>Fluxo permitido:
 * <pre>
 * RECEIVED -> TRIAGING -> CORRELATING -> AWAITING_APPROVAL -> CONTAINING -> CLOSED
 *                  |            |                |
 *                  +------------+----------------+--> CLOSED (falso positivo ou negado)
 * </pre>
 */
public final class IncidentStates {

    public static final IncidentState RECEIVED = new ReceivedState();
    public static final IncidentState TRIAGING = new TriagingState();
    public static final IncidentState CORRELATING = new CorrelatingState();
    public static final IncidentState AWAITING_APPROVAL = new AwaitingApprovalState();
    public static final IncidentState CONTAINING = new ContainingState();
    public static final IncidentState CLOSED = new ClosedState();

    private IncidentStates() {
    }

    /** Incidente recem-criado a partir de um ou mais alertas. */
    private static final class ReceivedState implements IncidentState {
        @Override
        public IncidentPhase phase() {
            return IncidentPhase.RECEIVED;
        }

        @Override
        public IncidentState triage(Incident incident, TriageVerdict verdict) {
            incident.recordVerdict(verdict);
            return TRIAGING;
        }
    }

    /** O modelo ja classificou; decide-se entre aprofundar ou encerrar. */
    private static final class TriagingState implements IncidentState {
        @Override
        public IncidentPhase phase() {
            return IncidentPhase.TRIAGING;
        }

        @Override
        public IncidentState correlate(Incident incident) {
            return CORRELATING;
        }

        /** Falso positivo encerra sem passar por contencao. */
        @Override
        public IncidentState close(Incident incident, String summary) {
            incident.recordClosure(summary);
            return CLOSED;
        }
    }

    /** Evidencias reunidas; o plano de resposta pode ser submetido a aprovacao. */
    private static final class CorrelatingState implements IncidentState {
        @Override
        public IncidentPhase phase() {
            return IncidentPhase.CORRELATING;
        }

        @Override
        public IncidentState requestApproval(Incident incident) {
            return AWAITING_APPROVAL;
        }

        @Override
        public IncidentState close(Incident incident, String summary) {
            incident.recordClosure(summary);
            return CLOSED;
        }
    }

    /**
     * Barreira de governanca: nenhuma acao destrutiva ocorre sem decisao humana.
     * E o estado que justifica o padrao no dominio de seguranca.
     */
    private static final class AwaitingApprovalState implements IncidentState {
        @Override
        public IncidentPhase phase() {
            return IncidentPhase.AWAITING_APPROVAL;
        }

        @Override
        public IncidentState approve(Incident incident, String approver) {
            incident.recordApproval(approver, true, null);
            return CONTAINING;
        }

        @Override
        public IncidentState deny(Incident incident, String approver, String reason) {
            incident.recordApproval(approver, false, reason);
            incident.recordClosure("Contencao negada por " + approver + ": " + reason);
            return CLOSED;
        }
    }

    /** Unica fase em que comandos de contencao podem ser executados. */
    private static final class ContainingState implements IncidentState {
        @Override
        public IncidentPhase phase() {
            return IncidentPhase.CONTAINING;
        }

        @Override
        public boolean allowsContainmentCommands() {
            return true;
        }

        @Override
        public IncidentState close(Incident incident, String summary) {
            incident.recordClosure(summary);
            return CLOSED;
        }
    }

    /** Estado terminal: nenhuma operacao e aceita. */
    private static final class ClosedState implements IncidentState {
        @Override
        public IncidentPhase phase() {
            return IncidentPhase.CLOSED;
        }
    }
}
