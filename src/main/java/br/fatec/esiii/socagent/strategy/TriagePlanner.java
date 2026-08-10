package br.fatec.esiii.socagent.strategy;

import java.util.List;

import br.fatec.esiii.socagent.command.AgentCommand;
import br.fatec.esiii.socagent.domain.TriageVerdict;
import br.fatec.esiii.socagent.state.Incident;

/**
 * Estrategia de planejamento da resposta (padrao Strategy).
 *
 * <p>O runtime do agente conhece apenas esta interface. Trocar de
 * {@link ReActPlanner} para {@link PlanThenExecutePlanner} nao altera uma linha
 * do orquestrador, e envolver qualquer um deles em
 * {@link HumanInTheLoopPlanner} acrescenta governanca sem modifica-los.
 */
public interface TriagePlanner {

    /** Identificador tecnico, usado em configuracao e metricas. */
    String id();

    /** Nome exibido na GUI ao selecionar a estrategia. */
    String displayName();

    Plan plan(Incident incident);

    /**
     * Resultado do planejamento.
     *
     * @param verdict          classificacao produzida pelo modelo
     * @param commands         comandos ja traduzidos e validados
     * @param requiresApproval se a estrategia exige decisao humana antes de executar
     */
    record Plan(TriageVerdict verdict, List<AgentCommand> commands, boolean requiresApproval) {

        public Plan {
            commands = commands == null ? List.of() : List.copyOf(commands);
        }

        public boolean isEmpty() {
            return commands.isEmpty();
        }
    }
}
