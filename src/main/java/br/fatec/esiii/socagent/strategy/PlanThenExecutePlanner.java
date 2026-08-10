package br.fatec.esiii.socagent.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import br.fatec.esiii.socagent.command.AgentCommand;
import br.fatec.esiii.socagent.domain.TriageVerdict;
import br.fatec.esiii.socagent.state.Incident;

/**
 * Estrategia que pede o plano completo ao modelo em uma unica interacao e
 * so depois executa.
 *
 * <p>Vantagem: previsivel, barato e auditavel — o plano inteiro pode ser
 * inspecionado antes de qualquer efeito colateral. Desvantagem: nao se adapta
 * ao que for descoberto durante a execucao.
 */
@Component
public class PlanThenExecutePlanner implements TriagePlanner {

    private final ThreatAnalyst analyst;
    private final CommandFactory commandFactory;

    public PlanThenExecutePlanner(ThreatAnalyst analyst, CommandFactory commandFactory) {
        this.analyst = analyst;
        this.commandFactory = commandFactory;
    }

    @Override
    public String id() {
        return "plan-then-execute";
    }

    @Override
    public String displayName() {
        return "Planejar e depois executar";
    }

    @Override
    public Plan plan(Incident incident) {
        TriageVerdict verdict = analyst.classify(incident);

        if (verdict.classification() == TriageVerdict.Classification.FALSE_POSITIVE) {
            return new Plan(verdict, List.of(), false);
        }

        List<ProposedAction> proposals = analyst.proposePlan(incident);
        List<AgentCommand> commands = commandFactory.createAll(proposals, incident.affectedHost());

        boolean needsApproval = commands.stream().anyMatch(AgentCommand::requiresApproval);
        return new Plan(verdict, commands, needsApproval);
    }
}
