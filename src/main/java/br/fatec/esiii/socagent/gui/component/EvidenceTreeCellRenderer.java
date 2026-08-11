package br.fatec.esiii.socagent.gui.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import br.fatec.esiii.socagent.domain.Severity;
import br.fatec.esiii.socagent.gui.theme.UiTheme;
import br.fatec.esiii.socagent.gui.tree.EvidenceNode;

/**
 * Desenha cada no da arvore de evidencias com um marcador colorido de
 * severidade.
 *
 * <p>Grupos exibem a severidade agregada da subarvore e a contagem de folhas,
 * que sao justamente as operacoes recursivas do Composite. O texto sempre traz
 * o nome da severidade, entao a cor e reforco e nao a unica pista.
 */
public class EvidenceTreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
            boolean expanded, boolean leaf, int row, boolean focused) {

        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, focused);

        Object userObject = value instanceof DefaultMutableTreeNode node ? node.getUserObject() : null;
        if (!(userObject instanceof EvidenceNode evidence)) {
            setIcon(null);
            return this;
        }

        Severity severity = evidence.isLeaf() ? evidence.severity() : evidence.highestSeverity();
        Color color = UiTheme.severityColor(severity);

        if (evidence.isLeaf()) {
            setText("<html>%s <font color='%s'>%s</font></html>".formatted(
                    escape(evidence.label()), toHex(color), severity));
        } else {
            // Os parenteses sao necessarios: sem eles o formatted() se aplicaria
            // apenas ao ultimo literal da concatenacao.
            setText(("<html><b>%s</b> &nbsp;<font color='%s'>%s</font>"
                    + " <font color='%s'>&middot; %d evidencia(s)</font></html>").formatted(
                            escape(evidence.label()), toHex(color), severity,
                            toHex(UiTheme.muted()), evidence.leafCount()));
        }

        setIcon(new DotIcon(color, evidence.isLeaf()));
        setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 6));
        // Rotulos de alerta sao longos e a arvore rola na horizontal; a dica
        // permite ler o conteudo inteiro sem precisar rolar.
        setToolTipText(evidence.label());
        return this;
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String toHex(Color color) {
        return "#%02x%02x%02x".formatted(color.getRed(), color.getGreen(), color.getBlue());
    }

    /** Marcador circular: preenchido para folha, anel para grupo. */
    private record DotIcon(Color color, boolean filled) implements Icon {

        private static final int SIZE = 10;

        @Override
        public void paintIcon(Component component, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            if (filled) {
                g2.fillOval(x, y + 3, SIZE - 2, SIZE - 2);
            } else {
                g2.setStroke(new java.awt.BasicStroke(2f));
                g2.drawOval(x, y + 3, SIZE - 2, SIZE - 2);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return SIZE + 4;
        }

        @Override
        public int getIconHeight() {
            return SIZE + 4;
        }
    }
}
