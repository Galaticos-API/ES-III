package br.fatec.esiii.socagent.state;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import br.fatec.esiii.socagent.domain.Alert;
import br.fatec.esiii.socagent.domain.Severity;
import br.fatec.esiii.socagent.domain.TriageVerdict;
import br.fatec.esiii.socagent.observer.AgentEvent;
import br.fatec.esiii.socagent.observer.AgentEvent.EventType;
import br.fatec.esiii.socagent.observer.AgentEventBus;

/**
 * Contexto do padrao State: agrega os alertas de um incidente e delega todo o
 * comportamento dependente de fase ao {@link IncidentState} corrente.
 *
 * <p>Repare que nao existe nenhum {@code switch} sobre a fase. Adicionar uma
 * nova fase significa criar uma classe de estado, sem tocar neste arquivo.
 *
 * <p>Cada transicao publica um evento no {@link AgentEventBus}, ligando o
 * padrao State ao Observer.
 */
public class Incident {

    private final String id;
    private final List<Alert> alerts;
    private final Instant createdAt;
    private final AgentEventBus eventBus;
    private final List<IncidentPhase> phaseHistory = new ArrayList<>();

    private IncidentState state = IncidentStates.RECEIVED;
    private TriageVerdict verdict;
    private String approver;
    private String approvalReason;
    private boolean approved;
    private String closingSummary;

    public Incident(String id, List<Alert> alerts, AgentEventBus eventBus) {
        this.id = Objects.requireNonNull(id, "id nao pode ser nulo");
        this.alerts = List.copyOf(Objects.requireNonNull(alerts, "alerts nao pode ser nulo"));
        if (this.alerts.isEmpty()) {
            throw new IllegalArgumentException("um incidente exige ao menos um alerta");
        }
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus nao pode ser nulo");
        this.createdAt = Instant.now();
        this.phaseHistory.add(state.phase());
        eventBus.publish(AgentEvent.of(EventType.INCIDENT_CREATED, id,
                "Incidente aberto com %d alerta(s)".formatted(this.alerts.size()),
                "severidade maxima " + highestSeverity()));
    }

    // ---------------------------------------------------------------
    // Operacoes delegadas ao estado corrente
    // ---------------------------------------------------------------

    public void triage(TriageVerdict newVerdict) {
        transitionTo(state.triage(this, newVerdict), "triagem concluida");
    }

    public void correlate() {
        transitionTo(state.correlate(this), "correlacao de evidencias iniciada");
    }

    public void requestApproval() {
        transitionTo(state.requestApproval(this), "plano submetido a aprovacao humana");
        eventBus.publish(AgentEvent.of(EventType.APPROVAL_REQUESTED, id,
                "Aprovacao humana requerida", "o agente nao prossegue sem decisao"));
    }

    public void approve(String who) {
        transitionTo(state.approve(this, who), "contencao autorizada por " + who);
        eventBus.publish(AgentEvent.of(EventType.APPROVAL_GRANTED, id,
                "Contencao aprovada", "responsavel: " + who));
    }

    public void deny(String who, String reason) {
        transitionTo(state.deny(this, who, reason), "contencao negada por " + who);
        eventBus.publish(AgentEvent.of(EventType.APPROVAL_DENIED, id,
                "Contencao negada", reason));
    }

    public void close(String summary) {
        transitionTo(state.close(this, summary), "incidente encerrado");
        eventBus.publish(AgentEvent.of(EventType.INCIDENT_CLOSED, id, "Incidente encerrado", summary));
    }

    private void transitionTo(IncidentState next, String reason) {
        IncidentPhase from = state.phase();
        this.state = next;
        this.phaseHistory.add(next.phase());
        eventBus.publish(AgentEvent.of(EventType.STATE_CHANGED, id,
                        "%s -> %s".formatted(from.label(), next.phase().label()), reason)
                .with("from", from)
                .with("to", next.phase()));
    }

    // ---------------------------------------------------------------
    // Registros feitos pelos estados concretos
    // ---------------------------------------------------------------

    void recordVerdict(TriageVerdict newVerdict) {
        this.verdict = newVerdict;
        eventBus.publish(AgentEvent.of(EventType.TRIAGE_COMPLETED, id,
                "Veredito: " + newVerdict.classification(),
                "confianca %.2f -- %s".formatted(newVerdict.confidence(), newVerdict.rationale())));
    }

    void recordApproval(String who, boolean granted, String reason) {
        this.approver = who;
        this.approved = granted;
        this.approvalReason = reason;
    }

    void recordClosure(String summary) {
        this.closingSummary = summary;
    }

    // ---------------------------------------------------------------
    // Consultas
    // ---------------------------------------------------------------

    public String id() {
        return id;
    }

    public List<Alert> alerts() {
        return alerts;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public IncidentPhase phase() {
        return state.phase();
    }

    public boolean allowsContainmentCommands() {
        return state.allowsContainmentCommands();
    }

    public Optional<TriageVerdict> verdict() {
        return Optional.ofNullable(verdict);
    }

    public Optional<String> approver() {
        return Optional.ofNullable(approver);
    }

    public Optional<String> approvalReason() {
        return Optional.ofNullable(approvalReason);
    }

    public boolean isApproved() {
        return approved;
    }

    public Optional<String> closingSummary() {
        return Optional.ofNullable(closingSummary);
    }

    public List<IncidentPhase> phaseHistory() {
        return List.copyOf(phaseHistory);
    }

    public Severity highestSeverity() {
        return alerts.stream()
                .map(Alert::severity)
                .reduce(Severity.INFO, Severity::max);
    }

    public String affectedHost() {
        return alerts.getFirst().hostname();
    }
}
