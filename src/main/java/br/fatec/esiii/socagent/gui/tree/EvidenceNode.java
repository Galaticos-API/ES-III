package br.fatec.esiii.socagent.gui.tree;

import java.util.List;

import br.fatec.esiii.socagent.domain.Severity;

/**
 * Componente do padrao Composite: um no da arvore de evidencias.
 *
 * <p>Cliente algum precisa saber se esta lidando com uma folha (um IOC isolado)
 * ou com um grupo (um alerta com varios indicadores). As operacoes recursivas
 * ficam aqui como metodos padrao, de modo que folha e composto respondem
 * exatamente a mesma interface — que e o proposito do padrao.
 *
 * <p>A GUI consome esta arvore para montar o {@code JTree}, e o relatorio em
 * texto usa {@link #render(int)}. Nenhum dos dois conhece as classes concretas.
 */
public interface EvidenceNode {

    String label();

    /** Severidade propria do no. Grupos costumam derivar a sua dos filhos. */
    Severity severity();

    List<EvidenceNode> children();

    default boolean isLeaf() {
        return children().isEmpty();
    }

    /**
     * Adiciona um filho. Folhas recusam, o que mantem a transparencia da
     * interface sem permitir composicao invalida.
     */
    default void add(EvidenceNode child) {
        throw new UnsupportedOperationException(
                "'%s' e uma folha e nao aceita filhos".formatted(label()));
    }

    /** Conta as folhas de toda a subarvore. Operacao recursiva uniforme. */
    default int leafCount() {
        if (isLeaf()) {
            return 1;
        }
        return children().stream().mapToInt(EvidenceNode::leafCount).sum();
    }

    /** Maior severidade da subarvore, incluindo a do proprio no. */
    default Severity highestSeverity() {
        return children().stream()
                .map(EvidenceNode::highestSeverity)
                .reduce(severity(), Severity::max);
    }

    /** Profundidade maxima da subarvore, usada em diagnostico. */
    default int depth() {
        return isLeaf() ? 1 : 1 + children().stream().mapToInt(EvidenceNode::depth).max().orElse(0);
    }

    /** Representacao textual indentada, util em relatorio e em teste. */
    default String render(int indent) {
        StringBuilder builder = new StringBuilder();
        builder.append("  ".repeat(indent))
                .append(isLeaf() ? "- " : "+ ")
                .append(label())
                .append(" [").append(severity()).append("]")
                .append(System.lineSeparator());
        children().forEach(child -> builder.append(child.render(indent + 1)));
        return builder.toString();
    }
}
