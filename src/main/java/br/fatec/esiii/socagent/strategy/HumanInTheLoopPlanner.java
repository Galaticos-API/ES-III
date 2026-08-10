package br.fatec.esiii.socagent.strategy;

import org.springframework.stereotype.Component;

import br.fatec.esiii.socagent.state.Incident;

/**
 * Estrategia que envolve outra e forca aprovacao humana em qualquer plano nao
 * vazio, mesmo quando composto apenas por acoes de leitura.
 *
 * <p>E um Strategy que tambem se comporta como Decorator: acrescenta
 * governanca sem duplicar o planejamento. Em ambientes regulados costuma ser a
 * unica configuracao aceitavel em producao.
 */
@Component
public class HumanInTheLoopPlanner implements TriagePlanner {

    private final TriagePlanner delegate;

    public HumanInTheLoopPlanner(PlanThenExecutePlanner delegate) {
        this.delegate = delegate;
    }

    @Override
    public String id() {
        return "human-in-the-loop";
    }

    @Override
    public String displayName() {
        return "Humano no circuito (aprovacao sempre)";
    }

    @Override
    public Plan plan(Incident incident) {
        Plan original = delegate.plan(incident);
        if (original.isEmpty()) {
            return original;
        }
        return new Plan(original.verdict(), original.commands(), true);
    }

    public String delegateId() {
        return delegate.id();
    }
}
