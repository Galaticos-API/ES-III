package br.fatec.esiii.socagent.gui.tree;

import java.util.ArrayList;
import java.util.List;

import br.fatec.esiii.socagent.domain.Severity;

/**
 * Composto da arvore de evidencias: agrupa outros nos, que podem ser folhas ou
 * novos grupos.
 *
 * <p>A severidade propria e apenas um piso; {@link #highestSeverity()} agrega
 * recursivamente os filhos. Assim, um grupo marcado como LOW que contenha um
 * IOC CRITICAL aparece como CRITICAL na GUI, sem que ninguem precise
 * recalcular isso manualmente.
 */
public class EvidenceGroup implements EvidenceNode {

    private final String label;
    private final Severity ownSeverity;
    private final List<EvidenceNode> children = new ArrayList<>();

    public EvidenceGroup(String label, Severity ownSeverity) {
        this.label = label;
        this.ownSeverity = ownSeverity == null ? Severity.INFO : ownSeverity;
    }

    public EvidenceGroup(String label) {
        this(label, Severity.INFO);
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public Severity severity() {
        return ownSeverity;
    }

    @Override
    public List<EvidenceNode> children() {
        return List.copyOf(children);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public void add(EvidenceNode child) {
        if (child != null) {
            children.add(child);
        }
    }

    /** Encadeamento conveniente para montar a arvore em uma expressao. */
    public EvidenceGroup with(EvidenceNode child) {
        add(child);
        return this;
    }
}
