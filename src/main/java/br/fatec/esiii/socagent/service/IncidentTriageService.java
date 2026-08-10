package br.fatec.esiii.socagent.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import br.fatec.esiii.socagent.command.CommandInvoker;
import br.fatec.esiii.socagent.domain.Alert;
import br.fatec.esiii.socagent.domain.TriageVerdict;
import br.fatec.esiii.socagent.observer.AgentEvent;
import br.fatec.esiii.socagent.observer.AgentEvent.EventType;
import br.fatec.esiii.socagent.observer.AgentEventBus;
import br.fatec.esiii.socagent.state.Incident;
import br.fatec.esiii.socagent.strategy.TriagePlanner;

/**
 * Orquestrador do agente: e onde os quatro padroes se encontram.
 *
 * <p>State define o que pode acontecer, Strategy decide o que fazer, Command
 * executa e Observer conta a todos o que aconteceu. O servico em si nao contem
 * regra de seguranca alguma — ele apenas coordena, e por isso permanece curto.
 */
@Service
public class IncidentTriageService {

    private final PlannerRegistry plannerRegistry;
    private final CommandInvoker invoker;
    private final AgentEventBus eventBus;
    private final AgentProperties properties;
    private final AtomicInteger sequence = new AtomicInteger(1);

    public IncidentTriageService(PlannerRegistry plannerRegistry, CommandInvoker invoker,
            AgentEventBus eventBus, AgentProperties properties) {
        this.plannerRegistry = plannerRegistry;
        this.invoker = invoker;
        this.eventBus = eventBus;
        this.properties = properties;
    }

    /** Abre um incidente a partir dos alertas correlacionados. */
    public Incident open(List<Alert> alerts) {
        String id = "INC-%04d".formatted(sequence.getAndIncrement());
        return new Incident(id, alerts, eventBus);
    }

    /**
     * Executa a triagem ate o ponto em que a decisao humana passa a ser
     * necessaria. Nenhuma acao destrutiva ocorre neste metodo: o State barra e
     * o Invoker recusa.
     */
    public TriagePlanner.Plan triage(Incident incident) {
        TriagePlanner planner = plannerRegistry.active();
        TriagePlanner.Plan plan = planner.plan(incident);

        eventBus.publish(AgentEvent.of(EventType.PLAN_CREATED, incident.id(),
                        "Plano gerado por " + planner.displayName(),
                        "%d comando(s) proposto(s)".formatted(plan.commands().size()))
                .with("planner", planner.id()));

        incident.triage(plan.verdict());

        if (plan.verdict().classification() == TriageVerdict.Classification.FALSE_POSITIVE) {
            incident.close("Falso positivo: " + plan.verdict().rationale());
            return plan;
        }

        incident.correlate();

        if (plan.isEmpty()) {
            incident.close("Nenhuma acao aplicavel: " + plan.verdict().rationale());
            return plan;
        }

        plan.commands().forEach(command -> invoker.enqueue(incident, command));

        if (plan.requiresApproval() || !plan.verdict().isActionable(properties.minimumConfidence())) {
            incident.requestApproval();
        } else {
            // Plano composto apenas por acoes de leitura: o Invoker ja recusaria
            // qualquer comando destrutivo que escapasse ate aqui.
            invoker.executeQueue(incident);
            incident.close("Acoes de leitura concluidas sem necessidade de contencao");
        }
        return plan;
    }

    /** Aprova o plano pendente e executa a contencao. */
    public void approve(Incident incident, String approver) {
        incident.approve(approver == null ? properties.defaultApprover() : approver);
        invoker.executeQueue(incident);
        incident.close("Contencao concluida e evidencias preservadas");
    }

    /** Nega o plano pendente; o incidente e encerrado sem qualquer acao. */
    public void deny(Incident incident, String approver, String reason) {
        incident.deny(approver == null ? properties.defaultApprover() : approver, reason);
    }

    /** Reverte a ultima contencao aplicada. */
    public void undoLastContainment(Incident incident) {
        invoker.undoLast(incident);
    }

    public PlannerRegistry planners() {
        return plannerRegistry;
    }

    public CommandInvoker invoker() {
        return invoker;
    }
}
