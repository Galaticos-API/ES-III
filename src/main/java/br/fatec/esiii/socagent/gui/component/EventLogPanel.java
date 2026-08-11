package br.fatec.esiii.socagent.gui.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;

import br.fatec.esiii.socagent.gui.theme.UiTheme;
import br.fatec.esiii.socagent.observer.AgentEvent;

/**
 * Lista rolavel dos eventos publicados pelo agente.
 *
 * <p>Cada linha traz horario, uma etiqueta colorida com o tipo do evento e a
 * descricao. As cores agrupam os eventos por natureza — transicao de estado,
 * execucao de comando, decisao humana e falha — para que a leitura durante uma
 * demonstracao seja rapida.
 */
public class EventLogPanel extends JPanel {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(java.time.ZoneId.systemDefault());

    private static final int MAX_ROWS = 400;

    private final JPanel rows = new JPanel();
    private final JScrollPane scroll;
    private final JLabel emptyState = new JLabel("Nenhum evento ainda. Inicie uma triagem.");

    public EventLogPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        rows.setBorder(BorderFactory.createEmptyBorder(UiTheme.GAP_SM, UiTheme.GAP_SM,
                UiTheme.GAP_SM, UiTheme.GAP_SM));

        emptyState.setForeground(UiTheme.muted());
        emptyState.setFont(UiTheme.fontBody());
        emptyState.setAlignmentX(Component.LEFT_ALIGNMENT);
        rows.add(emptyState);

        // Acompanhar a largura da viewport faz cada linha caber no espaco
        // disponivel; sem isto o JScrollPane respeita a largura preferida das
        // linhas e o texto e cortado por uma barra horizontal.
        JPanel holder = new WidthTrackingPanel();
        holder.setLayout(new BorderLayout());
        holder.setOpaque(false);
        holder.add(rows, BorderLayout.NORTH);

        scroll = new JScrollPane(holder);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    public void append(AgentEvent event) {
        if (emptyState.isVisible()) {
            emptyState.setVisible(false);
            rows.remove(emptyState);
        }
        if (rows.getComponentCount() >= MAX_ROWS) {
            rows.remove(0);
        }
        rows.add(buildRow(event));
        rows.revalidate();
        rows.repaint();
        SwingUtilities.invokeLater(() ->
                scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum()));
    }

    public void clear() {
        rows.removeAll();
        emptyState.setVisible(true);
        rows.add(emptyState);
        rows.revalidate();
        rows.repaint();
    }

    private JPanel buildRow(AgentEvent event) {
        JPanel row = new JPanel(new BorderLayout(UiTheme.GAP_SM, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        JLabel time = new JLabel(TIME.format(event.occurredAt()));
        time.setFont(UiTheme.fontMono());
        time.setForeground(UiTheme.muted());

        JPanel prefix = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.GAP_SM, 0));
        prefix.setOpaque(false);
        prefix.add(time);
        prefix.add(new Chip(shortLabel(event), colorFor(event)));

        String detail = event.detail() == null || event.detail().isBlank()
                ? event.title()
                : event.title() + " — " + event.detail();

        // No centro do BorderLayout o rotulo recebe a largura restante e o proprio
        // Swing acrescenta reticencias quando o texto nao cabe.
        JLabel text = new JLabel(normalize(detail));
        text.setFont(UiTheme.fontBody());
        text.setToolTipText(detail);
        text.setMinimumSize(new Dimension(0, 0));

        row.add(prefix, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        return row;
    }

    private String shortLabel(AgentEvent event) {
        return switch (event.type()) {
            case INCIDENT_CREATED -> "ABERTO";
            case STATE_CHANGED -> "ESTADO";
            case TRIAGE_COMPLETED -> "VEREDITO";
            case PLAN_CREATED -> "PLANO";
            case COMMAND_QUEUED -> "NA FILA";
            case COMMAND_EXECUTED -> "EXECUTADO";
            case COMMAND_FAILED -> "RECUSADO";
            case COMMAND_UNDONE -> "DESFEITO";
            case APPROVAL_REQUESTED -> "APROVACAO";
            case APPROVAL_GRANTED -> "APROVADO";
            case APPROVAL_DENIED -> "NEGADO";
            case INCIDENT_CLOSED -> "ENCERRADO";
        };
    }

    private Color colorFor(AgentEvent event) {
        return switch (event.type()) {
            case COMMAND_FAILED, APPROVAL_DENIED -> UiTheme.danger();
            case APPROVAL_REQUESTED -> UiTheme.warning();
            case COMMAND_EXECUTED, APPROVAL_GRANTED, INCIDENT_CLOSED -> UiTheme.success();
            case STATE_CHANGED, INCIDENT_CREATED -> UiTheme.accent();
            default -> UiTheme.muted();
        };
    }

    private String normalize(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    /** Painel que nunca fica mais largo que a viewport do JScrollPane. */
    private static class WidthTrackingPanel extends JPanel implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visible, int orientation, int direction) {
            return visible.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
