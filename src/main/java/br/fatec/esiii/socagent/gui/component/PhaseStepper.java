package br.fatec.esiii.socagent.gui.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import br.fatec.esiii.socagent.gui.theme.UiTheme;
import br.fatec.esiii.socagent.state.IncidentPhase;

/**
 * Trilha horizontal das fases do incidente.
 *
 * <p>Torna a maquina de estados visivel: o operador enxerga onde o incidente
 * esta, o que ja passou e o que falta. A fase de aprovacao recebe destaque
 * proprio por ser a unica que exige acao humana.
 */
public class PhaseStepper extends JPanel {

    private static final IncidentPhase[] TRACK = {
            IncidentPhase.RECEIVED,
            IncidentPhase.TRIAGING,
            IncidentPhase.CORRELATING,
            IncidentPhase.AWAITING_APPROVAL,
            IncidentPhase.CONTAINING,
            IncidentPhase.CLOSED};

    private IncidentPhase current;

    public PhaseStepper() {
        setOpaque(false);
        setPreferredSize(new Dimension(0, 56));
        setToolTipText("Padrao State: cada etapa e uma classe de estado; "
                + "transicoes ilegais lancam excecao");
    }

    public void setPhase(IncidentPhase phase) {
        this.current = phase;
        repaint();
    }

    private int indexOfCurrent() {
        if (current == null) {
            return -1;
        }
        for (int i = 0; i < TRACK.length; i++) {
            if (TRACK[i] == current) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setFont(UiTheme.fontLabel());

        int activeIndex = indexOfCurrent();
        int slot = Math.max(1, getWidth() / TRACK.length);
        int dotY = 16;
        int dotSize = 12;

        for (int i = 0; i < TRACK.length; i++) {
            IncidentPhase phase = TRACK[i];
            int centerX = slot * i + slot / 2;

            boolean done = activeIndex >= 0 && i < activeIndex;
            boolean active = i == activeIndex;
            boolean gate = phase == IncidentPhase.AWAITING_APPROVAL;

            Color color;
            if (active) {
                color = gate ? UiTheme.warning() : UiTheme.accent();
            } else if (done) {
                color = UiTheme.success();
            } else {
                color = UiTheme.muted();
            }

            // linha ligando ao proximo
            if (i < TRACK.length - 1) {
                g2.setColor(done ? UiTheme.success() : UiTheme.border());
                g2.fillRect(centerX + dotSize, dotY + dotSize / 2 - 1, slot - dotSize * 2, 2);
            }

            if (active) {
                g2.setColor(UiTheme.tint(color));
                g2.fillOval(centerX - dotSize, dotY - dotSize / 2 - 2, dotSize * 2 + 4, dotSize * 2 + 4);
            }
            g2.setColor(color);
            g2.fillOval(centerX - dotSize / 2, dotY, dotSize, dotSize);

            String label = phase.label();
            int textWidth = g2.getFontMetrics().stringWidth(label);
            g2.setColor(active ? color : UiTheme.muted());
            g2.drawString(label, centerX - textWidth / 2, dotY + dotSize + 18);
        }
        g2.dispose();
    }
}
