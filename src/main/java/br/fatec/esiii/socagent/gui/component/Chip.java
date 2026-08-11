package br.fatec.esiii.socagent.gui.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JLabel;

import br.fatec.esiii.socagent.gui.theme.UiTheme;

/**
 * Etiqueta arredondada usada para severidade, fase e tipo de evento.
 *
 * <p>A cor sempre acompanha o texto, nunca o substitui: a informacao continua
 * legivel para quem nao distingue as cores.
 */
public class Chip extends JLabel {

    private Color chipColor;

    public Chip(String text, Color color) {
        super(text);
        this.chipColor = color;
        setFont(UiTheme.fontLabel());
        setForeground(color);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 9, 3, 9));
        setOpaque(false);
    }

    public void update(String text, Color color) {
        setText(text);
        this.chipColor = color;
        setForeground(color);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(UiTheme.tint(chipColor));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.setColor(new Color(chipColor.getRed(), chipColor.getGreen(), chipColor.getBlue(), 90));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
