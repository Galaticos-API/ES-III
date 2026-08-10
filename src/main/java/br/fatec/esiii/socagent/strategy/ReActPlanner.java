package br.fatec.esiii.socagent.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.fatec.esiii.socagent.command.AgentCommand;
import br.fatec.esiii.socagent.domain.TriageVerdict;
import br.fatec.esiii.socagent.state.Incident;

/**
 * Estrategia ReAct: alterna raciocinio e acao, decidindo o proximo passo a
 * partir do que ja foi observado.
 *
 * <p>Somente comandos de leitura sao executados durante o ciclo; qualquer acao
 * destrutiva e acumulada para depois da aprovacao. Sem essa separacao, o ciclo
 * adaptativo contornaria a barreira de governanca.
 *
 * <p>O limite de passos evita que um modelo indeciso itere indefinidamente.
 */
@Component
public class ReActPlanner implements TriagePlanner {

    private static final int MAX_STEPS = 5;

    private final ThreatAnalyst analyst;
    private final CommandFactory commandFactory;

    public ReActPlanner(ThreatAnalyst analyst, CommandFactory commandFactory) {
        this.analyst = analyst;
        this.commandFactory = commandFactory;
    }

    @Override
    public String id() {
        return "react";
    }

    @Override
    public String displayName() {
        return "ReAct (raciocinio e acao alternados)";
    }

    @Override
    public Plan plan(Incident incident) {
        TriageVerdict verdict = analyst.classify(incident);

        if (verdict.classification() == TriageVerdict.Classification.FALSE_POSITIVE) {
            return new Plan(verdict, List.of(), false);
        }

        List<String> observations = new ArrayList<>();
        List<AgentCommand> pendingContainment = new ArrayList<>();

        for (int step = 0; step < MAX_STEPS; step++) {
            Optional<ProposedAction> next = analyst.proposeNext(incident, observations);
            if (next.isEmpty()) {
                break;
            }

            Optional<AgentCommand> command = commandFactory.create(next.get(), incident.affectedHost());
            if (command.isEmpty()) {
                observations.add("Acao '%s' rejeitada pela lista de permissao".formatted(next.get().tool()));
                continue;
            }

            AgentCommand candidate = command.get();
            if (candidate.requiresApproval()) {
                pendingContainment.add(candidate);
                observations.add("Acao '%s' acumulada para apos aprovacao humana".formatted(candidate.name()));
            } else {
                observations.add(candidate.execute().output());
            }
        }

        boolean needsApproval = !pendingContainment.isEmpty();
        return new Plan(verdict, pendingContainment, needsApproval);
    }
}
