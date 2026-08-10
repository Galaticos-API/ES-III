package br.fatec.esiii.socagent.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import br.fatec.esiii.socagent.strategy.TriagePlanner;

/**
 * Guarda as estrategias disponiveis e qual esta ativa.
 *
 * <p>E o que torna o padrao Strategy visivel na GUI: o operador troca o
 * planejador em tempo de execucao e o orquestrador nao percebe a diferenca,
 * porque continua conversando apenas com {@link TriagePlanner}.
 */
@Component
public class PlannerRegistry {

    private final Map<String, TriagePlanner> planners = new LinkedHashMap<>();
    private TriagePlanner active;

    public PlannerRegistry(List<TriagePlanner> available, AgentProperties properties) {
        available.forEach(planner -> planners.put(planner.id(), planner));
        this.active = planners.getOrDefault(properties.planner(), available.getFirst());
    }

    public TriagePlanner active() {
        return active;
    }

    public void activate(String plannerId) {
        TriagePlanner selected = planners.get(plannerId);
        if (selected == null) {
            throw new IllegalArgumentException("Estrategia desconhecida: " + plannerId);
        }
        this.active = selected;
    }

    public List<TriagePlanner> available() {
        return List.copyOf(planners.values());
    }
}
