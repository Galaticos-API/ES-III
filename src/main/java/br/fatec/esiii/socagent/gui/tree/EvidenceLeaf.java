package br.fatec.esiii.socagent.gui.tree;

import java.util.List;

import br.fatec.esiii.socagent.domain.Ioc;
import br.fatec.esiii.socagent.domain.MitreTechnique;
import br.fatec.esiii.socagent.domain.Severity;

/**
 * Folha da arvore de evidencias: um indicador, uma tecnica ou uma observacao
 * que nao se decompoe.
 */
public record EvidenceLeaf(String label, Severity severity) implements EvidenceNode {

    @Override
    public List<EvidenceNode> children() {
        return List.of();
    }

    public static EvidenceLeaf ofIoc(Ioc ioc, Severity severity) {
        return new EvidenceLeaf("%s: %s".formatted(ioc.type(), ioc.value()), severity);
    }

    public static EvidenceLeaf ofTechnique(MitreTechnique technique) {
        return new EvidenceLeaf(technique.toString(), Severity.MEDIUM);
    }

    public static EvidenceLeaf ofNote(String text) {
        return new EvidenceLeaf(text, Severity.INFO);
    }
}
